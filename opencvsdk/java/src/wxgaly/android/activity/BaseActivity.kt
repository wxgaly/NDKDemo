package wxgaly.android.activity

import android.Manifest
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.opencv.R
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

/**
 *  wxgaly.android.activity.
 *
 * @author Created by WXG on 2018/8/23 1:07.
 * @version V1.0
 */
abstract class BaseActivity: AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val TAG = "BaseActivity"

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        requireCameraPermission()
    }

    abstract fun initView()

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