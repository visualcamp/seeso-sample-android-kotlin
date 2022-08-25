package camp.visual.android.kotlin.sample.ui.scroll

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.window.layout.WindowMetricsCalculator
import camp.visual.android.kotlin.sample.R
import camp.visual.android.kotlin.sample.databinding.FragmentControlScrollBinding
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.gazetracker.state.TrackingState

class ScrollFragment : Fragment() {
    // Fragment View Binding
    private var _binding: FragmentControlScrollBinding? = null
    private val binding get() = _binding!!

    // Display Size
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // ROI
    private var roiTopX = IntArray(2)
    private var roiTopY = IntArray(2)
    private var roiBottomX = IntArray(2)
    private var roiBottomY = IntArray(2)

    // ROI Height (Related to the screen height)
    private val roiHeightRatio: Float = 0.3f

    // Gaze Area Margin(Related to the screen size)
    private val marginRatio: Float = 0.5f

    // Scroll speed per second
    private var scrollSpeed: Int = 0

    // ROI Visibility
    private var visibilityROI = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlScrollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Screen Size
        activity?.also { a ->
            val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(a)
            val currentBounds = windowMetrics.bounds

            screenWidth = currentBounds.width()
            screenHeight = currentBounds.height()
        }

        // Set scroll Speed
        scrollSpeed = screenHeight / 5

        // Resize ROI
        resizeROI()

        // Set Text
        var text: String = ""
        for (i in 0 until 100) {
            text += getString(R.string.title_line).toString() + " $i\n"
        }
        binding.textContent.text = text
    }

    // Resize ROI
    private fun resizeROI() {
        // ROI top
        roiTopX[0] = (-screenWidth * marginRatio).toInt()
        roiTopX[1] = roiTopX[0] + (screenWidth + (screenWidth * marginRatio * 2)).toInt()
        roiTopY[0] = (-screenHeight * marginRatio).toInt()
        roiTopY[1] =
            roiTopY[0] + (screenHeight * roiHeightRatio + (screenHeight * marginRatio)).toInt()

        binding.roiTop.apply {
            val tmpParam = layoutParams

            x = roiTopX[0].toFloat()
            y = roiTopY[0].toFloat()

            tmpParam.width = roiTopX[1] - roiTopX[0]
            tmpParam.height = roiTopY[1] - roiTopY[0]

            layoutParams = tmpParam
            invalidate()
        }

        // ROI bottom
        roiBottomX[0] = (-screenWidth * marginRatio).toInt()
        roiBottomX[1] = roiBottomX[0] + (screenWidth + (screenWidth * marginRatio * 2)).toInt()
        roiBottomY[0] = (screenHeight - (screenHeight * roiHeightRatio)).toInt()
        roiBottomY[1] =
            roiBottomY[0] + (screenHeight * roiHeightRatio + (screenHeight * marginRatio)).toInt()

        binding.roiBottom.apply {
            val tmpParam = layoutParams

            x = roiBottomX[0].toFloat()
            y = roiBottomY[0].toFloat()

            tmpParam.width = roiBottomX[1] - roiBottomX[0]
            tmpParam.height = roiBottomY[1] - roiBottomY[0]

            layoutParams = tmpParam
            invalidate()
        }
    }

    // Update ROI
    fun updateROI() {
        visibilityROI = !visibilityROI

        binding.apply {
            roiTop.visibility = if (visibilityROI) View.VISIBLE else View.INVISIBLE
            roiBottom.visibility = if (visibilityROI) View.VISIBLE else View.INVISIBLE
        }
    }

    // Gaze Data
    fun onGaze(gazeInfo: GazeInfo, fps: Int) {
        val x: Float = gazeInfo.x
        val y: Float = gazeInfo.y

        // Gaze Validation
        val isValidGazeInfo: Boolean =
            gazeInfo.trackingState == TrackingState.SUCCESS ||
                    gazeInfo.trackingState == TrackingState.LOW_CONFIDENCE

        // ROI Events

        if (isValidGazeInfo && roiTopX[0] <= x && x <= roiTopX[1]) {
            val speed =
                if (y < roiTopY[1]) {
                    -scrollSpeed
                } else if (roiBottomY[0] < y) {
                    scrollSpeed
                } else {
                    0
                }
            binding.scrollViewContents.apply {
                smoothScrollBy(0, speed / fps)
            }
        }
    }
}