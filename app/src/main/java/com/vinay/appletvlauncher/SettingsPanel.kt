package com.vinay.appletvlauncher

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsPanel(
    private val act: Activity,
    private val prefs: Prefs,
    private val accent: Int,
    private val onClose: () -> Unit
) {
    var first: View? = null

    private fun dp(v: Int) = (v * act.resources.displayMetrics.density).toInt()

    private fun rowBg(focused: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(Color.argb(if (focused) 70 else 28, 255, 255, 255))
        if (focused) setStroke(dp(3), accent) else setStroke(dp(1), Color.argb(60, 255, 255, 255))
    }

    private fun rowParams() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }

    fun build(): View {
        val sv = ScrollView(act).apply { isVerticalScrollBarEnabled = false }
        val list = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(28), dp(30), dp(40))
        }
        sv.addView(list)

        list.addView(TextView(act).apply {
            text = "Settings"
            setTextColor(Color.WHITE)
            textSize = 32f
        })
        list.addView(TextView(act).apply {
            text = "OK ya left/right dabao: badlo   |   Back: band karke lagao"
            setTextColor(Color.argb(180, 255, 255, 255))
            textSize = 14f
            setPadding(0, dp(4), 0, dp(8))
        })

        list.addView(section("Theme aur Rang"))
        list.addView(option("Theme", "theme", 0, listOf("Apple Dark", "Apple Light", "Glass", "Sunset")), rowParams())
        list.addView(option("Accent color", "accent", 0, listOf("Neela", "Hara", "Laal", "Sunehri", "Jamuni", "Safed")), rowParams())

        list.addView(section("Card"))
        list.addView(option("Card ka gol kona", "round", 1, listOf("Kam", "Medium", "Zyada")), rowParams())
        list.addView(option("Ek line me kitne card", "perrow", 1, listOf("4", "5", "6", "7", "8")), rowParams())
        list.addView(option("Card ke beech ka gap", "cardgap", 1, listOf("Kam", "Medium", "Zyada")), rowParams())
        list.addView(option("Side ka gap", "sidegap", 1, listOf("Kam", "Medium", "Zyada")), rowParams())
        list.addView(option("Upar-niche ka gap", "vgap", 1, listOf("Kam", "Medium", "Zyada")), rowParams())

        list.addView(section("Clock aur Greeting"))
        list.addView(option("Clock", "clock", 0, listOf("Dono", "Sirf Analog", "Sirf Digital")), rowParams())
        list.addView(option("Time", "h24", 0, listOf("12 ghante", "24 ghante")), rowParams())
        list.addView(option("Greeting card", "greet", 1, listOf("Off", "On")), rowParams())
        list.addView(nameRow(), rowParams())
        list.addView(option("Date aur din", "date", 1, listOf("Off", "On")), rowParams())

        list.addView(section("Wallpaper"))
        list.addView(option("Wallpaper badalna (slideshow)", "slide", 0, listOf("Band", "30 second", "1 minute", "5 minute", "15 minute", "1 ghanta")), rowParams())
        list.addView(option("Wallpaper ki dhundhli parat", "wpdim", 2, listOf("Nahi", "Kam", "Medium", "Zyada", "Bahut zyada")), rowParams())

        list.addView(section("Wallpaper"))
        list.addView(option("Wallpaper ki dhundhli parat", "dim", 1, listOf("Bilkul nahi", "Kam", "Medium", "Zyada")), rowParams())
        list.addView(option("Slideshow (apne aap badlo)", "slide", 0, listOf("Off", "1 minute", "5 minute", "15 minute", "30 minute")), rowParams())

        list.addView(section(""))
        list.addView(action("Sab default karo") {
            prefs.clear()
            onClose()
        }, rowParams())
        list.addView(action("Save karke band karo") { onClose() }, rowParams())
        return sv
    }

    private fun section(t: String): View = TextView(act).apply {
        text = t
        setTextColor(Color.argb(200, 255, 255, 255))
        textSize = 16f
        setPadding(dp(6), dp(18), 0, dp(2))
    }

    private fun option(label: String, key: String, def: Int, opts: List<String>): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            background = rowBg(false)
            setPadding(dp(24), dp(14), dp(24), dp(14))
        }
        val labelTv = TextView(act).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 20f
        }
        val valueTv = TextView(act).apply {
            setTextColor(accent)
            textSize = 20f
            gravity = Gravity.END
        }
        row.addView(labelTv, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(valueTv, LinearLayout.LayoutParams(-2, -2))

        var cur = prefs.int(key, def).coerceIn(0, opts.size - 1)
        fun show() { valueTv.text = "<   " + opts[cur] + "   >" }
        fun step(d: Int) {
            cur = (cur + d + opts.size) % opts.size
            prefs.put(key, cur)
            show()
        }
        show()

        row.setOnClickListener { step(1) }
        row.setOnKeyListener { _, code, ev ->
            if (ev.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_DPAD_LEFT) {
                step(-1)
                true
            } else if (ev.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                step(1)
                true
            } else {
                false
            }
        }
        row.setOnFocusChangeListener { vw, has -> vw.background = rowBg(has) }
        if (first == null) first = row
        return row
    }

    private fun nameRow(): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rowBg(false)
            setPadding(dp(24), dp(6), dp(24), dp(6))
        }
        val labelTv = TextView(act).apply {
            text = "Greeting me naam"
            setTextColor(Color.WHITE)
            textSize = 20f
        }
        val edit = EditText(act).apply {
            setText(prefs.str("name", "Vinay"))
            hint = "Naam likho"
            setHintTextColor(Color.argb(120, 255, 255, 255))
            setTextColor(accent)
            textSize = 20f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            gravity = Gravity.END
            setBackgroundColor(Color.TRANSPARENT)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    prefs.putStr("name", s?.toString() ?: "")
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        edit.setOnFocusChangeListener { _, has -> row.background = rowBg(has) }
        row.addView(labelTv, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(edit, LinearLayout.LayoutParams(dp(300), -2))
        return row
    }

    private fun action(label: String, onClick: () -> Unit): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = rowBg(false)
            setPadding(dp(24), dp(14), dp(24), dp(14))
        }
        row.addView(TextView(act).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 20f
        })
        row.setOnClickListener { onClick() }
        row.setOnFocusChangeListener { vw, has -> vw.background = rowBg(has) }
        return row
    }
}
