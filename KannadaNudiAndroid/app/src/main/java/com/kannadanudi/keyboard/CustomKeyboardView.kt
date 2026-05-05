package com.kannadanudi.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import androidx.core.content.ContextCompat

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {

    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30") // Modern Red color
        style = Paint.Style.FILL
    }

    private var micIcon: Drawable? = null
    private val scaledKeyboards = mutableSetOf<android.inputmethodservice.Keyboard>()

    init {
        micIcon = ContextCompat.getDrawable(context, R.drawable.ic_mic)
        micIcon?.setTint(Color.WHITE)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        val w = measuredWidth
        val kbd = keyboard
        
        if (kbd != null && w > 0 && !scaledKeyboards.contains(kbd)) {
            val originalWidth = kbd.minWidth
            if (originalWidth > 0 && originalWidth != w) {
                val scale = w.toFloat() / originalWidth.toFloat()
                
                var currentX = 0f
                var currentRowY = -1
                
                for (key in kbd.keys) {
                    if (key.y != currentRowY) {
                        currentRowY = key.y
                        currentX = 0f
                    }
                    val scaledWidth = key.width * scale
                    val gapScaled = key.gap * scale
                    
                    // Add gap to currentX before placing key
                    currentX += gapScaled
                    
                    key.x = currentX.toInt()
                    key.width = Math.round(scaledWidth)
                    
                    currentX += scaledWidth
                }
            }
            scaledKeyboards.add(kbd)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val currentKeyboard = keyboard ?: return
        for (key in currentKeyboard.keys) {
            if (key.codes.isNotEmpty() && key.codes[0] == -102) {
                // Determine the rectangle for the key
                val rect = RectF(
                    key.x.toFloat(),
                    key.y.toFloat(),
                    (key.x + key.width).toFloat(),
                    (key.y + key.height).toFloat()
                )
                
                // Draw the red background with rounded corners (8dp)
                val cornerRadius = 8f * resources.displayMetrics.density
                
                // To avoid drawing over the gap, we should apply a small inset similar to the standard key background.
                // Standard Android KeyboardView has some internal padding. Let's inset by 2dp to look good.
                val inset = 2f * resources.displayMetrics.density
                rect.inset(inset, inset)
                
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintBackground)

                // Draw the mic icon
                micIcon?.let { icon ->
                    val iconSize = (24 * resources.displayMetrics.density).toInt()
                    val left = (rect.centerX() - iconSize / 2).toInt()
                    val top = (rect.centerY() - iconSize / 2).toInt()
                    icon.setBounds(left, top, left + iconSize, top + iconSize)
                    icon.draw(canvas)
                }
            }
        }
    }
}
