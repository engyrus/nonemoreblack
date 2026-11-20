package com.engyrus.nonemoreblack

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * None More Black — A minimal pure-black (#000000) screen-blanking toggle for Android TV / Google TV.
 *
 * When SYSTEM_ALERT_WINDOW ("Display over other apps") is granted, delegates to BlackoutOverlayService
 * so underlying video apps stay resumed and keep playing. Otherwise prompts the user to grant the
 * Display Over Other Apps permission and opens the corresponding Settings screen.
 *
 * Author: Jerry Kindall
 * Version: 11.0
 */
class MainActivity : AppCompatActivity() {

    private var splash: BlackoutSplash? = null
    private var permissionDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        disableTransitionAnimation(isOpen = true)
        super.onCreate(savedInstanceState)

        // If overlay permission is granted, toggle the pure-black overlay and exit immediately
        // so video apps underneath stay resumed and never pause.
        if (Settings.canDrawOverlays(this)) {
            BlackoutOverlayService.toggle(this)
            finishImmediately()
            return
        }

        // Permission not granted: prompt the user to enable Display Over Other Apps.
        window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.BLACK

        showOverlayPermissionDialog()
    }

    private fun showOverlayPermissionDialog() {
        permissionDialog = AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(
                "None More Black requires the Display Over Other Apps permission. " +
                    "Click OK to go to the screen where you can grant it."
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                openOverlayPermissionSettings()
                finishImmediately()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                finishImmediately()
            }
            .setOnCancelListener {
                finishImmediately()
            }
            .show()
            .also { dialog ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.requestFocus()
            }
    }

    private fun openOverlayPermissionSettings() {
        val packageUri = Uri.parse("package:$packageName")
        val candidates = listOf(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri),
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        )
        for (intent in candidates) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try next fallback screen
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && permissionDialog == null) {
            hideSystemBars()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Settings.canDrawOverlays(this)) {
            BlackoutOverlayService.toggle(this)
        }
        finishImmediately()
    }

    override fun onStop() {
        super.onStop()
        // Safeguard: pressing Home or switching inputs cleanly destroys the task.
        if (!isFinishing) {
            finishImmediately()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (permissionDialog?.isShowing == true) {
            return super.dispatchKeyEvent(event)
        }
        when (event.keyCode) {
            // Pass volume keys through to system/CEC volume control without dismissing.
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> return super.dispatchKeyEvent(event)

            // Backup exit keys: consume DOWN and finish on UP so no stray key-up leaks to the previous app.
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    finishImmediately()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun finishImmediately() {
        permissionDialog?.dismiss()
        permissionDialog = null
        splash?.cancel()
        splash = null
        disableTransitionAnimation(isOpen = false)
        finishAndRemoveTask()
    }

    private fun disableTransitionAnimation(isOpen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val type = if (isOpen) OVERRIDE_TRANSITION_OPEN else OVERRIDE_TRANSITION_CLOSE
            overrideActivityTransition(type, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
