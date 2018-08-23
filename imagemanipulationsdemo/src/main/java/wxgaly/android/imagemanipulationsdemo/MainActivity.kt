package wxgaly.android.imagemanipulationsdemo

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import wxgaly.android.activity.BaseActivity
import java.util.*


class MainActivity : BaseActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "MainActivity"

    val VIEW_MODE_RGBA = 0
    val VIEW_MODE_HIST = 1
    val VIEW_MODE_CANNY = 2
    val VIEW_MODE_SEPIA = 3
    val VIEW_MODE_SOBEL = 4
    val VIEW_MODE_ZOOM = 5
    val VIEW_MODE_PIXELIZE = 6
    val VIEW_MODE_POSTERIZE = 7

    private var mItemPreviewRGBA: MenuItem? = null
    private var mItemPreviewHist: MenuItem? = null
    private var mItemPreviewCanny: MenuItem? = null
    private var mItemPreviewSepia: MenuItem? = null
    private var mItemPreviewSobel: MenuItem? = null
    private var mItemPreviewZoom: MenuItem? = null
    private var mItemPreviewPixelize: MenuItem? = null
    private var mItemPreviewPosterize: MenuItem? = null


    private var mSize0: Size? = null

    private var mIntermediateMat: Mat? = null
    private var mMat0: Mat? = null
    private var mChannels: Array<MatOfInt>? = null
    private var mHistSize: MatOfInt? = null
    private val mHistSizeNum = 25
    private var mRanges: MatOfFloat? = null
    private var mColorsRGB: Array<Scalar>? = null
    private var mColorsHue: Array<Scalar>? = null
    private var mWhilte: Scalar? = null
    private var mP1: Point? = null
    private var mP2: Point? = null
    private var mBuff: FloatArray? = null
    private var mSepiaKernel: Mat? = null

    var viewMode = VIEW_MODE_RGBA

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    cameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)
//        cameraView.visibility = CameraBridgeViewBase.VISIBLE
//        cameraView.setCvCameraViewListener(this)
    }

//    override fun getContentView(): View {
//
//        val contentParent = mSubDecor.findViewById(android.R.id.content) as ViewGroup
//        contentParent.removeAllViews()
//        LayoutInflater.from(this).inflate(R.layout.activity_main, contentParent)
//    }

    override fun initView() {
        cameraView.visibility = CameraBridgeViewBase.VISIBLE
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
        Log.i(TAG, "called onCreateOptionsMenu")
        mItemPreviewRGBA = menu.add("Preview RGBA")
        mItemPreviewHist = menu.add("Histograms")
        mItemPreviewCanny = menu.add("Canny")
        mItemPreviewSepia = menu.add("Sepia")
        mItemPreviewSobel = menu.add("Sobel")
        mItemPreviewZoom = menu.add("Zoom")
        mItemPreviewPixelize = menu.add("Pixelize")
        mItemPreviewPosterize = menu.add("Posterize")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "called onOptionsItemSelected; selected item: $item")
        when {
            item === mItemPreviewRGBA -> viewMode = VIEW_MODE_RGBA
            item === mItemPreviewHist -> viewMode = VIEW_MODE_HIST
            item === mItemPreviewCanny -> viewMode = VIEW_MODE_CANNY
            item === mItemPreviewSepia -> viewMode = VIEW_MODE_SEPIA
            item === mItemPreviewSobel -> viewMode = VIEW_MODE_SOBEL
            item === mItemPreviewZoom -> viewMode = VIEW_MODE_ZOOM
            item === mItemPreviewPixelize -> viewMode = VIEW_MODE_PIXELIZE
            item === mItemPreviewPosterize -> viewMode = VIEW_MODE_POSTERIZE
        }
        return true
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mIntermediateMat = Mat()
        mSize0 = Size()
        mChannels = arrayOf(MatOfInt(0), MatOfInt(1), MatOfInt(2))
        mBuff = FloatArray(mHistSizeNum)
        mHistSize = MatOfInt(mHistSizeNum)
        mRanges = MatOfFloat(0f, 256f)
        mMat0 = Mat()
        mColorsRGB = arrayOf(Scalar(200.0, 0.0, 0.0, 255.0), Scalar(0.0, 200.0, 0.0, 255.0), Scalar(0.0, 0.0, 200.0, 255.0))
        mColorsHue = arrayOf(Scalar(255.0, 0.0, 0.0, 255.0), Scalar(255.0, 60.0, 0.0, 255.0), Scalar(255.0, 120.0, 0.0, 255.0), Scalar(255.0, 180.0, 0.0, 255.0), Scalar(255.0, 240.0, 0.0, 255.0), Scalar(215.0, 213.0, 0.0, 255.0), Scalar(150.0, 255.0, 0.0, 255.0), Scalar(85.0, 255.0, 0.0, 255.0), Scalar(20.0, 255.0, 0.0, 255.0), Scalar(0.0, 255.0, 30.0, 255.0), Scalar(0.0, 255.0, 85.0, 255.0), Scalar(0.0, 255.0, 150.0, 255.0), Scalar(0.0, 255.0, 215.0, 255.0), Scalar(0.0, 234.0, 255.0, 255.0), Scalar(0.0, 170.0, 255.0, 255.0), Scalar(0.0, 120.0, 255.0, 255.0), Scalar(0.0, 60.0, 255.0, 255.0), Scalar(0.0, 0.0, 255.0, 255.0), Scalar(64.0, 0.0, 255.0, 255.0), Scalar(120.0, 0.0, 255.0, 255.0), Scalar(180.0, 0.0, 255.0, 255.0), Scalar(255.0, 0.0, 255.0, 255.0), Scalar(255.0, 0.0, 215.0, 255.0), Scalar(255.0, 0.0, 85.0, 255.0), Scalar(255.0, 0.0, 0.0, 255.0))
        mWhilte = Scalar.all(255.0)
        mP1 = Point()
        mP2 = Point()

        // Fill sepia kernel
        mSepiaKernel = Mat(4, 4, CvType.CV_32F)
        mSepiaKernel?.apply {
            put(0, 0, /* R */0.189, 0.769, 0.393, 0.0)
            put(1, 0, /* G */0.789, 0.686, 0.349, 0.0)
            put(2, 0, /* B */0.131, 0.534, 0.272, 0.0)
            put(3, 0, /* A */0.000, 0.000, 0.000, 1.0)
        }

    }

    override fun onCameraViewStopped() {
        // Explicitly deallocate Mats
        mIntermediateMat?.release()

        mIntermediateMat = null
    }


    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        val rgba = inputFrame.rgba()
        val sizeRgba = rgba.size()

        val rgbaInnerWindow: Mat

        val rows = sizeRgba.height.toInt()
        val cols = sizeRgba.width.toInt()

        val left = cols / 8
        val top = rows / 8

        val width = cols * 3 / 4
        val height = rows * 3 / 4

        when (viewMode) {
            VIEW_MODE_RGBA -> {
            }

            VIEW_MODE_HIST -> {
                val hist = Mat()
                var thikness = ((sizeRgba.width / (mHistSizeNum + 10) / 5)).toInt()
                if (thikness > 5) thikness = 5
                val offset = (((sizeRgba.width - (5 * mHistSizeNum + 4 * 10) * thikness) / 2)).toInt()
                // RGB
                for (c in 0..2) {
                    Imgproc.calcHist(Arrays.asList(rgba), mChannels!![c], mMat0, hist, mHistSize,
                            mRanges)
                    Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
                    hist.get(0, 0, mBuff)
                    for (h in 0 until mHistSizeNum) {
                        mP2!!.x = (offset + (c * (mHistSizeNum + 10) + h) * thikness).toDouble()
                        mP1!!.x = mP2!!.x
                        mP1!!.y = sizeRgba.height - 1
                        mP2!!.y = mP1!!.y - 2 - mBuff!![h].toInt()
                        Imgproc.line(rgba, mP1, mP2, mColorsRGB!![c], thikness)
                    }
                }
                // Value and Hue
                Imgproc.cvtColor(rgba, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL)
                // Value
                Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels!![2], mMat0, hist,
                        mHistSize, mRanges)
                Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
                hist.get(0, 0, mBuff)
                for (h in 0 until mHistSizeNum) {
                    mP2!!.x = (offset + (3 * (mHistSizeNum + 10) + h) * thikness).toDouble()
                    mP1!!.x = mP2!!.x
                    mP1!!.y = sizeRgba.height - 1
                    mP2!!.y = mP1!!.y - 2 - mBuff!![h].toInt()
                    Imgproc.line(rgba, mP1, mP2, mWhilte, thikness)
                }
                // Hue
                Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels!![0], mMat0, hist,
                        mHistSize, mRanges)
                Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
                hist.get(0, 0, mBuff)
                for (h in 0 until mHistSizeNum) {
                    mP2!!.x = (offset + (4 * (mHistSizeNum + 10) + h) * thikness).toDouble()
                    mP1!!.x = mP2!!.x
                    mP1!!.y = sizeRgba.height - 1
                    mP2!!.y = mP1!!.y - 2 - mBuff!![h].toInt()
                    Imgproc.line(rgba, mP1, mP2, mColorsHue!![h], thikness)
                }
            }

            VIEW_MODE_CANNY -> {
                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
                Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80.0, 90.0)
                Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4)
                rgbaInnerWindow.release()
            }

            VIEW_MODE_SOBEL -> {
                val gray = inputFrame.gray()
                val grayInnerWindow = gray.submat(top, top + height, left, left + width)
                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
                Imgproc.Sobel(grayInnerWindow, mIntermediateMat, CvType.CV_8U, 1, 1)
                Core.convertScaleAbs(mIntermediateMat, mIntermediateMat, 10.0, 0.0)
                Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4)
                grayInnerWindow.release()
                rgbaInnerWindow.release()
            }

            VIEW_MODE_SEPIA -> {
                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
                Core.transform(rgbaInnerWindow, rgbaInnerWindow, mSepiaKernel)
                rgbaInnerWindow.release()
            }

            VIEW_MODE_ZOOM -> {
                val zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10)
                val mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100)
                Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size(), 0.0, 0.0, Imgproc.INTER_LINEAR_EXACT)
                val wsize = mZoomWindow.size()
                Imgproc.rectangle(mZoomWindow, Point(1.0, 1.0), Point(wsize.width - 2, wsize.height
                        - 2), Scalar(255.0, 0.0, 0.0, 255.0), 2)
                zoomCorner.release()
                mZoomWindow.release()
            }

            VIEW_MODE_PIXELIZE -> {
                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
                Imgproc.resize(rgbaInnerWindow, mIntermediateMat, mSize0, 0.2, 0.2, Imgproc.INTER_NEAREST)
                Imgproc.resize(mIntermediateMat, rgbaInnerWindow, rgbaInnerWindow.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)
                rgbaInnerWindow.release()
            }

            VIEW_MODE_POSTERIZE -> {
                /*
            Imgproc.cvtColor(rgbaInnerWindow, mIntermediateMat, Imgproc.COLOR_RGBA2RGB);
            Imgproc.pyrMeanShiftFiltering(mIntermediateMat, mIntermediateMat, 5, 50);
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_RGB2RGBA);
            */
                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
                Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80.0, 90.0)
                rgbaInnerWindow.setTo(Scalar(0.0, 0.0, 0.0, 255.0), mIntermediateMat)
                Core.convertScaleAbs(rgbaInnerWindow, mIntermediateMat, 1.0 / 16, 0.0)
                Core.convertScaleAbs(mIntermediateMat, rgbaInnerWindow, 16.0, 0.0)
                rgbaInnerWindow.release()
            }
        }

        return rgba
    }

}
