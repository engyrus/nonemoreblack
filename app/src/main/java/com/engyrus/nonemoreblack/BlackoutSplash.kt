package com.engyrus.nonemoreblack

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Configures and runs the None More Black splash animation (plus the hourly wandering umlaut
 * Easter egg) on an inflated R.layout.activity_main view.
 * Shared by both MainActivity (fallback mode) and BlackoutOverlayService (overlay mode).
 */
class BlackoutSplash(rootView: View, private val resources: Resources) {

    private val logoView: ImageView = rootView.findViewById(R.id.logo_view)
    private val umlautsView: ImageView = rootView.findViewById(R.id.umlauts_view)
    private val logoSubtitle: TextView = rootView.findViewById(R.id.logo_subtitle)
    private val footerUrl: TextView = rootView.findViewById(R.id.footer_url)
    private val wanderingUmlautsView: ImageView = rootView.findViewById(R.id.wandering_umlauts_view)

    private val handler = Handler(Looper.getMainLooper())
    private val umlautWidth: Int
    private val umlautHeight: Int

    private val hourlyUmlautRunnable = object : Runnable {
        override fun run() {
            showWanderingUmlauts()
            handler.postDelayed(this, HOURLY_INTERVAL_MS)
        }
    }

    init {
        val logoStack = rootView.findViewById<FrameLayout>(R.id.logo_stack)

        val logoHeight = (resources.displayMetrics.heightPixels * 0.10f)
            .roundToInt()
            .coerceAtLeast(1)
        val logoWidth = (logoHeight * (224f / 96f))
            .roundToInt()
            .coerceAtLeast(1)

        logoStack.layoutParams = logoStack.layoutParams.apply {
            width = logoWidth
            height = logoHeight
        }

        // Match the wandering umlauts to the exact scale of the startup logo umlauts.
        val logoScale = logoHeight / 96f
        umlautWidth = (40f * logoScale).roundToInt().coerceAtLeast(1)
        umlautHeight = (15f * logoScale).roundToInt().coerceAtLeast(1)
        wanderingUmlautsView.layoutParams = wanderingUmlautsView.layoutParams.apply {
            width = umlautWidth
            height = umlautHeight
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

        // Match footer URL font size to the auto-sized "none more black" subtitle after layout.
        logoSubtitle.post {
            footerUrl.setTextSize(TypedValue.COMPLEX_UNIT_PX, logoSubtitle.textSize)
        }
    }

    fun start() {
        // Start at ~33% brightness and hold for 1s; subtitle fades over 1s,
        // nmb + github.com/engyrus fade over 2s, and the umlauts linger in the dark for 4s.
        fadeOutAfterHold(logoView, durationMs = 2000L)
        fadeOutAfterHold(logoSubtitle, durationMs = 1000L)
        fadeOutAfterHold(footerUrl, durationMs = 2000L)
        fadeOutAfterHold(umlautsView, durationMs = 4000L)

        // Once an hour, briefly appear at a random spot on the screen for 2s, then fade out.
        handler.removeCallbacks(hourlyUmlautRunnable)
        handler.postDelayed(hourlyUmlautRunnable, HOURLY_INTERVAL_MS)
    }

    fun cancel() {
        handler.removeCallbacks(hourlyUmlautRunnable)
        logoView.animate().cancel()
        umlautsView.animate().cancel()
        logoSubtitle.animate().cancel()
        footerUrl.animate().cancel()
        wanderingUmlautsView.animate().cancel()
    }

    private fun showWanderingUmlauts() {
        val dm = resources.displayMetrics
        val marginX = (dm.widthPixels * 0.10f).roundToInt()
        val marginY = (dm.heightPixels * 0.10f).roundToInt()
        val maxX = (dm.widthPixels - marginX - umlautWidth).coerceAtLeast(marginX)
        val maxY = (dm.heightPixels - marginY - umlautHeight).coerceAtLeast(marginY)

        val x = if (maxX > marginX) Random.nextInt(marginX, maxX) else marginX
        val y = if (maxY > marginY) Random.nextInt(marginY, maxY) else marginY

        wanderingUmlautsView.animate().cancel()
        wanderingUmlautsView.translationX = x.toFloat()
        wanderingUmlautsView.translationY = y.toFloat()
        wanderingUmlautsView.alpha = 0f
        wanderingUmlautsView.visibility = View.VISIBLE

        // Fade in gently, hold for 2 seconds, then fade back into pure black.
        wanderingUmlautsView.animate()
            .alpha(0.33f)
            .setStartDelay(0L)
            .setDuration(400L)
            .withEndAction {
                wanderingUmlautsView.animate()
                    .alpha(0f)
                    .setStartDelay(2000L)
                    .setDuration(2000L)
                    .withEndAction { wanderingUmlautsView.visibility = View.INVISIBLE }
                    .start()
            }
            .start()
    }

    private fun fadeOutAfterHold(view: View, durationMs: Long) {
        view.visibility = View.VISIBLE
        view.alpha = 0.33f
        view.animate()
            .alpha(0f)
            .setStartDelay(1000L)
            .setDuration(durationMs)
            .withEndAction { view.visibility = View.INVISIBLE }
            .start()
    }

    companion object {
        private const val HOURLY_INTERVAL_MS = 60L * 60L * 1000L
    }
}
