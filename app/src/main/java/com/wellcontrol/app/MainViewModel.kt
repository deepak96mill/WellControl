package com.wellcontrol.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val demo = DemoController()

    var devices by mutableStateOf(store.loadDevices())
        private set
    var settings by mutableStateOf(store.loadSettings())
        private set
    var statuses by mutableStateOf<Map<Int, DeviceStatus>>(emptyMap())
        private set

    /** null = checking, true = home box answered, false = not reachable */
    var connected by mutableStateOf<Boolean?>(null)
        private set

    /** Commands that were sent and are waiting for the motor sensor. Key = device id. */
    var pending by mutableStateOf<Map<Int, Pending>>(emptyMap())
        private set

    /** Result of the last command per device, kept for a few seconds. */
    var outcomes by mutableStateOf<Map<Int, Outcome>>(emptyMap())
        private set

    /** Pop-up shown at the bottom of the screen. The UI clears it after a moment. */
    var toast by mutableStateOf<Toast?>(null)

    /** Set false while the app is in the background to save battery. */
    var polling = true

    private fun controller(): Controller =
        if (settings.demoMode) demo else HomeBoxController(getApplication<Application>(), settings.host)

    init {
        viewModelScope.launch {
            while (isActive) {
                if (polling) refresh()
                delay(2000)
            }
        }
    }

    suspend fun refresh() {
        try {
            statuses = controller().status(devices)
            connected = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            connected = false
            statuses = devices.associate { it.id to DeviceStatus(online = false, running = false) }
        }
    }

    /**
     * Turn a device ON or OFF and wait until the motor sensor confirms it.
     * The card shows each step: sending, waiting for the sensor, then confirmed (or not).
     */
    fun setRunning(device: Device, on: Boolean) {
        if (pending.containsKey(device.id)) return
        viewModelScope.launch {
            pending = pending + (device.id to Pending(on, Stage.SENDING))
            outcomes = outcomes - device.id

            var confirmed = false
            var sendFailed = false
            try {
                if (controller().command(device, on)) {
                    pending = pending + (device.id to Pending(on, Stage.WAITING))
                    // Give the radio, the relay and the starter up to ~8 seconds.
                    for (i in 0 until 8) {
                        refresh()
                        val s = statuses[device.id]
                        if (s != null && s.online && s.running == on) {
                            confirmed = true
                            break
                        }
                        delay(1000)
                    }
                } else {
                    sendFailed = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendFailed = true
            }
            if (!confirmed && connected == false) sendFailed = true

            pending = pending - device.id
            val outcome = Outcome(on, confirmed)
            outcomes = outcomes + (device.id to outcome)

            toast = when {
                confirmed && on -> Toast("${device.name} is ON", "Confirmed by the motor sensor", ToastKind.ON)
                confirmed -> Toast("${device.name} is OFF", "Motor has stopped", ToastKind.OFF)
                sendFailed -> Toast("Couldn't reach the home box", "Is your phone on the home box Wi-Fi?", ToastKind.ERROR)
                else -> Toast(
                    "${device.name} didn't respond",
                    "Command sent, but the motor didn't ${if (on) "start" else "stop"}. Check the starter.",
                    ToastKind.ERROR,
                )
            }

            launch {
                delay(4000)
                if (outcomes[device.id] == outcome) outcomes = outcomes - device.id
            }
        }
    }

    fun stopAll() {
        viewModelScope.launch {
            pending = devices.associate { it.id to Pending(false, Stage.SENDING) }
            var failed = 0
            for (d in devices) {
                try {
                    if (!controller().command(d, false)) failed++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed++
                }
            }
            refresh()
            pending = emptyMap()
            toast = if (failed == 0) {
                Toast("All motors stopped", "STOP sent to every device", ToastKind.OFF)
            } else {
                Toast("STOP failed for $failed device(s)", "Check the home box connection", ToastKind.ERROR)
            }
        }
    }

    fun setDemo(on: Boolean) {
        settings = settings.copy(demoMode = on)
        store.saveSettings(settings)
        connected = null
        viewModelScope.launch { refresh() }
    }

    /** Home screen layout: tiles or list. */
    fun setListView(on: Boolean) {
        settings = settings.copy(listView = on)
        store.saveSettings(settings)
    }

    fun setHost(host: String) {
        if (host.isBlank()) return
        settings = settings.copy(host = host)
        store.saveSettings(settings)
        connected = null
        toast = Toast("Home box address saved", host, ToastKind.INFO)
        viewModelScope.launch { refresh() }
    }

    fun newDevice(): Device {
        val nextId = (devices.maxOfOrNull { it.id } ?: 0) + 1
        val nextBox = (devices.maxOfOrNull { it.boxId } ?: 0) + 1
        return Device(nextId, "New device", DeviceKind.OTHER, DeviceType.DIRECT, nextBox, 1, 0)
    }

    fun saveDevice(d: Device) {
        devices = if (devices.any { it.id == d.id }) {
            devices.map { if (it.id == d.id) d else it }
        } else {
            devices + d
        }
        store.saveDevices(devices)
        viewModelScope.launch { refresh() }
    }

    fun deleteDevice(id: Int) {
        devices = devices.filterNot { it.id == id }
        store.saveDevices(devices)
    }

    fun syncDevices() {
        viewModelScope.launch {
            toast = try {
                if (controller().pushDevices(devices)) {
                    Toast("Setup sent to home box", "Device list updated", ToastKind.INFO)
                } else {
                    Toast("Home box rejected the setup", "Try again", ToastKind.ERROR)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast("Couldn't reach the home box", "Is your phone on the home box Wi-Fi?", ToastKind.ERROR)
            }
        }
    }
}
