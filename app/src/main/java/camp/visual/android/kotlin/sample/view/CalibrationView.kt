package camp.visual.android.kotlin.sample.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup

class CalibrationView : ViewGroup {
    private var calibrationPoint: CalibrationPoint? = null

    private var offsetX = 0.0f
    private var offsetY = 0.0f

    private val backgroundColor = Color.rgb(0x64, 0x5E, 0x5E)

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context)
    }

    private fun init(context: Context) {
        setBackgroundColor(backgroundColor)
        calibrationPoint = CalibrationPoint(context)
        addView(calibrationPoint)
    }

    fun setOffset(x: Int,y:Int) {
        offsetX = x.toFloat()
        offsetY = y.toFloat()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) { }

    fun setPointAnimationPower(power:Float) {
        calibrationPoint?.setPower(power)
    }

    fun setPointPosition(x:Float, y:Float) {
        val px = x - offsetX
        val py = y - offsetY
        calibrationPoint?.layout(
            px.toInt() - 20,
            py.toInt() - 20,
            px.toInt() + 20,
            py.toInt() + 20
        )
        invalidate()
    }
}