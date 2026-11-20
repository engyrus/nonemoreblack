package com.engyrus.nonemoreblack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Allows tvQuickActions (or ADB) to toggle the blackout overlay via broadcast
 * (`com.engyrus.nonemoreblack.TOGGLE`) without touching the Activity stack at all.
 */
class ToggleOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
