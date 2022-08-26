package camp.visual.android.kotlin.sample.manager

import android.content.Context
import camp.visual.gazetracker.GazeTracker
import camp.visual.gazetracker.callback.*
import camp.visual.gazetracker.constant.AccuracyCriteria
import camp.visual.gazetracker.constant.CalibrationModeType
import camp.visual.gazetracker.constant.StatusErrorType
import camp.visual.gazetracker.constant.UserStatusOption
import java.lang.ref.WeakReference

class GazeTrackerManager private constructor(context: Context) {

    private val initializationCallbacks: MutableList<InitializationCallback> = ArrayList()
    private val gazeCallbacks: MutableList<GazeCallback> = ArrayList()
    private val calibrationCallbacks: MutableList<CalibrationCallback> = ArrayList()
    private val statusCallbacks: MutableList<StatusCallback> = ArrayList()

    private val mContext: WeakReference<Context> = WeakReference(context)
    private var gazeTracker: GazeTracker? = null

    // TODO: change licence key
    private val SEESO_LICENSE_KEY = "dev_1ntzip9admm6g0upynw3gooycnecx0vl93hz8nox"


    companion object {
        private var instance: GazeTrackerManager? = null
        fun makeNewInstance(context: Context): GazeTrackerManager? {
            instance.also { it?.deInitGazeTracker() }
            instance = GazeTrackerManager(context)
            return instance
        }
    }

    fun isTracking(): Boolean {
        return gazeTracker?.isTracking ?: false
    }

    fun isCalibrating(): Boolean {
        return gazeTracker?.isCalibrating ?: false
    }

    fun initGazeTracker(callback: InitializationCallback) {
        initializationCallbacks.add(callback)
        GazeTracker.initGazeTracker(
            mContext.get(),
            SEESO_LICENSE_KEY,
            initializationCallback,
            UserStatusOption()
        )
    }

    fun deInitGazeTracker() {
        gazeTracker?.also { GazeTracker.deinitGazeTracker(it) }
        gazeTracker = null
    }

    fun setGazeTrackerCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            when (callback) {
                is GazeCallback -> gazeCallbacks.add(callback)
                is CalibrationCallback -> calibrationCallbacks.add(callback)
                is StatusCallback -> statusCallbacks.add(callback)
            }
        }
    }

    fun removeCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            when (callback) {
                is GazeCallback -> gazeCallbacks.remove(callback)
                is CalibrationCallback -> calibrationCallbacks.remove(callback)
                is StatusCallback -> statusCallbacks.remove(callback)
            }
        }
    }

    fun setGazeTrackingFps(fps: Int): Boolean {
        return gazeTracker?.setTrackingFPS(fps) ?: false
    }

    fun startGazeTracking(): Boolean {
        gazeTracker?.also {
            it.startTracking()
            return true
        }
        return false
    }

    fun stopGazeTracking(): Boolean {
        gazeTracker?.also {
            it.stopTracking()
            return true
        }
        return false
    }

    // Start Calibration
    fun startCalibration(modeType: CalibrationModeType?, criteria: AccuracyCriteria?): Boolean {
        return if (isTracking()) {
            gazeTracker?.startCalibration(modeType, criteria)
            true
        } else {
            false
        }
    }

    fun stopCalibration(): Boolean {
        return if (isCalibrating()) {
            gazeTracker?.stopCalibration()
            true
        } else {
            false
        }
    }

    // Start Collect calibration sample data
    fun startCollectionCalibrationSamples(): Boolean {
        return if (isCalibrating()) {
            gazeTracker?.startCollectSamples()
            true
        } else {
            false
        }
    }

    // inner callbacks
    private val initializationCallback =
        InitializationCallback { gazeTracker, initializationErrorType ->
            this.gazeTracker = gazeTracker
            for (initializationCallback in initializationCallbacks) {
                initializationCallback.onInitialized(gazeTracker, initializationErrorType)
            }
            initializationCallbacks.clear()

            gazeTracker?.setTrackingFPS(30)
            gazeTracker?.setCallbacks(
                gazeCallback,
                calibrationCallback,
                statusCallback
            )
        }
    private val gazeCallback = GazeCallback { gazeInfo ->
        for (gazeCallback in gazeCallbacks) {
            gazeCallback.onGaze(gazeInfo)
        }
    }
    private val calibrationCallback: CalibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationProgress(progress)
            }
        }

        override fun onCalibrationNextPoint(x: Float, y: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationNextPoint(x, y)
            }
        }

        override fun onCalibrationFinished(calibrationData: DoubleArray) {
//            CalibrationDataStorage.saveCalibrationData(mContext.get(), calibrationData)
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationFinished(calibrationData)
            }
        }
    }
    private val statusCallback: StatusCallback = object : StatusCallback {
        override fun onStarted() {
            for (statusCallback in statusCallbacks) {
                statusCallback.onStarted()
            }
        }

        override fun onStopped(statusErrorType: StatusErrorType) {
            for (statusCallback in statusCallbacks) {
                statusCallback.onStopped(statusErrorType)
            }
        }
    }
}