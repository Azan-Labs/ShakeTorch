package com.innolytix.shaketorch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Restarts the shake service automatically after the phone powers on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return
        try {
            context.startForegroundService(Intent(context, ShakeService::class.java))
        } catch (e: Exception) {
            Log.e("ShakeTorch", "Could not start service on boot", e)
        }
    }
}
