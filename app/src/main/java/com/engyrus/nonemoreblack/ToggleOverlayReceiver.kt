package com.engyrus.nonemoreblack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Allows tvQuickActions, Home Assistant, Tasker, or ADB to control the blackout overlay via
 * broadcasts without touching the Activity stack:
 * - `com.engyrus.nonemoreblack.TOGGLE`
 * - `com.engyrus.nonemoreblack.ON`
 * - `com.engyrus.nonemoreblack.OFF`
 */
class ToggleOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ON -> {
                if (Settings.canDrawOverlays(context)) {
                    BlackoutOverlayService.show(context)
                }
            }
            ACTION_OFF -> {
                BlackoutOverlayService.hide(context)
            }
            ACTION_TOGGLE -> {
                if (Settings.canDrawOverlays(context)) {
                    BlackoutOverlayService.toggle(context)
                } else {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(launchIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.engyrus.nonemoreblack.TOGGLE"
        const val ACTION_ON = "com.engyrus.nonemoreblack.ON"
        const val ACTION_OFF = "com.engyrus.nonemoreblack.OFF"
    }
}
