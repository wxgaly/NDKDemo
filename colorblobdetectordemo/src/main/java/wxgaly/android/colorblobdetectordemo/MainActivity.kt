package wxgaly.android.colorblobdetectordemo

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, View.OnTouchListener,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "MainActivity"

    private var mIsColorSelected = false
    private var mRgba: Mat? = null
    private var mBlobColorRgba: Scalar? = null
    private var mBlobColorHsv: Scalar? = null
    private var mDetector: ColorBlobDetector? = null
    private var mSpectrum: Mat? = null
    private var SPECTRUM_SIZE: Size? = null
    private var CONTOUR_COLOR: Scalar? = null


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
        requestWindowFeature(Window.FEATURE_NO_TITLE)
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


    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mDetector = ColorBlobDetector()
        mSpectrum = Mat()
        mBlobColorRgba = Scalar(255.0)
        mBlobColorHsv = Scalar(255.0)
        SPECTRUM_SIZE = Size(200.0, 64.0)
        CONTOUR_COLOR = Scalar(255.0, 0.0, 0.0, 255.0)
    }

    override fun onCameraViewStopped() {
        mRgba?.release()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mRgba?.apply {
            val cols = cols()
            val rows = rows()

            val xOffset = (cameraView.width - cols) / 2
            val yOffset = (cameraView.height - rows) / 2

            val x = event.x.toInt() - xOffset
            val y = event.y.toInt() - yOffset

            Log.i(TAG, "Touch image coordinates: ($x, $y)")

            if (x < 0 || y < 0 || x > cols || y > rows) return false

            val touchedRect = Rect()

            touchedRect.x = if (x > 4) x - 4 else 0
            touchedRect.y = if (y > 4) y - 4 else 0

            touchedRect.width = if (x + 4 < cols) x + 4 - touchedRect.x else cols - touchedRect.x
            touchedRect.height = if (y + 4 < rows) y + 4 - touchedRect.y else rows - touchedRect.y

            val touchedRegionRgba = submat(touchedRect)

            val touchedRegionHsv = Mat()
            Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL)

            // Calculate average color of touched region
            mBlobColorHsv = Core.sumElems(touchedRegionHsv)
            val pointCount = touchedRect.width * touchedRect.height
            mBlobColorHsv?.apply {
                for (i in 0 until `val`.size) {
                    `val`[i] = `val`[i].rem(pointCount)
                }
            }

            mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv)

            Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba!!.`val`[0] + ", " + mBlobColorRgba!!.`val`[1] +
                    ", " + mBlobColorRgba!!.`val`[2] + ", " + mBlobColorRgba!!.`val`[3] + ")")

            mDetector?.setHsvColor(mBlobColorHsv)

            Imgproc.resize(mDetector!!.spectrum, mSpectrum, SPECTRUM_SIZE, 0.0, 0.0, Imgproc.INTER_LINEAR_EXACT)

            mIsColorSelected = true

            touchedRegionRgba.release()
            touchedRegionHsv.release()
        }


        return false // don't need subsequent touch events
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        mRgba = inputFrame.rgba()

        if (mIsColorSelected) {
            mDetector?.process(mRgba)
            val contours = mDetector?.contours
            Log.e(TAG, "Contours count: " + contours?.size)
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR)

            val colorLabel = mRgba?.submat(4, 68, 4, 68)
            colorLabel?.setTo(mBlobColorRgba)

            val spectrumLabel = mRgba?.submat(4, 4 + mSpectrum!!.rows(), 70, 70 + mSpectrum!!.cols())
            mSpectrum?.copyTo(spectrumLabel)
        }

        return mRgba
    }

    private fun converScalarHsv2Rgba(hsvColor: Scalar?): Scalar? {
        val pointMatRgba = Mat()
        val pointMatHsv = Mat(1, 1, CvType.CV_8UC3, hsvColor)
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4)

        return Scalar(pointMatRgba.get(0, 0))
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
