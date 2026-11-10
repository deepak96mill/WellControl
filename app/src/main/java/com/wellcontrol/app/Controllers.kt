package com.wellcontrol.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/** Something that can read status and switch devices. */
interface Controller {
    suspend fun status(devices: List<Device>): Map<Int, DeviceStatus>
    suspend fun command(device: Device, on: Boolean): Boolean
    suspend fun pushDevices(devices: List<Device>): Boolean
}

/**
 * Demo mode: pretends to be the home box so you can try the app with no hardware.
 */
class DemoController : Controller {
    private val startedAt = mutableMapOf<Int, Long>()
    private val baseAmps = mutableMapOf<Int, Float>()

    override suspend fun status(devices: List<Device>): Map<Int, DeviceStatus> {
        val now = System.currentTimeMillis()
        return devices.associate { d ->
            val start = startedAt[d.id]
            if (start != null && d.autoOffMin > 0 && now - start >= d.autoOffMin * 60_000L) {
                startedAt.remove(d.id)
            }
            val s = startedAt[d.id]
            val status = if (s == null) {
                DeviceStatus(online = true, running = false)
            } else {
                val base = baseAmps.getOrPut(d.id) {
                    when (d.kind) {
                        DeviceKind.WELL -> 6.5f
                        DeviceKind.PUMP -> 3.8f
                        DeviceKind.COMPRESSOR -> 5.0f
                        DeviceKind.OTHER -> 2.0f
                    }
                }
                DeviceStatus(
                    online = true,
                    running = true,
                    amps = base + Random.nextFloat() * 0.4f - 0.2f,
                    runSecs = ((now - s) / 1000).toInt(),
                )
            }
            d.id to status
        }
    }

    override suspend fun command(device: Device, on: Boolean): Boolean {
        delay(700) // pretend the radio message takes a moment
        if (on) {
            if (!startedAt.containsKey(device.id)) startedAt[device.id] = System.currentTimeMillis()
        } else {
            startedAt.remove(device.id)
        }
        return true
    }

    override suspend fun pushDevices(devices: List<Device>): Boolean {
        delay(500)
        return true
    }
}

/**
 * Live mode: talks to the home box over local Wi-Fi using a tiny JSON API.
 *
 *   GET  /api/status   -> {"devices":[{"id":1,"online":true,"running":true,"amps":6.4,"runSecs":120,"fault":""}]}
 *   POST /api/cmd      <- {"id":1,"action":"on","autoOffMin":45}   -> {"ok":true}
 *   POST /api/devices  <- {"devices":[ ...device list... ]}       -> {"ok":true}
 */
class HomeBoxController(private val context: Context, private val host: String) : Controller {

    private fun open(path: String): HttpURLConnection {
        val url = URL("http://$host$path")
        // The home box Wi-Fi has no internet, so Android may prefer mobile data.
        // Force the request through the Wi-Fi network.
        val cm: ConnectivityManager? = context.getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        val wifi = if (cm == null) null else cm.allNetworks.firstOrNull { n ->
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        val conn = (wifi?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
        conn.connectTimeout = 3000
        conn.readTimeout = 5000
        return conn
    }

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = open(path)
        try {
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code != 200) throw IllegalStateException("Home box replied $code")
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val conn = open(path)
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code != 200) throw IllegalStateException("Home box replied $code")
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun status(devices: List<Device>): Map<Int, DeviceStatus> {
        val json = get("/api/status")
        val arr = json.optJSONArray("devices") ?: JSONArray()
        val result = mutableMapOf<Int, DeviceStatus>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result[o.getInt("id")] = DeviceStatus(
                online = o.optBoolean("online", false),
                running = o.optBoolean("running", false),
                amps = o.optDouble("amps", 0.0).toFloat(),
                runSecs = o.optInt("runSecs", 0),
                fault = o.optString("fault", ""),
            )
        }
        // Devices the box doesn't know about are shown as offline.
        devices.forEach { d -> if (!result.containsKey(d.id)) result[d.id] = DeviceStatus(online = false, running = false) }
        return result
    }

    override suspend fun command(device: Device, on: Boolean): Boolean {
        val body = JSONObject()
            .put("id", device.id)
            .put("action", if (on) "on" else "off")
            .put("autoOffMin", device.autoOffMin)
        return post("/api/cmd", body).optBoolean("ok", false)
    }

    override suspend fun pushDevices(devices: List<Device>): Boolean {
        val arr = JSONArray()
        devices.forEach { arr.put(it.toJson()) }
        return post("/api/devices", JSONObject().put("devices", arr)).optBoolean("ok", false)
    }
}
