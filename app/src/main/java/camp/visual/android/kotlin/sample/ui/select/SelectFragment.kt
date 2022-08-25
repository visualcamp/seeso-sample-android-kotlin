package camp.visual.android.kotlin.sample.ui.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.window.layout.WindowMetricsCalculator
import camp.visual.android.kotlin.sample.R
import camp.visual.android.kotlin.sample.databinding.FragmentControlSelectBinding
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.gazetracker.state.TrackingState

class SelectFragment : Fragment() {
    // Fragment View Binding
    private var _binding: FragmentControlSelectBinding? = null
    private val binding get() = _binding!!

    // Display Size
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // ROI
    private var roiTopX = IntArray(2)
    private var roiTopY = IntArray(2)
    private var roiBottomX = IntArray(2)
    private var roiBottomY = IntArray(2)

    // Gaze Area Margin(Related to the screen size)
    private val marginRatio: Float = 0.5f

    // ROI Height (Related to the screen height)
    private val roiHeightRatio: Float = 0.3f

    // Button Progress
    private var progressBtnArray = arrayOf(0.0f, 0.0f)

    // Button trigger/recover speed per second
    private var triggerSpeed = 0.0f
    private var recoverSpeed = 0.0f

    // ROI Visibility
    private var visibilityROI = false

    // Item
    private val indexItemMin: Int = 0
    private val indexItemMax: Int = 10
    private var indexItem: Int = 0
    private var itemListArray = ArrayList<String>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlSelectBinding.inflate(inflater, container, false)
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

        // Set button Speed
        triggerSpeed = 60.0f
        recoverSpeed = 70.0f

        // Resize ROI
        resizeROI()

        // Item List
        for (i in indexItemMin..indexItemMax) {
            itemListArray.add(getString(R.string.title_item) + " $i")
        }

        // Item Setting
        setItemIndex(0)
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
        roiBottomY[0] = (screenHeight - (screenHeight * marginRatio)).toInt()
        roiBottomY[1] =
            roiBottomY[0] + (screenHeight * roiHeightRatio + (screenHeight * marginRatio)).toInt()

        binding.roiBottom.apply {
            val tmpParam = layoutParams

            x = roiBottomX[0].toFloat()
            y = roiBottomY[0].toFloat()
            tmpParam.width = roiBottomX[1] - roiBottomY[0]
            tmpParam.height = roiBottomY[1] - roiBottomY[0]

            layoutParams = tmpParam
            invalidate()
        }
    }

    // Item Index
    private fun setItemIndex(index: Int) {
        if (index < indexItemMin || indexItemMax < index) return


        indexItem = index

        activity?.runOnUiThread {
            binding.apply {
                // Previous Item Text
                textItemPrevious2.text =
                    if (index > indexItemMin + 1) itemListArray[index - 2] else ""
                textItemPrevious1.text = if (index > indexItemMin) itemListArray[index - 1] else ""
                // Next Item Text
                textItemNext2.text = if (index < indexItemMax - 1) itemListArray[index + 2] else ""
                textItemNext1.text = if (index < indexItemMax) itemListArray[index + 1] else ""

                textItemCurrent.text = itemListArray[index]

                btnPrevious.visibility = if (indexItemMin >= index) View.INVISIBLE else View.VISIBLE
                btnNext.visibility = if (index >= indexItemMax) View.INVISIBLE else View.VISIBLE
            }
        }
    }

    // Button Progress
    private fun progressButton(id: Int, isGazed: Boolean, fps: Int) {
        val addValue: Float = triggerSpeed / fps
        val subValue: Float = recoverSpeed / fps

        if (isGazed) {
            if (progressBtnArray[id] < 100) {
                progressBtnArray[id] += addValue
            } else {
                progressBtnArray[id] = 0.0f
                setItemIndex(if (id == 0) --indexItem else ++indexItem)
            }
        } else {
            if (progressBtnArray[id] > 0) {
                progressBtnArray[id] -= subValue
            } else {
                progressBtnArray[id] = 0.0f
            }
        }
    }

    // Update ROI
    fun updateROI() {
        visibilityROI = !visibilityROI
        // ROI Visibility
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
        if (roiTopX[0] <= x && x <= roiTopX[1]) {
            val idIndex = 1
            if (isValidGazeInfo) {
                progressButton(0, (y < roiTopY[1] && indexItemMin < indexItem), fps)
                progressButton(1, (roiBottomY[0] < y && indexItem < indexItemMax), fps)
            }
        }

        // Update Progress UI
        activity?.runOnUiThread {
            binding.apply {
                progressPrevious.progress = progressBtnArray[0].toInt()
                progressNext.progress = progressBtnArray[0].toInt()
            }
        }
    }
}