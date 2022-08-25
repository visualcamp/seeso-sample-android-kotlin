package camp.visual.android.kotlin.sample.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi

class PointView : View {
    private val defaultColor: Int = Color.rgb(0x00, 0x00, 0xff)
    private val outOffScreenColor: Int = Color.rgb(0xff, 0x00, 0x00)

    private var paint: Paint? = null
    private var showLine: Boolean = true

    private val position: PointF = PointF()
    private var offsetX = 0.0f
    private var offsetY = 0.0f

    companion object {
        const val TYPE_DEFAULT = 0
        const val TYPE_OUT_OF_SCRREN = 1
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        paint = Paint().also {
            it.color = defaultColor
            it.strokeWidth = 2f
        }
    }

    fun setOffset(x: Int, y: Int) {
        offsetX = x.toFloat()
        offsetY = y.toFloat()
    }

    fun setPosition(x: Float, y: Float) {
        position.x = x - offsetX
        position.y = y - offsetY
        invalidate()
    }

    fun hideLine() {
        showLine = false
    }

    fun showLine() {
        showLine = true
    }

    fun setType(type: Int) {
        paint?.color = if (type == TYPE_DEFAULT) defaultColor else outOffScreenColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint?.also { p ->
            canvas.drawCircle(position.x, position.y, 10.0f, p)
            if (showLine) {
                canvas.drawLine(0.0f, position.y, width.toFloat(), position.y, p)
                canvas.drawLine(position.x, 0.0f, position.x, height.toFloat(), p)
            }
        }
    }
}