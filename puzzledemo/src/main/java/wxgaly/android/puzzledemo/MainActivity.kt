package wxgaly.android.puzzledemo

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener, View
.OnTouchListener, EasyPermissions.PermissionCallbacks {

    private val TAG = "MainActivity"

    private var mPuzzle15: Puzzle15Processor? = null
    private var mItemHideNumbers: MenuItem? = null
    private var mItemStartNewGame: MenuItem? = null


    private var mGameWidth: Int = 0
    private var mGameHeight: Int = 0

    private val mLoaderCallback = object : BaseLoaderCallback(this) {

        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

                    /* Now enable camera view to start receiving frames */
                    cameraView.setOnTouchListener(this@MainActivity)
                    cameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requireCameraPermission()
    }

    private fun initView() {
        cameraView.setCvCameraViewListener(this)
        mPuzzle15 = Puzzle15Processor()
        mPuzzle15?.prepareNewGame()
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
        Log.i(TAG, "called onCreateOptionsMenu")
        mItemHideNumbers = menu.add("Show/hide tile numbers")
        mItemStartNewGame = menu.add("Start new game")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "Menu Item selected $item")
        if (item === mItemStartNewGame) {
            /* We need to start new game */
            mPuzzle15?.prepareNewGame()
        } else if (item === mItemHideNumbers) {
            /* We need to enable or disable drawing of the tile numbers */
            mPuzzle15?.toggleTileNumbers()
        }
        return true
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGameWidth = width
        mGameHeight = height
        mPuzzle15?.prepareGameSize(width, height)
    }

    override fun onCameraViewStopped() {}

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        var xpos: Int = (view.width - mGameWidth) / 2
        var ypos: Int = (view.height - mGameHeight) / 2

        xpos = event.x.toInt() - xpos

        ypos = event.y.toInt() - ypos

        if (xpos in 0..mGameWidth && ypos >= 0 && ypos <= mGameHeight) {
            /* click is inside the picture. Deliver this event to processor */
            mPuzzle15?.deliverTouchEvent(xpos, ypos)
        }

        return false
    }

    override fun onCameraFrame(inputFrame: Mat): Mat? {
        return mPuzzle15?.puzzleFrame(inputFrame)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
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
