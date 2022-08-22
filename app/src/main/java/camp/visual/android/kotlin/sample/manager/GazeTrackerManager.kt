package camp.visual.android.kotlin.sample.manager

import android.content.Context
import android.view.TextureView
import camp.visual.gazetracker.GazeTracker
import camp.visual.gazetracker.callback.*
import camp.visual.gazetracker.constant.*
import java.lang.ref.WeakReference

public class GazeTrackerManager {

    private val initializationCallbacks: List<InitializationCallback> = ArrayList()
    private val gazeCallbacks: List<GazeCallback> = ArrayList()
    private val calibrationCallbacks: List<CalibrationCallback> = ArrayList()
    private val statusCallbacks: List<StatusCallback> = ArrayList()
    private val imageCallbacks: List<ImageCallback> = ArrayList()
    private val userStatusCallbacks: List<UserStatusCallback> = ArrayList()

    private var mInstance: GazeTrackerManager? = null

    private var cameraPreview: WeakReference<TextureView>? = null
    private var mContext: WeakReference<Context>? = null

    var gazeTracker: GazeTracker? = null

    // TODO: change licence key
    val SEESO_LICENSE_KEY = "dev_1ntzip9admm6g0upynw3gooycnecx0vl93hz8nox"

    fun makeNewInstance(context: Context?): GazeTrackerManager? {
        if (GazeTrackerManager.mInstance != null) {
            GazeTrackerManager.mInstance.deinitGazeTracker()
        }
        GazeTrackerManager.mInstance = GazeTrackerManager(context)
        return GazeTrackerManager.mInstance
    }

    fun getInstance(): GazeTrackerManager? {
        return GazeTrackerManager.mInstance
    }

    private fun GazeTrackerManager(context: Context) {
        mContext = WeakReference(context)
    }

    fun hasGazeTracker(): Boolean {
        return gazeTracker != null
    }

    fun initGazeTracker(callback: InitializationCallback?, option: UserStatusOption?) {
        initializationCallbacks.add(callback)
        GazeTracker.initGazeTracker(
            mContext!!.get(),
            SEESO_LICENSE_KEY,
            this.initializationCallbacks,
            option
        )
    }

    fun deinitGazeTracker() {
        if (hasGazeTracker()) {
            GazeTracker.deinitGazeTracker(gazeTracker)
            gazeTracker = null
        }
    }

    fun setGazeTrackerCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            if (callback is GazeCallback) {
                gazeCallbacks.add(callback as GazeCallback?)
            } else if (callback is CalibrationCallback) {
                calibrationCallbacks.add(callback as CalibrationCallback?)
            } else if (callback is ImageCallback) {
                imageCallbacks.add(callback as ImageCallback?)
            } else if (callback is StatusCallback) {
                statusCallbacks.add(callback as StatusCallback?)
            } else if (callback is UserStatusCallback) {
                userStatusCallbacks.add(callback as UserStatusCallback?)
            }
        }
    }

    fun removeCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            gazeCallbacks.remove(callback)
            calibrationCallbacks.remove(callback)
            imageCallbacks.remove(callback)
            statusCallbacks.remove(callback)
        }
    }

    fun startGazeTracking(): Boolean {
        if (hasGazeTracker()) {
            gazeTracker!!.startTracking()
            return true
        }
        return false
    }

    fun stopGazeTracking(): Boolean {
        if (isTracking()) {
            gazeTracker!!.stopTracking()
            return true
        }
        return false
    }

    fun startCalibration(modeType: CalibrationModeType?, criteria: AccuracyCriteria?): Boolean {
        return if (hasGazeTracker()) {
            gazeTracker!!.startCalibration(modeType, criteria)
        } else false
    }

    fun stopCalibration(): Boolean {
        if (isCalibrating()) {
            gazeTracker!!.stopCalibration()
            return true
        }
        return false
    }

    fun startCollectingCalibrationSamples(): Boolean {
        return if (isCalibrating()) {
            gazeTracker!!.startCollectSamples()
        } else false
    }

    fun isTracking(): Boolean {
        return if (hasGazeTracker()) {
            gazeTracker!!.isTracking
        } else false
    }

    fun isCalibrating(): Boolean {
        return if (hasGazeTracker()) {
            gazeTracker!!.isCalibrating
        } else false
    }

    enum class LoadCalibrationResult {
        SUCCESS, FAIL_DOING_CALIBRATION, FAIL_NO_CALIBRATION_DATA, FAIL_HAS_NO_TRACKER
    }

    fun loadCalibrationData(): LoadCalibrationResult? {
        if (!hasGazeTracker()) {
            return LoadCalibrationResult.FAIL_HAS_NO_TRACKER
        }
        val calibrationData: DoubleArray =
            CalibrationDataStorage.loadCalibrationData(mContext!!.get())
        return if (calibrationData != null) {
            if (!gazeTracker!!.setCalibrationData(calibrationData)) {
                LoadCalibrationResult.FAIL_DOING_CALIBRATION
            } else {
                LoadCalibrationResult.SUCCESS
            }
        } else {
            LoadCalibrationResult.FAIL_NO_CALIBRATION_DATA
        }
    }

    fun setCameraPreview(preview: TextureView) {
        cameraPreview = WeakReference(preview)
        if (hasGazeTracker()) {
            gazeTracker!!.setCameraPreview(preview)
        }
    }

    fun removeCameraPreview(preview: TextureView) {
        if (cameraPreview!!.get() === preview) {
            cameraPreview = null
            if (hasGazeTracker()) {
                gazeTracker!!.removeCameraPreview()
            }
        }
    }

    // GazeTracker Callbacks
    private val initializationCallback =
        InitializationCallback { gazeTracker, initializationErrorType ->
            setGazeTracker(gazeTracker)
            for (initializationCallback in initializationCallbacks) {
                initializationCallback.onInitialized(gazeTracker, initializationErrorType)
            }
            initializationCallbacks.clear()
            if (gazeTracker != null) {
                gazeTracker.setCallbacks(
                    gazeCallback,
                    calibrationCallback,
                    imageCallback,
                    statusCallback,
                    userStatusCallback
                )
                if (cameraPreview != null) {
                    gazeTracker.setCameraPreview(cameraPreview.get())
                }
            }
        }

    private val gazeCallback =
        GazeCallback { gazeInfo ->
            for (gazeCallback in gazeCallbacks) {
                gazeCallback.onGaze(gazeInfo)
            }
        }

    private val userStatusCallback: UserStatusCallback = object : UserStatusCallback {
        override fun onAttention(timestampBegin: Long, timestampEnd: Long, attentionScore: Float) {
            for (userStatusCallback in userStatusCallbacks) {
                userStatusCallback.onAttention(timestampBegin, timestampEnd, attentionScore)
            }
        }

        override fun onBlink(
            timestamp: Long,
            isBlinkLeft: Boolean,
            isBlinkRight: Boolean,
            isBlink: Boolean,
            eyeOpenness: Float
        ) {
            for (userStatusCallback in userStatusCallbacks) {
                userStatusCallback.onBlink(
                    timestamp,
                    isBlinkLeft,
                    isBlinkRight,
                    isBlink,
                    eyeOpenness
                )
            }
        }

        override fun onDrowsiness(timestamp: Long, isDrowsiness: Boolean) {
            for (userStatusCallback in userStatusCallbacks) {
                userStatusCallback.onDrowsiness(timestamp, isDrowsiness)
            }
        }
    }

    private val calibrationCallback: CalibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(v: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationProgress(v)
            }
        }

        override fun onCalibrationNextPoint(v: Float, v1: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationNextPoint(v, v1)
            }
        }

        override fun onCalibrationFinished(doubles: DoubleArray) {
            CalibrationDataStorage.saveCalibrationData(mContext!!.get(), doubles)
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationFinished(doubles)
            }
        }
    }

    private val imageCallback =
        ImageCallback { l, bytes ->
            for (imageCallback in imageCallbacks) {
                imageCallback.onImage(l, bytes)
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

    private fun setGazeTracker(gazeTracker: GazeTracker) {
        this.gazeTracker = gazeTracker
    }

}