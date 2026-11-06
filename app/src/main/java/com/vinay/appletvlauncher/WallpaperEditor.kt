package com.vinay.appletvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

internal class EditorView(ctx: Context, private val src: Bitmap) : View(ctx) {
    private var zoom = 1f
    private var rot = 0
    private var ox = 0f
    private var oy = 0f
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    private fun rw(): Int = if (rot % 2 == 0) src.width else src.height
    private fun rh(): Int = if (rot % 2 == 0) src.height else src.width

    private fun scaleFor(vw: Float, vh: Float): Float =
        maxOf(vw / rw(), vh / rh()) * zoom

    private fun clampOffsets() {
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val s = scaleFor(vw, vh)
        val mx = ((rw() * s - vw) / 2f / vw).coerceAtLeast(0f)
        val my = ((rh() * s - vh) / 2f / vh).coerceAtLeast(0f)
        ox = ox.coerceIn(-mx, mx)
        oy = oy.coerceIn(-my, my)
    }

    private fun matrixFor(vw: Float, vh: Float): Matrix {
        val m = Matrix()
        m.postTranslate(-src.width / 2f, -src.height / 2f)
        m.postRotate(rot * 90f)
        val s = scaleFor(vw, vh)
        m.postScale(s, s)
        m.postTranslate(vw / 2f + ox * vw, vh / 2f + oy * vh)
        return m
    }

    fun pan(dx: Float, dy: Float) {
        ox += dx
        oy += dy
        clampOffsets()
        invalidate()
    }

    fun zoomBy(d: Float) {
        zoom = (zoom + d).coerceIn(1f, 4f)
        clampOffsets()
        invalidate()
    }

    fun rotate(d: Int) {
        rot = ((rot + d) % 4 + 4) % 4
        ox = 0f
        oy = 0f
        clampOffsets()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(src, matrixFor(width.toFloat(), height.toFloat()), paint)
    }

    fun render(): Bitmap {
        val outW = 1920
        val outH = (1920f * height / width).toInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(src, matrixFor(outW.toFloat(), outH.toFloat()), paint)
        return out
    }
}

class WallpaperEditor(ctx: Context, src: Bitmap, private val onDone: (Bitmap?) -> Unit) {
    val root = FrameLayout(ctx)
    private val view = EditorView(ctx, src)
    private val hint = TextView(ctx)
    private var mode = 0

    init {
        root.isFocusable = true
        root.isFocusableInTouchMode = true
        root.addView(view, FrameLayout.LayoutParams(-1, -1))
        val d = ctx.resources.displayMetrics.density
        hint.setTextColor(Color.WHITE)
        hint.textSize = 18f
        hint.setBackgroundColor(Color.argb(200, 0, 0, 0))
        hint.setPadding((24 * d).toInt(), (14 * d).toInt(), (24 * d).toInt(), (14 * d).toInt())
        root.addView(hint, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        update()
    }

    private fun update() {
        hint.text = when (mode) {
            0 -> "1/4 KHISKAO: arrow se photo khiskao   |   OK: agla   |   Back: cancel"
            1 -> "2/4 ZOOM: Upar/Right = bada, Neeche/Left = chhota   |   OK: agla"
            2 -> "3/4 GHUMAO: Left/Right = 90 degree ghumao   |   OK: agla"
            else -> "4/4 SAVE: OK dabao = wallpaper save   |   koi arrow = wapas khiskao"
        }
    }

    fun handleKey(code: Int, repeat: Int): Boolean {
        val isArrow = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT ||
                code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (repeat > 0) return true
            if (mode >= 3) {
                onDone(view.render())
            } else {
                mode++
                update()
            }
            return true
        }
        if (code == KeyEvent.KEYCODE_MENU) {
            onDone(view.render())
            return true
        }
        if (!isArrow) return false

        val left = code == KeyEvent.KEYCODE_DPAD_LEFT
        val right = code == KeyEvent.KEYCODE_DPAD_RIGHT
        val up = code == KeyEvent.KEYCODE_DPAD_UP
        val step = 0.03f
        when (mode) {
            0 -> {
                val dx = if (left) -step else if (right) step else 0f
                val dy = if (up) -step else if (!left && !right) step else 0f
                view.pan(dx, dy)
            }
            1 -> view.zoomBy(if (up || right) 0.1f else -0.1f)
            2 -> if (repeat == 0) view.rotate(if (left || up) -1 else 1)
            else -> {
                mode = 0
                update()
            }
        }
        return true
    }
}
