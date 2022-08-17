package camp.visual.android.kotlin.sample.activity


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import camp.visual.android.kotlin.sample.databinding.ActivityMainBinding
import camp.visual.android.kotlin.sample.manager.GazeTrackerManager
import camp.visual.android.kotlin.sample.manager.SeeSoInitializeState
import camp.visual.gazetracker.GazeTracker
import camp.visual.gazetracker.callback.*
import camp.visual.gazetracker.constant.AccuracyCriteria
import camp.visual.gazetracker.constant.CalibrationModeType
import camp.visual.gazetracker.constant.InitializationErrorType
import camp.visual.gazetracker.constant.StatusErrorType
import camp.visual.gazetracker.gaze.GazeInfo


@SuppressLint("ClickableViewAccessibility")
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // Permissions
    private val permissions = arrayOf(
        Manifest.permission.CAMERA
    )
    private val permissionCode: Int = 1000

    // SeeSo
    private var gazeTrackerManager: GazeTrackerManager? = null
    var isGazeTrackingStarting = false
    var gazeTrackerFPS: Int = 30

    // SeeSo UserStatus
    var recentAttentionScore: Int = 0
    var blinked = false
    var isSleepy = false

    // Thread control
    private val backgroundThread: HandlerThread = HandlerThread("background")
    private var backgroundHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this)
        Log.i("SeeSo", "SeeSo GazeTracker version: " + GazeTracker.getVersionName())

        initHandler()
        addTouchListenerToViews()
        checkPermission()

    }

    override fun onStart() {
        super.onStart()
        gazeTrackerManager?.setGazeTrackerCallbacks(
            gazeCallback,
            calibrationCallback,
            statusCallback,
            userStatusCallback
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseHandler()
    }

    // Thread control
    private fun initHandler() {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun releaseHandler() {
        backgroundThread.quitSafely()
    }

    // Permission Check
    private fun checkPermission() {
        if (hasPermissions(permissions)) {
            permissionsGranted()
        }
    }

    private fun hasPermissions(permission: Array<String>): Boolean {
        for (perms in permissions) {
            if ((perms == Manifest.permission.SYSTEM_ALERT_WINDOW) && !Settings.canDrawOverlays(this)) {
                return false
            }
            val check = ContextCompat.checkSelfPermission(this, perms)
            if (check == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    private fun checkPermission(isGranted: Boolean) {
        if (isGranted) permissionsGranted() else finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionCode -> if (grantResults.isNotEmpty()) {
                checkPermission(grantResults.first() == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    private fun permissionsGranted() {
        updateViewState()
    }

    // View Setup
    private fun addTouchListenerToViews() {
        binding.apply {
            requestPermissionButton.setOnTouchListener { _, _ ->
                requestPermissions(permissions, permissionCode)
                true
            }

            initGazeTrackerButton.setOnTouchListener { _, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (gazeTrackerManager?.initializeState == SeeSoInitializeState.default) {
                            initGazeTracker()
                            updateViewState()
                        } else {
                            deinitGazeTracker()
                        }

                    }
                }

                true
            }

            startTrackingButton.setOnTouchListener { _, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (gazeTrackerManager?.isTracking() != true) {
                            startTracking()
                        } else {
                            stopTracking()
                        }
                    }
                }
                true
            }
            startCalibrationButton.setOnTouchListener { _, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startCalibration()
                    }
                }
                true
            }
        }
    }

    private fun updateViewState() {
        runOnUiThread {

            binding.apply {

                // ------ permission ------ //
                permissionContainer.visibility =
                    if (!hasPermissions(permissions)) View.VISIBLE else View.GONE
                upperInitButtonGuideContainer.visibility =
                    if (!hasPermissions(permissions)) View.GONE else View.VISIBLE


                val isInitializing =
                    (gazeTrackerManager?.initializeState == SeeSoInitializeState.initializing)

                // ------ loading ------ //
                loadingView.visibility =
                    if (isInitializing || isGazeTrackingStarting) View.VISIBLE else View.GONE
                loadingText.text = if (isInitializing) {
                    "Initializing Gaze Tracker.."
                } else if (isGazeTrackingStarting) {
                    "Starting Gaze Tracking.."
                } else {
                    ""
                }

                // ------ variables ------ //
                val isDefault =
                    gazeTrackerManager?.initializeState == SeeSoInitializeState.default
                val isInitialized =
                    gazeTrackerManager?.initializeState == SeeSoInitializeState.initialized
                val initializedWithOption = gazeTrackerManager?.isInitWithUserOption == true
                val isTracking = gazeTrackerManager?.isTracking() == true
                val isCalibrating = gazeTrackerManager?.isCalibrating() == true


                // ------ init ------ //
                guideInitGazeTracker.text =
                    if (isDefault) {
                        "Your need to init GazeTracker first."
                    } else {
                        var base = "GazeTracker is activated"
                        if (initializedWithOption) {
                            "$base with User Options!"
                        } else {
                            "$base!"
                        }
                    }

                initGazeTrackerButton.text =
                    if (!isInitialized) "Initialize GazeTracker" else "Stop GazeTracker"
                initUserOptionContainer.visibility = if (!isInitialized) View.VISIBLE else View.GONE
                guideInitWithUserOption.visibility =
                    if (isInitialized && !initializedWithOption) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                // ------ tracking ------ //
                gazePointView.apply {
                    pivotX = (width / 2).toFloat()
                    pivotY = (height / 2).toFloat()
                }
                upperStartTrackingContainer.visibility =
                    if (!isInitialized) View.GONE else View.VISIBLE
                startTrackingButton.text = if (isTracking) "Stop Tracking" else "Start Tracking"
                gazePointViewContainer.visibility =
                    if (isTracking && !isCalibrating) View.VISIBLE else View.GONE


                // ------ calibration ------ //
                calibrationIcon.apply {
                    pivotX = (width / 2).toFloat()
                    pivotY = (height / 2).toFloat()
                }
                calibrationContainer.visibility =
                    if (isTracking) View.VISIBLE else View.GONE
                calibrationViewContainer.visibility = if (isCalibrating) View.VISIBLE else View.GONE

                // ------ user options info ------ //
                userStatusContainer.visibility =
                    if (isTracking && initializedWithOption) View.VISIBLE else View.GONE
                if (initializedWithOption) {
                    attentionScore.text = ""
                    blinkState.text = ""
                    sleepyState.text = ""
                }
            }
        }
    }

    private fun updateUserStatusView() {
        if (gazeTrackerManager?.isInitWithUserOption == true) {
            runOnUiThread {
                binding.apply {
                    attentionScore.text = "$recentAttentionScore%"
                    blinkState.text = if (blinked) "X_X" else "O_O"
                    sleepyState.text = if (isSleepy) "yes.." else "nope!"
                }
            }
        }
    }

    // SeeSo GazeTracker Functions
    private fun initGazeTracker() {
        val withOption = binding.switchInitUserOption.isChecked
        gazeTrackerManager?.initGazeTracker(initializationCallback, withOption)

        // You can also set FPS of gazeTracker if you want
        gazeTrackerManager?.setGazeTrackingFps(gazeTrackerFPS)
    }

    private fun deinitGazeTracker() {
        gazeTrackerManager?.deInitGazeTracker()
        updateViewState()
    }

    private fun startTracking() {
        backgroundHandler?.post {
            gazeTrackerManager?.startGazeTracking()
        }
        isGazeTrackingStarting = true
        updateViewState()

    }

    private fun stopTracking() {
        gazeTrackerManager?.stopGazeTracking()
        updateViewState()
    }

    private fun startCalibration() {
        val mode =
            if (binding.onePoint.isChecked) CalibrationModeType.ONE_POINT else CalibrationModeType.FIVE_POINT
        gazeTrackerManager?.startCalibration(mode, AccuracyCriteria.DEFAULT)
        updateViewState()
    }

    private fun startCollectSamples() {
        gazeTrackerManager?.startCollectionCalibrationSamples()
    }


    // SeeSo GazeTrackerCallback
    private val initializationCallback = object : InitializationCallback {
        // Note: for understanding, left as function here
        // you can change this to lambda
        override fun onInitialized(gazeTracker: GazeTracker?, error: InitializationErrorType?) {
            runOnUiThread {
                if (gazeTracker != null) {
                    updateViewState()
                } else {
                    // Check https://docs.seeso.io/docs/document/authentication-overview
                    showToast("SeeSo GazeTracker Init Failed\nerror:${error}")
                }
            }
        }
    }
    private val statusCallback = object : StatusCallback {
        override fun onStarted() {
            // will be called after gaze tracking started
        }

        override fun onStopped(error: StatusErrorType?) {
            // Check https://docs.seeso.io/docs/api/android-api-docs#statuserrortype
            when (error) {
                StatusErrorType.ERROR_NONE -> {}
                StatusErrorType.ERROR_CAMERA_START -> {}
                StatusErrorType.ERROR_CAMERA_INTERRUPT -> {}
                else -> {}
            }
        }
    }
    private val gazeCallback = object : GazeCallback {
        override fun onGaze(gazeInfo: GazeInfo) {
            if (isGazeTrackingStarting) {
                isGazeTrackingStarting = false
                updateViewState()
            }
            if (gazeTrackerManager?.isCalibrating() == false) {
                runOnUiThread {
                    binding.gazePointView.apply {
                        x = gazeInfo.x
                        y = gazeInfo.y
                    }
                }
            }
        }
    }
    private val calibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {
            runOnUiThread {
                binding.calibrationPercentText.text = "${((progress * 100).toInt()).toString()}%"
            }
        }

        override fun onCalibrationNextPoint(fx: Float, fy: Float) {
            binding.calibrationIcon.apply {
                // TODO: absolute/perfect coordinates calculation fix
                runOnUiThread {
                    x = fx
                    y = fy
                }
            }

            backgroundHandler?.postDelayed({
                startCollectSamples()
            }, 1000)
        }

        override fun onCalibrationFinished(calibrationData: DoubleArray?) {
            updateViewState()
        }
    }

    private val userStatusCallback = object : UserStatusCallback {
        override fun onAttention(timestampBegin: Long, timestampEnd: Long, score: Float) {
            recentAttentionScore = (score * 100).toInt()
            updateUserStatusView()
        }

        override fun onBlink(
            timestamp: Long,
            isBlinkLeft: Boolean,
            isBlinkRight: Boolean,
            isBlink: Boolean,
            leftOpenness: Float,
            rightOpenness: Float
        ) {
            if (isBlink) {
                blinked = true
                backgroundHandler?.postDelayed({
                    blinked = false
                }, 400)
            }
            updateUserStatusView()
        }

        override fun onDrowsiness(timestamp: Long, isDrowsiness: Boolean, intensity : Float) {
            isSleepy = isDrowsiness
            updateUserStatusView()
        }
    }

    // ----- Toast Helper ----- //
    private fun showToast(msg: String, isShort: Boolean = true) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}