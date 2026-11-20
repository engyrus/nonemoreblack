package com.engyrus.nonemoreblack

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import kotlin.math.roundToInt

/**
 * Configures and runs the None More Black splash animation on an inflated R.layout.activity_main view.
 * Shared by both MainActivity (fallback mode) and BlackoutOverlayService (overlay mode).
 */
class BlackoutSplash(rootView: View, resources: Resources) {

    private val logoView: ImageView = rootView.findViewById(R.id.logo_view)
    private val umlautsView: ImageView = rootView.findViewById(R.id.umlauts_view)
    private val logoSubtitle: TextView = rootView.findViewById(R.id.logo_subtitle)
    private val footerUrl: TextView = rootView.findViewById(R.id.footer_url)

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
    }

    fun cancel() {
        logoView.animate().cancel()
        umlautsView.animate().cancel()
        logoSubtitle.animate().cancel()
        footerUrl.animate().cancel()
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
}
