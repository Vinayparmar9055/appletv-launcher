package com.vinay.appletvlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class HomeKeeperService : AccessibilityService() {

    private var last = 0L

    private val googleHomes = setOf(
        "com.google.android.apps.tv.launcherx",
        "com.google.android.tvlauncher",
        "com.google.android.leanbacklauncher"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        openOurHome()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg in googleHomes) openOurHome()
    }

    override fun onInterrupt() {}

    private fun openOurHome() {
        val now = SystemClock.elapsedRealtime()
        if (now - last < 1500) return
        last = now
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(i)
    }
}
