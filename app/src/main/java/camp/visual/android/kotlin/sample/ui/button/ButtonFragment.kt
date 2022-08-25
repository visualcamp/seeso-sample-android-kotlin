package camp.visual.android.kotlin.sample.ui.button

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.window.layout.WindowMetricsCalculator
import camp.visual.android.kotlin.sample.R
import camp.visual.android.kotlin.sample.databinding.FragmentControlButtonBinding
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.gazetracker.state.TrackingState


class ButtonFragment : Fragment() {
    // Fragment View Binding
    private var _binding: FragmentControlButtonBinding? = null
    private val binding get() = _binding!!

    // Display Size
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // ROI
    private var roiPrevX = IntArray(2)
    private var roiPrevY = IntArray(2)
    private var roiNextX = IntArray(2)
    private var roiNextY = IntArray(2)

    // Button Area Margin(Related to the button size)
    private val marginRatio: Float = 0.5f

    // Button Progress
    private var progressBtnArray = arrayOf(0.0f, 0.0f)

    // Button trigger/recover speed per second
    private var triggerSpeed: Float = 0.0f
    private var recoverSpeed: Float = 0.0f

    // ROI Visibility
    private var visibilityROI = false

    // Page
    private val indexPageMin: Int = 0
    private val indexPageMax: Int = 5
    private var indexPage: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlButtonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // save Screen Size
        activity?.also { a ->
            val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(a)
            val currentBounds = windowMetrics.bounds

            screenWidth = currentBounds.width()
            screenHeight = currentBounds.height()
        }
        // set button speed
        triggerSpeed = 60.0f
        recoverSpeed = 70.0f
    }

    private fun resizeROI() {
        // ROI previous
        binding.btnPrevious.apply {
            val width = layoutParams.width
            val height = layoutParams.height

            roiPrevX[0] = (-width * marginRatio).toInt()
            roiPrevX[1] = (roiPrevX[0] + width + (width * marginRatio * 2)).toInt()

            roiPrevY[0] = (-height * marginRatio).toInt()
            roiPrevY[1] = (roiPrevY[0] + height + (height * marginRatio * 2)).toInt()

            binding.roiPrevious.apply {
                val tmpParam = layoutParams
                x = roiPrevX[0].toFloat()
                y = roiPrevY[0].toFloat()
                tmpParam.width = roiPrevX[1] - roiPrevX[0]
                tmpParam.height = roiPrevY[1] - roiPrevY[0]

                layoutParams = tmpParam
                invalidate()
            }
        }

        // ROI next
        binding.btnNext.apply {
            val width = layoutParams.width
            val height = layoutParams.height

            roiNextX[0] = (screenWidth - width - (width * marginRatio)).toInt()
            roiNextX[1] = (roiNextX[0] + width + (width * marginRatio * 2)).toInt()

            roiNextY[0] = (-height * marginRatio).toInt()
            roiNextY[1] = (roiNextY[0] + height + (height * marginRatio * 2)).toInt()

            binding.roiNext.apply {
                val tmpParam = layoutParams
                x = roiNextX[0].toFloat()
                y = roiNextY[0].toFloat()
                tmpParam.width = roiNextX[1] - roiNextX[0]
                tmpParam.height = roiNextY[1] - roiNextY[0]

                layoutParams = tmpParam
                invalidate()
            }
        }
    }

    // Page Index
    private fun setPageIndex(index: Int) {
        if (index < indexPageMin || indexPageMax < index) return

        indexPage = index

        activity?.runOnUiThread {
            binding.textPage.text = getString(R.string.title_page) + " $index"

            binding.btnPrevious.visibility =
                if (indexPageMin >= index) View.INVISIBLE else View.VISIBLE
            binding.btnNext.visibility = if (index >= indexPageMax) View.INVISIBLE else View.VISIBLE
        }
    }

    // Button Progress
    private fun progressButton(id: Int, isGazed: Boolean, fps: Int) {
        val addValue: Float = triggerSpeed / fps
        val subValue: Float = recoverSpeed / fps

        var btn = progressBtnArray[id]
        if (isGazed) {
            if (btn < 100) {
                btn += addValue
            } else {
                btn = 0.0f
                when (id) {
                    0 -> --indexPage
                    1 -> ++indexPage
                    else -> print("never come here")
                }
            }
        } else {
            btn = if (btn > 0) btn - subValue else 0.0f
        }
    }

    // Update ROI
    fun updateROI() {
        visibilityROI = !visibilityROI

        binding.apply {
            roiPrevious.visibility = if (visibilityROI) View.VISIBLE else View.INVISIBLE
            roiNext.visibility = if (visibilityROI) View.VISIBLE else View.INVISIBLE
        }
    }

    fun onGaze(gazeInfo: GazeInfo, fps: Int) {
        val x: Float = gazeInfo.x
        val y: Float = gazeInfo.y

        // Gaze Validation
        val isValidGazeInfo: Boolean =
            gazeInfo.trackingState == TrackingState.SUCCESS || gazeInfo.trackingState == TrackingState.LOW_CONFIDENCE

        // ROI Events
        if (binding.btnPrevious != null && indexPageMin < indexPage) {
            val isGazed =
                isValidGazeInfo && roiPrevX[0] <= x && x <= roiPrevX[1] && roiPrevY[0] <= y && y <= roiPrevY[1]
            progressButton(0, isGazed, fps)
        }

        if (binding.btnNext != null && indexPage < indexPageMax) {
            val isGazed =
                isValidGazeInfo && roiNextX[0] <= x && x <= roiNextX[1] && roiNextY[0] <= y && y <= roiNextY[1]
            progressButton(1, isGazed, fps)
        }

        // Update Progress UI
        activity?.runOnUiThread {
            binding.apply {
                progressPrevious.progress = progressBtnArray[0].toInt()
                progressNext.progress = progressBtnArray[1].toInt()
            }
        }
    }
}