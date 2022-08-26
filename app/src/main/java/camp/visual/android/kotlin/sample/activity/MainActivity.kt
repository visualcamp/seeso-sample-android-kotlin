package camp.visual.android.kotlin.sample.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import camp.visual.android.kotlin.sample.R
import camp.visual.android.kotlin.sample.databinding.ActivityMainBinding
import camp.visual.android.kotlin.sample.manager.GazeTrackerManager
import camp.visual.android.kotlin.sample.ui.button.ButtonFragment
import camp.visual.android.kotlin.sample.ui.scroll.ScrollFragment
import camp.visual.android.kotlin.sample.ui.select.SelectFragment
import camp.visual.android.kotlin.sample.view.PointView
import camp.visual.gazetracker.callback.CalibrationCallback
import camp.visual.gazetracker.callback.GazeCallback
import camp.visual.gazetracker.callback.InitializationCallback
import camp.visual.gazetracker.callback.StatusCallback
import camp.visual.gazetracker.constant.AccuracyCriteria
import camp.visual.gazetracker.constant.CalibrationModeType
import camp.visual.gazetracker.constant.StatusErrorType
import camp.visual.gazetracker.filter.OneEuroFilterManager
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.gazetracker.state.ScreenState
import camp.visual.gazetracker.util.ViewLayoutChecker
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val logTag = "SeeSo"

    // Required Permissions
    private val permissions = arrayOf(
        android.Manifest.permission.CAMERA
    )
    private val permissionCode: Int = 1000

    // view
    private var navigationHostFragment: Fragment? = null

    // SeeSo
    private var gazeTrackerManager: GazeTrackerManager? = null
    private val viewLayoutChecker: ViewLayoutChecker = ViewLayoutChecker()
    private val backgroundThread: HandlerThread = HandlerThread("background")
    private var backgroundHandler: Handler? = null
    private val trackingFps: Int = 30

    // screen Offset
    private var offsets: IntArray = IntArray(2)

    // Calibration
    private val calibrationType: CalibrationModeType = CalibrationModeType.DEFAULT
    private val criteria: AccuracyCriteria = AccuracyCriteria.DEFAULT

    // Filter
    private val oneEuroFilterManager: OneEuroFilterManager = OneEuroFilterManager(2)
    private val isUseGazeFilter: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        // SeeSo GazeTracker Manager
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this)

        // View
        navigationHostFragment = supportFragmentManager.findFragmentByTag("navHostFragment")

        // Handler
        initHandler()

        // Button
        initButtonEvent()

        // Permission
        checkPermission()
    }

    override fun onStart() {
        super.onStart()
        gazeTrackerManager?.setGazeTrackerCallbacks(
            gazeCallback,
            calibrationCallback,
            statusCallback
        )
    }

    override fun onResume() {
        super.onResume()

        // Set Offset for Views
        setOffsetOfView()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Handler
        releaseHandler()
    }

    // permission
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(permissions)) {
                requestPermissions(permissions, permissionCode)
                return
            }
        }
        checkPermissionGranted(true)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (perms in permissions) {
            if (perms == Manifest.permission.SYSTEM_ALERT_WINDOW) {
                if (!Settings.canDrawOverlays(this)) {
                    return false
                }
            }
            val result = ContextCompat.checkSelfPermission(this, perms)
            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionCode -> if (grantResults.isNotEmpty()) {
                checkPermissionGranted(grantResults.first() == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    private fun checkPermissionGranted(isGranted: Boolean) {
        if (isGranted) permissionGranted() else finish()
    }

    private fun permissionGranted() {
        initGaze()
    }

    // Handler
    private fun initHandler() {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun releaseHandler() {
        backgroundThread.quitSafely()
    }

    // Button Event
    private fun initButtonEvent() {
        binding.apply {
            btnCalibration.setOnClickListener {
                if (isTracking()) {
                    startCalibration()
                }
            }

            btnRoi.setOnClickListener {
                navigationHostFragment?.childFragmentManager?.fragments?.also { fs ->
                    for (fragment in fs) {
                        val buttonFragment = fragment as? ButtonFragment
                        val selectFragment = fragment as? SelectFragment
                        val scrollFragment = fragment as? ScrollFragment
                        buttonFragment?.updateROI()
                        selectFragment?.updateROI()
                        scrollFragment?.updateROI()
                    }
                }
            }
        }
    }

    // View
    private fun setOffsetOfView() {
        viewLayoutChecker.setOverlayView(binding.viewPoint as View) { x, y ->
            binding.viewPoint?.setOffset(x, y)
            binding.viewCalibration?.setOffset(x, y)

            offsets[0] = x
            offsets[1] = y
        }
    }

    private fun showGazePoint(x: Float, y: Float, type: ScreenState) {
        runOnUiThread {
            binding.viewPoint.apply {
                setType(if (type == ScreenState.INSIDE_OF_SCREEN) PointView.TYPE_DEFAULT else PointView.TYPE_OUT_OF_SCRREN)
                setPosition(x, y)
            }
        }
    }

    private fun setCalibrationPoint(x: Float, y: Float) {
        runOnUiThread {
            binding.viewCalibration?.apply {
                setPointPosition(x, y)
                setPointAnimationPower(0.0f)
            }
        }
    }

    private fun setCalibrationProgress(progress: Float) {
        runOnUiThread {
            binding.viewCalibration?.setPointAnimationPower(progress)
        }
    }

    private fun showCalibrationView() {
        runOnUiThread {
            binding.apply {
                viewCalibration?.visibility = View.VISIBLE
                viewPoint?.visibility = View.INVISIBLE
                navView?.visibility = View.INVISIBLE
            }
        }
    }

    private fun hideCalibrationView() {
        runOnUiThread {
            binding.apply {
                viewCalibration?.visibility = View.INVISIBLE
                viewPoint?.visibility = View.VISIBLE
                navView.visibility = View.VISIBLE
            }
        }
    }

    // Gaze Filter (OneEuro)
    private fun processFilterGaze(gazeInfo: GazeInfo): FloatArray {
        if (isUseGazeFilter) {
            if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                return oneEuroFilterManager.filteredValues
            }
        }
        return floatArrayOf(gazeInfo.x, gazeInfo.y)
    }

    // SeeSo GazeTracker Functions
    private fun initGaze() {
        gazeTrackerManager?.initGazeTracker(initializationCallback)
    }

    private fun releaseGaze() {
        gazeTrackerManager?.deInitGazeTracker()
    }

    private fun isTracking(): Boolean {
        return gazeTrackerManager?.isTracking() ?: false
    }

    private fun startTracking() {
        gazeTrackerManager?.startGazeTracking()
    }

    private fun stopTracking() {
        gazeTrackerManager?.stopGazeTracking()
    }

    private fun setTrackingFps(fps: Int) {
        gazeTrackerManager?.setGazeTrackingFps(fps)
    }

    private fun startCalibration() {
        gazeTrackerManager?.startCalibration(calibrationType, criteria)
        showCalibrationView()
    }

    private fun startCollectSample() {
        gazeTrackerManager?.startCollectionCalibrationSamples()
    }

    private fun stopCalibration() {
        gazeTrackerManager?.stopCalibration()
    }


    // SeeSo GazeTracker Callback
    private val initializationCallback =
        InitializationCallback { gazeTracker, error ->
            // onInitialized
            if (gazeTracker != null) {
                setTrackingFps(trackingFps)
                startTracking()
            } else {
                showToast("SeeSo GazeTracker Init Failed: $error")
            }
        }


    private val gazeCallback = GazeCallback { gazeInfo ->
        // onGaze
        if (gazeTrackerManager?.isCalibrating() == false) {
            val gaze = processFilterGaze(gazeInfo)

            runOnUiThread {
                showGazePoint(gaze[0], gaze[1], gazeInfo.screenState)
            }

            gazeInfo.x = gaze[0] - offsets[0]
            gazeInfo.y = gaze[1] - offsets[1]

            navigationHostFragment?.childFragmentManager?.fragments?.also { fs ->
                for (fragment in fs) {
                    val buttonFragment = fragment as? ButtonFragment
                    val scrollFragment = fragment as? ScrollFragment
                    val selectFragment = fragment as? SelectFragment
                    buttonFragment?.onGaze(gazeInfo, trackingFps)
                    scrollFragment?.onGaze(gazeInfo, trackingFps)
                    selectFragment?.onGaze(gazeInfo, trackingFps)
                }
            }
        }
    }
    private val calibrationCallback: CalibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {
            setCalibrationProgress(progress)
        }

        override fun onCalibrationNextPoint(x: Float, y: Float) {
            setCalibrationPoint(x, y)

            backgroundHandler?.postDelayed({
                startCollectSample()
            }, 1000)
        }

        override fun onCalibrationFinished(calibrationData: DoubleArray?) {
            hideCalibrationView()
        }
    }
    private val statusCallback: StatusCallback = object : StatusCallback {
        override fun onStarted() {

        }

        override fun onStopped(error: StatusErrorType?) {
            when (error) {
                StatusErrorType.ERROR_CAMERA_START ->
                    showToast("ERROR_CAMERA_START")
                StatusErrorType.ERROR_CAMERA_INTERRUPT ->
                    showToast("ERROR_CAMERA_INTERRUPT")
                else -> {}
            }
        }
    }

    private fun showToast(msg: String, isShort: Boolean = false) {
        runOnUiThread {
            Toast.makeText(
                this,
                msg,
                if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }
}