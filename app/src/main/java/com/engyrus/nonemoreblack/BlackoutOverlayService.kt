package com.engyrus.nonemoreblack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

/**
 * Displays a 100% opaque pure-black (#000000) TYPE_APPLICATION_OVERLAY window over the active app
 * so video players (YouTube, Plex, etc.) remain resumed and keep playing uninterrupted.
 *
 * Runs as a foreground service (specialUse) so broadcast receivers (tvQuickActions, Home Assistant,
 * ADB) can toggle it cleanly from the background on Android 12–14+.
 */
class BlackoutOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayRoot: OverlayRootLayout? = null
    private var splash: BlackoutSplash? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must enter foreground immediately when started via startForegroundService().
        enterForeground()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_SHOW -> {
                if (overlayRoot == null) {
                    showOverlay()
                }
            }
            ACTION_HIDE -> {
                removeOverlay()
                stopSelf()
            }
            ACTION_TOGGLE, null -> {
                if (overlayRoot != null) {
                    removeOverlay()
                    stopSelf()
                } else {
                    showOverlay()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun enterForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val themedContext = ContextThemeWrapper(this, R.style.Theme_NoneMoreBlack)
        val root = OverlayRootLayout(themedContext) {
            removeOverlay()
            stopSelf()
        }

        LayoutInflater.from(themedContext).inflate(R.layout.activity_main, root, true)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.OPAQUE
        ).apply {
            colorMode = ActivityInfo.COLOR_MODE_DEFAULT
            windowAnimations = 0
        }

        val blackoutSplash = BlackoutSplash(root, resources)
        wm.addView(root, params)
        root.requestFocus()
        blackoutSplash.start()

        overlayRoot = root
        splash = blackoutSplash
        isShowing = true
    }

    private fun removeOverlay() {
        splash?.cancel()
        splash = null
        overlayRoot?.let { view ->
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayRoot = null
        isShowing = false
    }

    private class OverlayRootLayout(
        context: Context,
        private val onDismiss: () -> Unit
    ) : FrameLayout(context) {

        init {
            isFocusable = true
            isFocusableInTouchMode = true
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            when (event.keyCode) {
                // Pass volume & media keys through to system/CEC without dismissing.
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_STOP -> return super.dispatchKeyEvent(event)

                // Backup exit keys: consume DOWN and dismiss on UP so no stray key-up leaks underneath.
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        onDismiss()
                    }
                    return true
                }
            }
            return super.dispatchKeyEvent(event)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.engyrus.nonemoreblack.ACTION_TOGGLE"
        const val ACTION_SHOW = "com.engyrus.nonemoreblack.ACTION_SHOW"
        const val ACTION_HIDE = "com.engyrus.nonemoreblack.ACTION_HIDE"

        private const val CHANNEL_ID = "blackout_overlay"
        private const val NOTIFICATION_ID = 11

        @Volatile
        var isShowing: Boolean = false
            private set

        fun toggle(context: Context) {
            val intent = Intent(context, BlackoutOverlayService::class.java).apply {
                action = ACTION_TOGGLE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun show(context: Context) {
            if (isShowing) return
            val intent = Intent(context, BlackoutOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun hide(context: Context) {
            if (!isShowing) return
            val intent = Intent(context, BlackoutOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
