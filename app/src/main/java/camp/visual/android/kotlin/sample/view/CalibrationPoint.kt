package camp.visual.android.kotlin.sample.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class CalibrationPoint(context: Context?) : View(context) {
    private var paint: Paint? = null
    private var toDraw = true

    private val defaultColor = Color.rgb(0x00, 0xA7, 0x26)

    private var oval: RectF? = null

    private var animationPower = 0.0f
    private var centerX = 0.0f
    private var centerY = 0.0f

    companion object {
        private const val default_radius = 30.0f
    }

    init {
        paint = Paint().also { p ->
            p.isAntiAlias = true
            p.color = defaultColor
        }
        oval = RectF()
    }

    fun setPower(power: Float) {
        animationPower = power
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint?.also { p ->
            oval?.also { o ->
                if (toDraw) {
                    val ovalLong = default_radius
                    val ovalShort = default_radius

                    o.left = centerX - ovalLong / 2
                    o.top = centerY - ovalShort / 2
                    o.right = centerX + ovalLong / 2
                    o.bottom = centerY + ovalShort / 2


                    val red = (0x00 - (animationPower * 0x88)).toInt()
                    val green = (0x88 + (animationPower * 0x77)).toInt()

                    p.color = Color.rgb(red, green, 0x26)
                    canvas.drawOval(o, p)
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        centerX = (right - left) / 2.0f
        centerY = (bottom - top) / 2.0f
    }
}