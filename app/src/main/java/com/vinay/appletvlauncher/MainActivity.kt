package com.vinay.appletvlauncher

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    data class AppItem(val label: String, val pkg: String, val banner: Drawable?, val icon: Drawable)

    private lateinit var rootFrame: FrameLayout
    private lateinit var mainCol: LinearLayout
    private lateinit var row: LinearLayout
    private lateinit var scroll: HorizontalScrollView
    private lateinit var nameLabel: TextView
    private lateinit var greetingTv: TextView
    private lateinit var wallpaperView: ImageView
    private lateinit var prefs: Prefs
    private lateinit var wall: WallpaperUi
    private var overlay: FrameLayout? = null
    private var settingsOpen = false

    private var th: Th = THEMES[0]
    private var accent: Int = ACCENTS[0]
    private var cardR = 28
    private var tileR = 18
    private var tileW = 190
    private var tileH = 108
    private var cgap = 10

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun gi(k: String, d: Int) = prefs.int(k, d)

    private fun pick(k: String, d: Int, vararg v: Int): Int = v[gi(k, d).coerceIn(0, v.size - 1)]

    private fun loadTheme() {
        th = THEMES[gi("theme", 0).coerceIn(0, THEMES.size - 1)]
        accent = ACCENTS[gi("accent", 0).coerceIn(0, ACCENTS.size - 1)]
        cardR = pick("round", 1, 14, 28, 44)
        tileR = pick("round", 1, 8, 18, 30)
        if (::wall.isInitialized) wall.accent = accent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        wall = WallpaperUi(this, prefs, { v, f -> showOverlay(v, f) }, { hideOverlay() })
        loadTheme()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        setContentView(buildUi())
        if (savedInstanceState == null) askDefaultHome()
    }

    private fun askDefaultHome() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (!rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 88)
                    }
                    return
                }
            }
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Default home ka option nahi mila", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
        wall.restartSlideshow()
        if (overlay == null) loadApps()
    }

    override fun onPause() {
        super.onPause()
        wall.stopSlideshow()
    }

    private fun dismissOverlay() {
        if (settingsOpen) closeSettings() else hideOverlay()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (overlay != null) dismissOverlay()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (wall.editorOpen && wall.handleKey(keyCode, event?.repeatCount ?: 0)) return true
        if (keyCode == KeyEvent.KEYCODE_MENU && overlay == null) {
            showSettings()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (overlay != null) dismissOverlay()
        if (::scroll.isInitialized) {
            scroll.smoothScrollTo(0, 0)
            row.getChildAt(0)?.requestFocus()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WallpaperUi.PERM) {
            wall.onPermission(
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    private fun wa(c: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))

    private fun glass(radius: Int, alpha: Int, border: Int = 70, focus: Boolean = false): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(wa(th.tint, (alpha * th.aMul).toInt()))
            if (focus) {
                setStroke(dp(3), accent)
            } else {
                setStroke(dp(1), wa(th.bTint, (border * th.bMul).toInt()))
            }
        }

    private fun updateGreeting() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val g = when {
            h < 12 -> "Good morning"
            h < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val n = prefs.str("name", "Vinay").trim()
        greetingTv.text = if (n.isEmpty()) g else "$g, $n"
    }

    private fun buildUi(): View {
        val side = pick("sidegap", 1, 20, 40, 64)
        val vpad = pick("vgap", 1, 14, 28, 48)
        val dockGap = pick("vgap", 1, 10, 20, 36)
        val topGap = pick("cardgap", 1, 10, 20, 32)
        cgap = pick("cardgap", 1, 4, 10, 16)
        val perRow = pick("perrow", 1, 4, 5, 6, 7, 8)
        val screenDp = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
        val avail = screenDp - 2 * side - 2 * 16 - 2 * 8
        tileW = ((avail - 8) / perRow - 2 * cgap).coerceAtLeast(80)
        tileH = tileW * 108 / 190

        val root = FrameLayout(this)
        rootFrame = root
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, th.bg)

        wallpaperView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wallpaperView, FrameLayout.LayoutParams(-1, -1))
        val dimA = pick("wpdim", 2, 0, 40, 70, 110, 160)
        val dim = View(this).apply { setBackgroundColor(wa(th.dim, dimA)) }
        root.addView(dim, FrameLayout.LayoutParams(-1, -1))
        wall.attach(wallpaperView)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(side), dp(vpad), dp(side), dp(vpad))
        }
        mainCol = col
        root.addView(col, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        col.addView(top, LinearLayout.LayoutParams(-1, 0, 1f))

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            background = glass(cardR, 40)
            setPadding(dp(36), dp(24), dp(36), dp(24))
        }
        greetingTv = TextView(this).apply {
            setTextColor(th.sub)
            textSize = 22f
            visibility = if (gi("greet", 1) == 1) View.VISIBLE else View.GONE
        }
        val fmt = if (gi("h24", 0) == 1) "HH:mm" else "h:mm a"
        val time = TextClock(this).apply {
            format12Hour = fmt
            format24Hour = fmt
            setTextColor(th.text)
            textSize = 64f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            visibility = if (gi("clock", 0) == 1) View.GONE else View.VISIBLE
        }
        val date = TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"
            format24Hour = "EEEE, d MMMM"
            setTextColor(th.sub)
            textSize = 22f
            visibility = if (gi("date", 1) == 1) View.VISIBLE else View.GONE
        }
        left.addView(greetingTv)
        left.addView(time)
        left.addView(date)
        top.addView(left, LinearLayout.LayoutParams(0, -1, 1.3f).apply { rightMargin = dp(topGap) })

        val clockCard = FrameLayout(this).apply {
            background = glass(cardR, 40)
            visibility = if (gi("clock", 0) == 2) View.GONE else View.VISIBLE
        }
        val analog = AnalogClockView(this)
        if (gi("theme", 0) == 1) {
            val cm = ColorMatrix().apply { setScale(0.15f, 0.15f, 0.15f, 1f) }
            analog.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply {
                setColorFilter(ColorMatrixColorFilter(cm))
            })
        }
        clockCard.addView(analog, FrameLayout.LayoutParams(dp(200), dp(200), Gravity.CENTER))
        top.addView(clockCard, LinearLayout.LayoutParams(0, -1, 0.9f).apply { rightMargin = dp(topGap) })

        val wp = makeCard(
            android.R.drawable.ic_menu_gallery, "Wallpaper",
            "OK dabao: photo lagao / badlo", "Wallpaper"
        )
        wp.setOnClickListener { wall.openManager() }
        top.addView(wp, LinearLayout.LayoutParams(0, -1, 0.9f).apply { rightMargin = dp(topGap) })

        val st = makeCard(
            android.R.drawable.ic_menu_preferences, "Settings",
            "OK dabao (ya remote ka Menu)", "Settings"
        )
        st.setOnClickListener { showSettings() }
        top.addView(st, LinearLayout.LayoutParams(0, -1, 0.7f))

        val dockBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glass(cardR + 4, 35)
            setPadding(dp(16), dp(8), dp(16), dp(14))
            clipChildren = false
            clipToPadding = false
        }
        scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
        }
        row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            setPadding(dp(8), dp(18), dp(8), dp(18))
        }
        scroll.addView(row)
        nameLabel = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(th.text)
            textSize = 20f
        }
        dockBox.addView(scroll, LinearLayout.LayoutParams(-1, -2))
        dockBox.addView(nameLabel, LinearLayout.LayoutParams(-1, -2))
        col.addView(dockBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(dockGap) })

        return root
    }

    private fun makeCard(iconRes: Int, title: String, hint: String, focusName: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = glass(cardR, 40)
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setColorFilter(th.text)
        }
        card.addView(icon, LinearLayout.LayoutParams(dp(64), dp(64)))
        card.addView(TextView(this).apply {
            text = title
            setTextColor(th.text)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = hint
            setTextColor(th.sub)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), 0)
        })
        card.setOnFocusChangeListener { v, has ->
            val sc = if (has) 1.05f else 1f
            v.animate().scaleX(sc).scaleY(sc).setDuration(150).start()
            v.background = glass(cardR, if (has) 85 else 40, 70, has)
            if (has) nameLabel.text = focusName
        }
        return card
    }

    private fun showOverlay(content: View, focus: View?) {
        val ov = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(240, 8, 8, 28))
            isClickable = true
        }
        ov.addView(content, FrameLayout.LayoutParams(-1, -1))
        mainCol.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        rootFrame.addView(ov, FrameLayout.LayoutParams(-1, -1))
        overlay = ov
        focus?.requestFocus()
    }

    private fun hideOverlay() {
        overlay?.let { rootFrame.removeView(it) }
        overlay = null
        wall.overlayClosed()
        mainCol.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        row.getChildAt(0)?.requestFocus()
    }

    private fun showSettings() {
        val panel = SettingsPanel(this, prefs, accent) { closeSettings() }
        val holder = FrameLayout(this)
        holder.addView(panel.build(), FrameLayout.LayoutParams(dp(760), -1, Gravity.CENTER_HORIZONTAL))
        showOverlay(holder, panel.first)
        settingsOpen = true
    }

    private fun closeSettings() {
        settingsOpen = false
        overlay = null
        loadTheme()
        setContentView(buildUi())
        updateGreeting()
        loadApps()
        wall.restartSlideshow()
    }

    private fun queryApps(category: String): List<android.content.pm.ResolveInfo> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(category)
        return packageManager.queryIntentActivities(i, 0)
    }

    private fun loadApps() {
        val pm = packageManager
        val infos = queryApps(Intent.CATEGORY_LEANBACK_LAUNCHER) + queryApps(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val items = ArrayList<AppItem>()
        for (ri in infos) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName || !seen.add(pkg)) continue
            items.add(
                AppItem(
                    ri.loadLabel(pm).toString(), pkg,
                    try { ri.activityInfo.loadBanner(pm) } catch (e: Exception) { null },
                    ri.loadIcon(pm)
                )
            )
        }
        items.sortBy { it.label.lowercase() }

        row.removeAllViews()
        for (item in items) row.addView(makeTile(item))
        row.getChildAt(0)?.requestFocus()
    }

    private fun makeTile(item: AppItem): View {
        val tile = FrameLayout(this).apply {
            isFocusable = true
            isClickable = true
            background = glass(tileR, 30)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) {
                    o.setRoundRect(0, 0, v.width, v.height, dp(tileR).toFloat())
                }
            }
        }
        val img = ImageView(this)
        if (item.banner != null) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setImageDrawable(item.banner)
        } else {
            img.scaleType = ImageView.ScaleType.CENTER_INSIDE
            img.setPadding(dp(tileW / 7), dp(tileH / 7), dp(tileW / 7), dp(tileH / 7))
            img.setImageDrawable(item.icon)
        }
        tile.addView(img, FrameLayout.LayoutParams(-1, -1))

        tile.layoutParams = LinearLayout.LayoutParams(dp(tileW), dp(tileH)).apply {
            leftMargin = dp(cgap)
            rightMargin = dp(cgap)
        }

        tile.setOnFocusChangeListener { v, has ->
            val s = if (has) 1.12f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(150).start()
            v.elevation = if (has) dp(16).toFloat() else 0f
            v.background = glass(tileR, if (has) 80 else 30, 70, has)
            if (has) {
                nameLabel.text = item.label
                scroll.smoothScrollTo(v.left - (scroll.width - v.width) / 2, 0)
            }
        }
        tile.setOnClickListener {
            val pm = packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(item.pkg)
                ?: pm.getLaunchIntentForPackage(item.pkg)
            if (intent != null) startActivity(intent)
        }
        return tile
    }
}
