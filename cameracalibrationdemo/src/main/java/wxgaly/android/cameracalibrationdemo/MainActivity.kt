package wxgaly.android.cameracalibrationdemo

import android.Manifest
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import wxgaly.android.cameracalibrationdemo.render.CalibrationFrameRender
import wxgaly.android.cameracalibrationdemo.render.ComparisonFrameRender
import wxgaly.android.cameracalibrationdemo.render.PreviewFrameRender
import wxgaly.android.cameracalibrationdemo.render.UndistortionFrameRender


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
        CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private val TAG = "MainActivity"

    private var mCalibrator: CameraCalibrator? = null
    private var mOnCameraFrameRender: OnCameraFrameRender? = null
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    cameraView.enableView()
                    cameraView.setOnTouchListener(this@MainActivity)
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        requireCameraPermission()
    }

    private fun initView() {
        cameraView.visibility = SurfaceView.VISIBLE
        cameraView.setCvCameraViewListener(this)
    }

    public override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.calibration, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.preview_mode).isEnabled = true
        mCalibrator?.apply {
            if (!isCalibrated)
                menu.findItem(R.id.preview_mode).isEnabled = false
        }


        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.calibration -> {
                mOnCameraFrameRender = OnCameraFrameRender(CalibrationFrameRender(mCalibrator))
                item.isChecked = true
                return true
            }
            R.id.undistortion -> {
                mOnCameraFrameRender = OnCameraFrameRender(UndistortionFrameRender(mCalibrator))
                item.isChecked = true
                return true
            }
            R.id.comparison -> {
                mOnCameraFrameRender = OnCameraFrameRender(ComparisonFrameRender(mCalibrator, mWidth, mHeight, resources))
                item.isChecked = true
                return true
            }
            R.id.calibrate -> {
                val res = resources
                mCalibrator?.apply {
                    if (cornersBufferSize < 2) {
                        Toast.makeText(this@MainActivity, res.getString(R.string.more_samples), Toast.LENGTH_SHORT).show()
                        return true
                    }
                }


                mOnCameraFrameRender = OnCameraFrameRender(PreviewFrameRender())
                val execute = object : AsyncTask<Void, Void, Void>() {
                    private var calibrationProgress: ProgressDialog? = null

                    override fun onPreExecute() {
                        calibrationProgress = ProgressDialog(this@MainActivity)
                        calibrationProgress!!.setTitle(res.getString(R.string.calibrating))
                        calibrationProgress!!.setMessage(res.getString(R.string.please_wait))
                        calibrationProgress!!.setCancelable(false)
                        calibrationProgress!!.isIndeterminate = true
                        calibrationProgress!!.show()
                    }

                    override fun doInBackground(vararg arg0: Void): Void? {
                        mCalibrator?.calibrate()
                        return null
                    }

                    override fun onPostExecute(result: Void) {
                        calibrationProgress!!.dismiss()
                        mCalibrator?.clearCorners()
                        mOnCameraFrameRender = OnCameraFrameRender(CalibrationFrameRender(mCalibrator))
                        mCalibrator?.apply {
                            val resultMessage = if (isCalibrated)
                                res.getString(R.string.calibration_successful) + " " + avgReprojectionError
                            else
                                res.getString(R.string.calibration_unsuccessful)
                            Toast.makeText(this@MainActivity, resultMessage, Toast.LENGTH_SHORT).show()

                            if (isCalibrated) {
                                CalibrationResult.save(this@MainActivity, cameraMatrix, distortionCoefficients)
                            }
                        }

                    }
                }
                execute.execute()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        if (mWidth !== width || mHeight !== height) {
            mWidth = width
            mHeight = height
            mCalibrator = CameraCalibrator(mWidth, mHeight)
            mCalibrator?.apply {
                if (CalibrationResult.tryLoad(this@MainActivity, cameraMatrix, distortionCoefficients)) {
                    setCalibrated()
                }
            }

            mOnCameraFrameRender = OnCameraFrameRender(CalibrationFrameRender(mCalibrator))
        }
    }

    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        return mOnCameraFrameRender?.render(inputFrame)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        Log.d(TAG, "onTouch invoked")

        mCalibrator?.addCorners()
        return false
    }

    @AfterPermissionGranted(CAMERA)
    private fun requireCameraPermission() {
        val perms = Manifest.permission.CAMERA

        if (EasyPermissions.hasPermissions(this, perms)) {
            Log.d(TAG, "the permission has granted.")
            initView()
        } else {
            EasyPermissions.requestPermissions(
                    PermissionRequest.Builder(this, CAMERA, perms)
                            .setRationale(R.string.camera)
                            .setPositiveButtonText(R.string.yes)
                            .setNegativeButtonText(R.string.no)
                            .setTheme(R.style.AppTheme)
                            .build())
        }

    }


    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "the permission has denied.")
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        initView()
    }

}
