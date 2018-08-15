package wxgaly.android.ndkdemo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


class MainActivity : AppCompatActivity(), View.OnClickListener {


    private val TAG = "MainActivity"
    private var flag = true
    private var sourceBitmap: Bitmap? = null
    private var grayBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        initView()

        initData()

    }

    fun initData() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, object :
                BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS -> {
                        Log.d(TAG, "加载成功")
                        initBitmap()
                    }
                    else -> Log.d(TAG, "加载失败")
                }
            }
        })
    }

    private fun initBitmap() {
        val rgbMat = Mat()
        val grayMat = Mat()
        sourceBitmap = BitmapFactory.decodeResource(resources, R.drawable.pic)
        sourceBitmap?.apply {
            grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            Utils.bitmapToMat(sourceBitmap, rgbMat) //convert original bitmap to Mat, R G B.
            Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY) //rgbMat to gray grayMat
            Utils.matToBitmap(grayMat, grayBitmap) //convert mat to bitmap
            Log.i(TAG, "initBitmap sucess...")
        }
    }

    fun initView() {
        tv.text = stringFromJNI()

        btn.setOnClickListener(this)

    }

    override fun onClick(v: View?) {

        v?.apply {
            when (id) {
                R.id.btn -> grayScale()
                else -> Log.d(TAG, "nothing to click")
            }
        }

    }

    private fun grayScale() {
        if (flag) {
            iv.setImageBitmap(grayBitmap!!)
            btn.text = "查看原图"
        } else {
            iv.setImageBitmap(sourceBitmap!!)
            btn.text = "灰度化"
        }
        flag = !flag
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
