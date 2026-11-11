package com.wellcontrol.app

import org.json.JSONObject

/** What the device is (only changes the icon and label). */
enum class DeviceKind(val label: String) {
    WELL("Well motor"),
    PUMP("Water motor"),
    COMPRESSOR("Compressor"),
    OTHER("Other");

    companion object {
        fun from(name: String?): DeviceKind = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** How the box switches the device. */
enum class DeviceType(val label: String, val help: String) {
    STARTER("Starter (ON + OFF buttons)", "Uses 2 relays that press the starter's buttons"),
    DIRECT("Direct (through a contactor)", "Uses 1 relay that holds a contactor ON");

    companion object {
        fun from(name: String?): DeviceType = entries.firstOrNull { it.name == name } ?: STARTER
    }
}

/** One motor / machine that the app controls. */
data class Device(
    val id: Int,
    val name: String,
    val kind: DeviceKind,
    val type: DeviceType,
    /** Which box it is connected to (1 = Well box 1, etc.). */
    val boxId: Int,
    /** Which relay on that box (1, 2, 3...). For STARTER devices this is the first of two relays. */
    val channel: Int,
    /** Turn off automatically after this many minutes. 0 = never. */
    val autoOffMin: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("kind", kind.name)
        .put("type", type.name)
        .put("box", boxId)
        .put("channel", channel)
        .put("autoOffMin", autoOffMin)

    companion object {
        fun fromJson(o: JSONObject) = Device(
            id = o.getInt("id"),
            name = o.optString("name", "Device"),
            kind = DeviceKind.from(o.optString("kind")),
            type = DeviceType.from(o.optString("type")),
            boxId = o.optInt("box", 1),
            channel = o.optInt("channel", 1),
            autoOffMin = o.optInt("autoOffMin", 0),
        )
    }
}

/** Live status reported by the home box (or the demo simulator). */
data class DeviceStatus(
    val online: Boolean,
    val running: Boolean,
    val amps: Float = 0f,
    val runSecs: Int = 0,
    /** Empty = no problem. Otherwise e.g. "Dry run - motor stopped". */
    val fault: String = "",
)

/** A command that has been sent and is waiting for the motor sensor to confirm. */
enum class Stage { SENDING, WAITING }

data class Pending(val target: Boolean, val stage: Stage)

/** Result of the last command, shown on the card for a few seconds. */
data class Outcome(val target: Boolean, val ok: Boolean)

enum class ToastKind { ON, OFF, ERROR, INFO }

/** Small pop-up message at the bottom of the screen. */
data class Toast(val title: String, val sub: String, val kind: ToastKind)

data class Settings(
    val demoMode: Boolean = true,
    val host: String = "192.168.4.1",
    /** Home screen layout: false = tiles (2 per row), true = list (1 per row). */
    val listView: Boolean = false,
)

fun defaultDevices(): List<Device> = listOf(
    Device(1, "Well 1", DeviceKind.WELL, DeviceType.STARTER, boxId = 1, channel = 1, autoOffMin = 45),
    Device(2, "Well 2", DeviceKind.WELL, DeviceType.STARTER, boxId = 2, channel = 1, autoOffMin = 45),
    Device(3, "Water motor", DeviceKind.PUMP, DeviceType.DIRECT, boxId = 3, channel = 1, autoOffMin = 30),
    Device(4, "Compressor", DeviceKind.COMPRESSOR, DeviceType.DIRECT, boxId = 3, channel = 2, autoOffMin = 0),
)

/** "45 s", "12 min", "1 h 5 min" */
fun formatDuration(totalSecs: Int): String {
    val s = totalSecs.coerceAtLeast(0)
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min"
        else -> "${s / 3600} h ${(s % 3600) / 60} min"
    }
}
