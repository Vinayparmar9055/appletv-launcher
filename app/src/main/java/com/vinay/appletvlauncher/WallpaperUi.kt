package com.vinay.appletvlauncher

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class WallpaperUi(
    private val act: Activity,
    private val prefs: Prefs,
    private val showOverlay: (View, View?) -> Unit,
    private val hideOverlay: () -> Unit
) {
    companion object {
        const val PERM = 77
        const val MAX = 10
    }

    var accent: Int = ACCENTS[0]

    private var target: ImageView? = null
    private var editor: WallpaperEditor? = null
    private val handler = Handler(Looper.getMainLooper())
    private val wallDir = File(act.filesDir, "walls")

    val editorOpen: Boolean
        get() = editor != null

    init {
        wallDir.mkdirs()
        val old = File(act.filesDir, "wallpaper.jpg")
        if (old.exists()) old.renameTo(File(wallDir, "w0.jpg"))
    }

    private fun dp(v: Int) = (v * act.resources.displayMetrics.density).toInt()

    private fun toast(s: String) {
        Toast.makeText(act, s, Toast.LENGTH_LONG).show()
    }

    // ---------- saved wallpapers ----------

    private fun files(): List<File> =
        (wallDir.listFiles() ?: emptyArray<File>())
            .filter { it.name.endsWith(".jpg") }
            .sortedBy { it.name }

    private fun current(): File? {
        val fs = files()
        val name = prefs.str("wpcur", "")
        return fs.firstOrNull { it.name == name } ?: fs.firstOrNull()
    }

    fun attach(iv: ImageView) {
        target = iv
        applyCurrent()
    }

    fun applyCurrent() {
        val iv = target ?: return
        val f = current()
        if (f != null) {
            val b = BitmapFactory.decodeFile(f.absolutePath)
            if (b != null) {
                iv.setImageBitmap(b)
                return
            }
        }
        iv.setImageDrawable(null)
    }

    fun overlayClosed() {
        editor = null
    }

    fun handleKey(code: Int, repeat: Int): Boolean = editor?.handleKey(code, repeat) ?: false

    // ---------- slideshow ----------

    private val tick = object : Runnable {
        override fun run() {
            nextOne()
            schedule()
        }
    }

    private fun intervalMs(): Long = when (prefs.int("slide", 0)) {
        1 -> 30000L
        2 -> 60000L
        3 -> 300000L
        4 -> 900000L
        5 -> 3600000L
        else -> 0L
    }

    private fun schedule() {
        handler.removeCallbacks(tick)
        val ms = intervalMs()
        if (ms > 0L && files().size >= 2) handler.postDelayed(tick, ms)
    }

    fun restartSlideshow() {
        schedule()
    }

    fun stopSlideshow() {
        handler.removeCallbacks(tick)
    }

    private fun nextOne() {
        val fs = files()
        if (fs.size < 2) return
        val curName = current()?.name
        val i = fs.indexOfFirst { it.name == curName }
        val n = fs[(i + 1) % fs.size]
        prefs.putStr("wpcur", n.name)
        val iv = target ?: return
        val b = BitmapFactory.decodeFile(n.absolutePath) ?: return
        iv.animate().alpha(0f).setDuration(400).withEndAction {
            iv.setImageBitmap(b)
            iv.animate().alpha(1f).setDuration(400).start()
        }.start()
    }

    // ---------- tile look ----------

    private fun tileBg(focus: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(Color.argb(40, 255, 255, 255))
        if (focus) {
            setStroke(dp(3), accent)
        } else {
            setStroke(dp(1), Color.argb(80, 255, 255, 255))
        }
    }

    private fun styleTile(v: View) {
        v.isFocusable = true
        v.isClickable = true
        v.background = tileBg(false)
        v.clipToOutline = true
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(14).toFloat())
            }
        }
        v.layoutParams = LinearLayout.LayoutParams(dp(280), dp(158)).apply {
            leftMargin = dp(10)
            rightMargin = dp(10)
        }
    }

    private fun focusFx(v: View, hs: HorizontalScrollView) {
        v.setOnFocusChangeListener { x, has ->
            val s = if (has) 1.12f else 1f
            x.animate().scaleX(s).scaleY(s).setDuration(150).start()
            x.elevation = if (has) dp(16).toFloat() else 0f
            x.background = tileBg(has)
            if (has) hs.smoothScrollTo(x.left - (hs.width - x.width) / 2, 0)
        }
    }

    private fun showStrip(title: String, fill: (LinearLayout, HorizontalScrollView) -> View?) {
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        box.addView(TextView(act).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(24), 0, dp(24), dp(24))
        })
        val hs = HorizontalScrollView(act).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
        }
        val strip = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            setPadding(dp(40), dp(24), dp(40), dp(24))
        }
        val first = fill(strip, hs)
        hs.addView(strip)
        box.addView(hs, LinearLayout.LayoutParams(-1, -2))
        showOverlay(box, first)
    }

    // ---------- wallpaper manager ----------

    fun openManager() {
        val fs = files()
        showStrip("Wallpaper (max 10)   |   OK: lagao   |   OK lamba: hatao   |   Back: band") { strip, hs ->
            val add = TextView(act).apply {
                text = "+\nNayi photo"
                textSize = 26f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            styleTile(add)
            focusFx(add, hs)
            add.setOnClickListener {
                if (files().size >= MAX) {
                    toast("10 photo ho gayi. Pehle koi hatao (OK lamba dabao)")
                } else {
                    hideOverlay()
                    pickPhoto()
                }
            }
            strip.addView(add)

            for (f in fs) {
                val img = ImageView(act).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                styleTile(img)
                Thread {
                    val b = try { decodeFileSampled(f.absolutePath, 400) } catch (e: Exception) { null }
                    act.runOnUiThread { if (b != null) img.setImageBitmap(b) }
                }.start()
                focusFx(img, hs)
                img.setOnClickListener {
                    prefs.putStr("wpcur", f.name)
                    applyCurrent()
                    hideOverlay()
                }
                img.setOnLongClickListener {
                    f.delete()
                    applyCurrent()
                    schedule()
                    toast("Wallpaper hata diya")
                    hideOverlay()
                    openManager()
                    true
                }
                strip.addView(img)
            }
            add
        }
    }

    // ---------- gallery picker ----------

    private fun pickPhoto() {
        if (act.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            act.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERM)
        } else {
            showGallery()
        }
    }

    fun onPermission(ok: Boolean) {
        if (ok) showGallery() else toast("Photo dekhne ki permission nahi mili")
    }

    private fun showGallery() {
        val uris = ArrayList<Uri>()
        try {
            act.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
            )?.use { c ->
                val idx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext() && uris.size < 30) {
                    uris.add(
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(idx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        if (uris.isEmpty()) {
            toast("TV me koi photo nahi mili. Send Files to TV se photo bhejo.")
            return
        }
        showStrip("Photo chuno (OK dabao, wapas jane ke liye Back)") { strip, hs ->
            var first: View? = null
            for (u in uris) {
                val img = ImageView(act).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                styleTile(img)
                Thread {
                    val b = try { decodeSampled(u, 400) } catch (e: Exception) { null }
                    act.runOnUiThread { if (b != null) img.setImageBitmap(b) }
                }.start()
                focusFx(img, hs)
                img.setOnClickListener { startEdit(u) }
                strip.addView(img)
                if (first == null) first = img
            }
            first
        }
    }

    private fun decodeSampled(uri: Uri, targetW: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        act.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1
        while (o.outWidth / (s * 2) >= targetW) s *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = s }
        return act.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
    }

    private fun decodeFileSampled(path: String, targetW: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= targetW) s *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = s }
        return BitmapFactory.decodeFile(path, o2)
    }

    // ---------- editor ----------

    private fun startEdit(u: Uri) {
        val bmp = try { decodeSampled(u, 1600) } catch (e: Exception) { null }
        if (bmp == null) {
            toast("Photo khul nahi payi")
            return
        }
        hideOverlay()
        val ed = WallpaperEditor(act, bmp) { out ->
            if (out != null) saveNew(out)
            editor = null
            hideOverlay()
        }
        editor = ed
        showOverlay(ed.root, ed.root)
    }

    private fun saveNew(out: Bitmap) {
        try {
            val f = File(wallDir, "w" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            prefs.putStr("wpcur", f.name)
            applyCurrent()
            schedule()
            toast("Wallpaper lag gaya")
        } catch (e: Exception) {
            toast("Wallpaper save nahi hua")
        }
    }
}
