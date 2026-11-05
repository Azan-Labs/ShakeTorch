package com.innolytix.shaketorch

import android.content.Context

/** Settings stored in device-protected storage so they can be read right after boot, even before unlock. */
object Prefs {
    private fun sp(c: Context) = c.createDeviceProtectedStorageContext()
        .getSharedPreferences("shake_torch", Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    /** 1 (hard shake) .. 10 (light shake) */
    fun sensitivity(c: Context) = sp(c).getInt("sensitivity", 5)
    fun setSensitivity(c: Context, v: Int) = sp(c).edit().putInt("sensitivity", v).apply()

    /** Shake strength (in g) needed to count as one shake. */
    fun threshold(c: Context) = 4.0f - sensitivity(c) * 0.25f
}
