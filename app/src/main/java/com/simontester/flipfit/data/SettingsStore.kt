package com.simontester.flipfit.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var keepAwake: Boolean
        get() = prefs.getBoolean("keep_awake", true)
        set(value) = prefs.edit().putBoolean("keep_awake", value).apply()

    var haptics: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(value) = prefs.edit().putBoolean("haptics", value).apply()

    var incrementKg: Float
        get() = prefs.getFloat("increment_kg", 0.5f)
        set(value) = prefs.edit().putFloat("increment_kg", value).apply()
}
