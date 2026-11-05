package com.innolytix.shaketorch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Shake Torch"
            textSize = 26f
        })
        root.addView(TextView(this).apply {
            text = "Phone ko jaldi se 2 baar shake karein (Motorola ki tarah) – flashlight ON/OFF ho jayegi. " +
                "Screen band ho ya phone restart ho, yeh khud chalti rahegi."
            setPadding(0, pad / 2, 0, pad)
        })

        root.addView(Switch(this).apply {
            text = "Shake se flashlight (hamesha ON)"
            textSize = 18f
            isChecked = Prefs.isEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Prefs.setEnabled(this@MainActivity, on)
                if (on) startShakeService() else stopService(Intent(this@MainActivity, ShakeService::class.java))
                refresh()
            }
        })

        val sensLabel = TextView(this).apply { setPadding(0, pad, 0, 0) }
        fun setLabel(v: Int) { sensLabel.text = "Sensitivity: $v  (1 = zor se shake, 10 = halka shake)" }
        setLabel(Prefs.sensitivity(this))
        root.addView(sensLabel)
        root.addView(SeekBar(this).apply {
            max = 9
            progress = Prefs.sensitivity(this@MainActivity) - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                    Prefs.setSensitivity(this@MainActivity, p + 1); setLabel(p + 1)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    if (Prefs.isEnabled(this@MainActivity)) startShakeService()
                }
            })
        })

        root.addView(Button(this).apply {
            text = "Battery optimization band karein (zaroori)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
        })
        root.addView(Button(this).apply {
            text = "App settings (Autostart / Background allow karein)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            }
        })

        status = TextView(this).apply { setPadding(0, pad, 0, 0) }
        root.addView(status)
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (Prefs.isEnabled(this)) startShakeService()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun startShakeService() {
        startForegroundService(Intent(this, ShakeService::class.java))
    }

    private fun refresh() {
        val ignoring = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        status.text = "Status: " + (if (Prefs.isEnabled(this)) "ON ✅" else "OFF ❌") +
            "\nBattery optimization: " + (if (ignoring) "band ✅" else "ON hai ⚠️ (band karein)")
    }
}
