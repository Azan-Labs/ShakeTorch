package com.innolytix.shaketorch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlin.math.sqrt

/**
 * Always-on foreground service: listens to the accelerometer and toggles the
 * flashlight when the phone is shaken twice quickly (like Motorola "chop chop").
 */
class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var torchOn = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var threshold = 2.75f

    private var firstShakeAt = 0L
    private var lastShakeAt = 0L
    private var shakeCount = 0
    private var lastToggleAt = 0L

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) torchOn = enabled
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        sensorManager = getSystemService(SensorManager::class.java)
        cameraManager = getSystemService(CameraManager::class.java)
        cameraId = findFlashCamera()
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        registerShakeSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.isEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        threshold = Prefs.threshold(this)
        return START_STICKY // Android restarts the service if it gets killed
    }

    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        if (::cameraManager.isInitialized) cameraManager.unregisterTorchCallback(torchCallback)
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Shake Torch", NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_torch)
            .setContentTitle("Shake Torch active")
            .setContentText("Phone ko 2 baar shake karein: flashlight ON/OFF")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun registerShakeSensor() {
        // A "wake-up" accelerometer keeps working when the screen is off without a wake lock.
        val wakeUp = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
        val sensor = wakeUp ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            Log.e(TAG, "No accelerometer on this device")
            stopSelf()
            return
        }
        if (wakeUp == null) {
            // Fallback: keep CPU awake so shake works with the screen off.
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShakeTorch::sensor")
                .apply { setReferenceCounted(false); acquire() }
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(e: SensorEvent) {
        val x = e.values[0] / SensorManager.GRAVITY_EARTH
        val y = e.values[1] / SensorManager.GRAVITY_EARTH
        val z = e.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(x * x + y * y + z * z)
        if (gForce < threshold) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastToggleAt < 1200) return     // cooldown after a toggle
        if (now - lastShakeAt < 150) return       // same shake, ignore
        if (now - firstShakeAt > 1000) {          // start a new gesture window
            firstShakeAt = now
            shakeCount = 0
        }
        shakeCount++
        lastShakeAt = now
        if (shakeCount >= 2) {
            shakeCount = 0
            lastToggleAt = now
            toggleTorch()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun toggleTorch() {
        val id = cameraId ?: findFlashCamera().also { cameraId = it } ?: return
        try {
            cameraManager.setTorchMode(id, !torchOn)
            torchOn = !torchOn
            vibrate()
        } catch (ex: Exception) {
            Log.e(TAG, "Torch toggle failed (camera may be in use)", ex)
        }
    }

    private fun findFlashCamera(): String? = try {
        val ids = cameraManager.cameraIdList
        ids.firstOrNull {
            val c = cameraManager.getCameraCharacteristics(it)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: ids.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (ex: Exception) {
        Log.e(TAG, "Camera lookup failed", ex); null
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val v = getSystemService(Vibrator::class.java) ?: return
        v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        private const val TAG = "ShakeTorch"
        private const val CHANNEL = "shake_torch"
        private const val NOTIF_ID = 1
    }
}
