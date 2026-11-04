package com.vinay.appletvlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import java.util.Calendar

class AnalogClockView(context: Context) : View(context) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - 6f

        // Glass gola
        p.style = Paint.Style.FILL
        p.color = Color.argb(45, 255, 255, 255)
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = Color.argb(150, 255, 255, 255)
        c.drawCircle(cx, cy, r, p)

        // 12 nishan
        p.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val big = i % 3 == 0
            p.strokeWidth = if (big) 6f else 3f
            p.color = if (big) Color.WHITE else Color.argb(170, 255, 255, 255)
            val r1 = r - (if (big) 22f else 14f)
            val r2 = r - 6f
            c.drawLine(
                cx + (Math.cos(a) * r1).toFloat(), cy + (Math.sin(a) * r1).toFloat(),
                cx + (Math.cos(a) * r2).toFloat(), cy + (Math.sin(a) * r2).toFloat(), p
            )
        }

        val now = Calendar.getInstance()
        val s = now.get(Calendar.SECOND) + now.get(Calendar.MILLISECOND) / 1000f
        val m = now.get(Calendar.MINUTE) + s / 60f
        val h = (now.get(Calendar.HOUR) % 12) + m / 60f

        hand(c, cx, cy, h / 12f * 360f, r * 0.50f, 9f, Color.WHITE)
        hand(c, cx, cy, m / 60f * 360f, r * 0.75f, 6f, Color.WHITE)
        hand(c, cx, cy, s / 60f * 360f, r * 0.82f, 3f, Color.rgb(255, 149, 0))

        p.style = Paint.Style.FILL
        p.color = Color.rgb(255, 149, 0)
        c.drawCircle(cx, cy, 8f, p)

        postInvalidateDelayed(1000)
    }

    private fun hand(c: Canvas, cx: Float, cy: Float, deg: Float, len: Float, w: Float, color: Int) {
        val a = Math.toRadians(deg - 90.0)
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        p.color = color
        c.drawLine(cx, cy, cx + (Math.cos(a) * len).toFloat(), cy + (Math.sin(a) * len).toFloat(), p)
    }
}
