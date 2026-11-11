package com.wellcontrol.app

import android.content.Context
import org.json.JSONArray

/** Saves the device list and settings on the phone. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("wellcontrol", Context.MODE_PRIVATE)

    fun loadDevices(): List<Device> {
        val raw = prefs.getString("devices", null) ?: return defaultDevices()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Device.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            defaultDevices()
        }
    }

    fun saveDevices(devices: List<Device>) {
        val arr = JSONArray()
        devices.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("devices", arr.toString()).apply()
    }

    fun loadSettings(): Settings = Settings(
        demoMode = prefs.getBoolean("demoMode", true),
        host = prefs.getString("host", null) ?: "192.168.4.1",
        listView = prefs.getBoolean("listView", false),
    )

    fun saveSettings(s: Settings) {
        prefs.edit()
            .putBoolean("demoMode", s.demoMode)
            .putString("host", s.host)
            .putBoolean("listView", s.listView)
            .apply()
    }
}
