package com.vinay.appletvlauncher

import android.content.Context
import android.graphics.Color

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("apple_home", Context.MODE_PRIVATE)

    fun int(k: String, d: Int): Int = sp.getInt(k, d)
    fun put(k: String, v: Int) { sp.edit().putInt(k, v).apply() }
    fun str(k: String, d: String): String = sp.getString(k, d) ?: d
    fun putStr(k: String, v: String) { sp.edit().putString(k, v).apply() }
    fun clear() { sp.edit().clear().apply() }
}

class Th(
    val bg: IntArray,
    val tint: Int,
    val aMul: Float,
    val bTint: Int,
    val bMul: Float,
    val text: Int,
    val sub: Int,
    val dim: Int
)

// 0 Apple Dark, 1 Apple Light, 2 Glass, 3 Sunset
val THEMES = listOf(
    Th(
        intArrayOf(0xFF0B1026.toInt(), 0xFF2B1B5A.toInt(), 0xFF0B3B5A.toInt()),
        Color.WHITE, 1f, Color.WHITE, 1f,
        Color.WHITE, Color.argb(220, 255, 255, 255), Color.argb(70, 0, 0, 0)
    ),
    Th(
        intArrayOf(0xFFDCE6F7.toInt(), 0xFFF2F5FB.toInt(), 0xFFD3E3F8.toInt()),
        Color.WHITE, 3.2f, Color.BLACK, 0.5f,
        0xFF14213D.toInt(), Color.argb(210, 20, 33, 61), Color.argb(110, 255, 255, 255)
    ),
    Th(
        intArrayOf(0xFF10243E.toInt(), 0xFF3A2A6B.toInt(), 0xFF0E5A6B.toInt()),
        Color.WHITE, 1.7f, Color.WHITE, 2.2f,
        Color.WHITE, Color.argb(230, 255, 255, 255), Color.argb(40, 0, 0, 0)
    ),
    Th(
        intArrayOf(0xFF2B0F3A.toInt(), 0xFFB83B5E.toInt(), 0xFFF08A5D.toInt()),
        Color.WHITE, 1f, Color.WHITE, 1f,
        Color.WHITE, Color.argb(230, 255, 255, 255), Color.argb(60, 0, 0, 0)
    )
)

// 0 Neela, 1 Hara, 2 Laal, 3 Sunehri, 4 Jamuni, 5 Safed
val ACCENTS = intArrayOf(
    0xFF0A84FF.toInt(),
    0xFF30D158.toInt(),
    0xFFFF453A.toInt(),
    0xFFFFD60A.toInt(),
    0xFFBF5AF2.toInt(),
    0xFFFFFFFF.toInt()
)
