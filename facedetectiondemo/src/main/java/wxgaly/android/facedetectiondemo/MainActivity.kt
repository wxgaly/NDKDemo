package wxgaly.android.facedetectiondemo

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import wxgaly.android.colorblobdetectordemo.CAMERA

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requireCameraPermission()
    }

    private fun initView() {

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
