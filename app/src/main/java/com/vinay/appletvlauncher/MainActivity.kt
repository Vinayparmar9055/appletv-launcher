package com.vinay.appletvlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    data class AppItem(val label: String, val pkg: String, val banner: Drawable?, val icon: Drawable)

    private lateinit var row: LinearLayout
    private lateinit var scroll: HorizontalScrollView
    private lateinit var bigIcon: ImageView
    private lateinit var bigName: TextView
    private lateinit var wallpaperView: ImageView
    private val PICK = 1001
    private val wpFile by lazy { File(filesDir, "wallpaper.jpg") }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    // Home screen par back dabane se kuch nahi hoga
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (::scroll.isInitialized) {
            scroll.smoothScrollTo(0, 0)
            row.getChildAt(0)?.requestFocus()
        }
    }

    private fun glass(radius: Int, alpha: Int): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(Color.argb(alpha, 255, 255, 255))
        }

    private fun buildUi(): View {
        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0B1026.toInt(), 0xFF2B1B5A.toInt(), 0xFF0B3B5A.toInt())
        )

        wallpaperView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wallpaperView, FrameLayout.LayoutParams(-1, -1))
        val dim = View(this).apply { setBackgroundColor(Color.argb(90, 0, 0, 0)) }
        root.addView(dim, FrameLayout.LayoutParams(-1, -1))
        applyWallpaper()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(32), dp(48), dp(32))
        }
        root.addView(col, FrameLayout.LayoutParams(-1, -1))

        // Upar: time aur date
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val time = TextClock(this).apply {
            format12Hour = "h:mm a"
            format24Hour = "h:mm a"
            setTextColor(Color.WHITE)
            textSize = 34f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        val spacer = View(this)
        val date = TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"
            format24Hour = "EEEE, d MMMM"
            setTextColor(Color.argb(200, 255, 255, 255))
            textSize = 20f
        }
        top.addView(time)
        top.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(date)
        col.addView(top, LinearLayout.LayoutParams(-1, -2))

        // Beech me: bada glass card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = glass(28, 38)
            setPadding(dp(40), dp(24), dp(40), dp(24))
        }
        bigIcon = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        card.addView(bigIcon, LinearLayout.LayoutParams(dp(120), dp(120)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), 0, 0, 0)
        }
        bigName = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 40f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val hint = TextView(this).apply {
            text = "Kholne ke liye OK dabao"
            setTextColor(Color.argb(180, 255, 255, 255))
            textSize = 18f
        }
        texts.addView(bigName)
        texts.addView(hint)
        card.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(AnalogClockView(this), LinearLayout.LayoutParams(dp(170), dp(170)))
        val cardLp = LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(24); bottomMargin = dp(24)
        }
        col.addView(card, cardLp)

        // Neeche: apps ki row
        scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
        }
        row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            setPadding(dp(8), dp(24), dp(8), dp(24))
        }
        scroll.addView(row)
        col.addView(scroll, LinearLayout.LayoutParams(-1, -2))

        return root
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
        row.addView(makeWallpaperTile())
        for (item in items) row.addView(makeTile(item))
        (row.getChildAt(1) ?: row.getChildAt(0))?.requestFocus()
    }

    private fun applyWallpaper() {
        if (wpFile.exists()) {
            val bmp = BitmapFactory.decodeFile(wpFile.absolutePath)
            if (bmp != null) { wallpaperView.setImageBitmap(bmp); return }
        }
        wallpaperView.setImageDrawable(null)
    }

    private fun makeWallpaperTile(): View {
        val tile = FrameLayout(this).apply {
            isFocusable = true
            isClickable = true
            background = glass(18, 30)
        }
        val tv = TextView(this).apply {
            text = "Wallpaper\nbadlo"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 20f
        }
        tile.addView(tv, FrameLayout.LayoutParams(-1, -1))
        tile.layoutParams = LinearLayout.LayoutParams(dp(220), dp(124)).apply {
            leftMargin = dp(12); rightMargin = dp(12)
        }
        tile.setOnFocusChangeListener { v, has ->
            val sc = if (has) 1.18f else 1f
            v.animate().scaleX(sc).scaleY(sc).setDuration(150).start()
            v.background = glass(18, if (has) 80 else 30)
            if (has) {
                bigIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                bigName.text = "Wallpaper (gallery se)"
                scroll.smoothScrollTo(v.left - (scroll.width - v.width) / 2, 0)
            }
        }
        tile.setOnClickListener {
            try {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                startActivityForResult(i, PICK)
            } catch (e: Exception) {
                try {
                    startActivityForResult(Intent.createChooser(
                        Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "Photo chuno"), PICK)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Is TV me gallery/file picker nahi mila", Toast.LENGTH_LONG).show()
                }
            }
        }
        // Lamba dabane par wallpaper hata do
        tile.setOnLongClickListener {
            wpFile.delete(); applyWallpaper()
            Toast.makeText(this, "Wallpaper hata diya", Toast.LENGTH_SHORT).show()
            true
        }
        return tile
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            while (opts.outWidth / (sample * 2) >= 1920) sample *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
            if (bmp == null) {
                Toast.makeText(this, "Photo khul nahi payi", Toast.LENGTH_LONG).show()
                return
            }
            FileOutputStream(wpFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            applyWallpaper()
        } catch (e: Exception) {
            Toast.makeText(this, "Wallpaper lagane me dikkat aayi", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeTile(item: AppItem): View {
        val tile = FrameLayout(this).apply {
            isFocusable = true
            isClickable = true
            background = glass(18, 30)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) {
                    o.setRoundRect(0, 0, v.width, v.height, dp(18).toFloat())
                }
            }
        }
        val img = ImageView(this)
        if (item.banner != null) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setImageDrawable(item.banner)
        } else {
            img.scaleType = ImageView.ScaleType.CENTER_INSIDE
            img.setPadding(dp(28), dp(16), dp(28), dp(16))
            img.setImageDrawable(item.icon)
        }
        tile.addView(img, FrameLayout.LayoutParams(-1, -1))

        val lp = LinearLayout.LayoutParams(dp(220), dp(124)).apply {
            leftMargin = dp(12); rightMargin = dp(12)
        }
        tile.layoutParams = lp

        tile.setOnFocusChangeListener { v, has ->
            val s = if (has) 1.18f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(150).start()
            v.elevation = if (has) dp(16).toFloat() else 0f
            v.background = glass(18, if (has) 80 else 30)
            if (has) {
                bigIcon.setImageDrawable(item.icon)
                bigName.text = item.label
                val target = v.left - (scroll.width - v.width) / 2
                scroll.smoothScrollTo(target, 0)
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
