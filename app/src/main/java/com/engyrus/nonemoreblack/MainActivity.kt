package com.engyrus.nonemoreblack

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.TextViewCompat
import kotlin.math.roundToInt

/**
 * None More Black — A minimal pure-black (#000000) screen-blanking toggle for Android TV / Google TV.
 *
 * Author: Jerry Kindall
 * Version: 11.0
 */
class MainActivity : AppCompatActivity() {

    private lateinit var logoContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        disableTransitionAnimation(isOpen = true)
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.BLACK

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        hideSystemBars()

        logoContainer = findViewById(R.id.logo_container)
        val logoView = findViewById<ImageView>(R.id.logo_view)
        val logoSubtitle = findViewById<TextView>(R.id.logo_subtitle)

        val logoHeight = (resources.displayMetrics.heightPixels * 0.10f)
            .roundToInt()
            .coerceAtLeast(1)
        val logoWidth = (logoHeight * (224f / 96f))
            .roundToInt()
            .coerceAtLeast(1)

        logoView.layoutParams = logoView.layoutParams.apply {
            width = logoWidth
            height = logoHeight
        }

        // Size "none more black" in sans-serif to match the exact width of the nmb logo.
        logoSubtitle.layoutParams = (logoSubtitle.layoutParams as LinearLayout.LayoutParams).apply {
            width = logoWidth
            height = (logoWidth * 0.22f).roundToInt().coerceAtLeast(1)
            topMargin = (logoHeight * 0.10f).roundToInt()
        }
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            logoSubtitle,
            4,
            200,
            1,
            TypedValue.COMPLEX_UNIT_PX
        )

        // Start at ~33% brightness, hold for 1 second, then fade out over 1 second to leave pure #000000.
        logoContainer.alpha = 0.33f
        logoContainer.animate()
            .alpha(0f)
            .setStartDelay(1000L)
            .setDuration(1000L)
            .withEndAction { logoContainer.visibility = View.GONE }
            .start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Relaunching while already active (e.g., via a mapped remote button) toggles off immediately.
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
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun finishImmediately() {
        if (::logoContainer.isInitialized) {
            logoContainer.animate().cancel()
        }
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
