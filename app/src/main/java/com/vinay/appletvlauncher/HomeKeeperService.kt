package com.vinay.tvlauncher

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class HomeKeeperService : AccessibilityService() {

    private var lastOpen = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var currentPkg = ""
    private var homeCache: Set<String> = emptySet()
    private var homeCacheAt = 0L
    private var receiverOn = false

    // Stock Google TV launcher packages
    private val stockLaunchers = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.google.android.leanbacklauncher"
    )

    // Ye packages recent list me nahi judte
    private val ignoredForRecents = setOf(
        "android",
        "com.android.systemui"
    )

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            val a = i?.action
            if (a == Intent.ACTION_SCREEN_ON || a == Intent.ACTION_USER_PRESENT) {
                afterScreenOn()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!receiverOn) {
            val f = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            receiverOn = runCatching { registerReceiver(screenReceiver, f) }.isSuccess
        }
        // TV chalu hote hi helper judta hai, usi pal apna launcher kholo
        openLauncher(true)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (receiverOn) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverOn = false
        }
        super.onDestroy()
    }

    // TV ka asli Home app (Google TV launcher) kaun sa hai, apne aap pata karo
    private fun homePackages(): Set<String> {
        val now = SystemClock.elapsedRealtime()
        if (homeCache.isNotEmpty() && now - homeCacheAt < 60_000) return homeCache
        val found = runCatching {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(i, 0)
                .map { it.activityInfo.packageName }
                .filter { it != packageName }
                .toSet()
        }.getOrDefault(emptySet())
        homeCache = found + stockLaunchers
        homeCacheAt = now
        return homeCache
    }

    // TV standby se jaagne par: agar Google ka Home khula hai to apna launcher kholo
    private fun afterScreenOn() {
        handler.removeCallbacksAndMessages(null)
        currentPkg = ""
        val delays = longArrayOf(1500, 4000, 8000)
        for (d in delays) {
            handler.postDelayed({
                if (currentPkg.isEmpty() || currentPkg in homePackages()) {
                    openLauncher(true)
                }
            }, d)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode

        if (code == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP) openLauncher()
            return true
        }

        // Remap chun'ne ke waqt button app tak jaane do
        if (RemapState.capturing) return false

        val action = RemapStore.get(this, code)
        if (action == null) {
            // Asli App Switch button ho to Recent Apps kholo
            if (code == KeyEvent.KEYCODE_APP_SWITCH) {
                if (event.action == KeyEvent.ACTION_UP) openRecents()
                return true
            }
            return false
        }
        if (event.action == KeyEvent.ACTION_UP) runAction(action)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg !in ignoredForRecents
        ) {
            currentPkg = pkg
        }

        if (pkg != packageName && (pkg in stockLaunchers || pkg in homePackages())) {
            openLauncher()
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg != packageName &&
            pkg !in ignoredForRecents
        ) {
            RecentStore.push(this, pkg)
        }
    }

    override fun onInterrupt() {}

    private fun runAction(action: String) {
        try {
            when {
                action == "home" -> openLauncher()
                action == "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                action == "recents" -> openRecents()
                action == "playpause" -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                action == "mute" -> {
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_TOGGLE_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                action == "settings" -> startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                action == "nothing" -> {}
                action.startsWith("app:") -> {
                    val pkg = action.removePrefix("app:")
                    val i = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                        ?: packageManager.getLaunchIntentForPackage(pkg)
                    if (i != null) startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } catch (e: Exception) {
            // koi galti ho to chup-chaap ignore
        }
    }

    private fun mediaKey(code: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun openLauncher(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastOpen < 800) return
        lastOpen = now
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
    }

    private fun openRecents() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastOpen < 800) return
        lastOpen = now
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra("recents", true)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
            )
        }
    }
}
