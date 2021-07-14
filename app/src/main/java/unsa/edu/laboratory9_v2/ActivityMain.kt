package unsa.edu.laboratory9_v2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.collections.ArrayList


class ActivityMain : AppCompatActivity() {
    private val REQUEST_CAMERA_PERMISSION = 1
    private val TAG = MainActivity::class.java.simpleName
    private var textureView: TextureView? = null
    private var textureViewResult: TextureView? = null
    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var mCameraId: String? = null
    private var cameraDevice: CameraDevice? = null

    private var previewList: ArrayList<Surface>? =null
    private var previewListToResult: ArrayList<Surface>? =null

    private var buttonTakePhoto: Button? =null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        initView()
        initData()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initData() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun initView() {
        textureView = findViewById(R.id.ttv_camera)
        textureViewResult = findViewById(R.id.ttv_camera_result)
        buttonTakePhoto = findViewById(R.id.btn_take_photo)
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onResume() {
        super.onResume()
        buttonTakePhoto?.setOnClickListener{
            takePhoto()
        }
        textureView!!.surfaceTextureListener = object : SurfaceTextureListener {
            @RequiresApi(api = Build.VERSION_CODES.M)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // 1. Configuring the camera whenTextureView is available
                initCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCamera() {
        // 2. Configure the pre-camera, get the size and ID
        getCameraIdAndPreviewSizeByFacing(CameraCharacteristics.LENS_FACING_FRONT)
        // 3. Open the camera
        openCamera()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun openCamera() {
        try {
            // 4. Permission check
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermission()
                return
            }
            // 5. Virtually open the camera
            cameraManager!!.openCamera(mCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera // Opened, save the CAMERADEVICE instance representing the camera
                    val surfaceTexture = textureView!!.surfaceTexture
                    surfaceTexture!!.setDefaultBufferSize(textureView!!.width, textureView!!.height)
                    val surface = Surface(surfaceTexture)
                    previewList = ArrayList()
                    //val previewList: ArrayList<Surface> = ArrayList()
                    previewList!!.add(surface)
                    try {
                        // 6. Pass the TEXTUREVIEW's Surface to CameraDevice
                        cameraDevice!!.createCaptureSession(previewList!!, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    builder.addTarget(surface) // must set up to preview normal
                                    val captureRequest = builder.build()
                                    // 7.cameracaptureSession and CaptureRequest Bind (this is the last step, you have displayed camera preview)
                                    session.setRepeatingRequest(captureRequest, object : CaptureCallback() {

                                    }, null)
                                } catch (e: CameraAccessException) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    releaseCamera()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    releaseCamera()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun releaseCamera() {
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    private fun getCameraIdAndPreviewSizeByFacing(lensFacingFront: Int) {
        try {
            val cameraIdList = cameraManager!!.cameraIdList
            for (cameraId in cameraIdList) {
                val cameraCharacteristics = cameraManager!!.getCameraCharacteristics(cameraId)
                val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (facing != lensFacingFront) {
                    continue
                }
                val streamConfigurationMap =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputSizes = streamConfigurationMap!!.getOutputSizes(
                    SurfaceTexture::class.java
                )
                mCameraId = cameraId
                previewSize =
                    setOptimalPreviewSize(outputSizes, textureView!!.measuredWidth, textureView!!.measuredHeight)
                Log.d(
                    TAG,
                    "Best Preview Size (W-H):" + previewSize!!.width + "+" + previewSize!!.height + ", camera ID: " + mCameraId
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setOptimalPreviewSize(sizes: Array<Size>, previewViewWidth: Int, previewViewHeight: Int): Size? {
        val bigEnoughSizes: MutableList<Size> = ArrayList()
        val notBigEnoughSizes: MutableList<Size> = ArrayList()
        for (size in sizes) {
            if (size.width >= previewViewWidth && size.height >= previewViewHeight) {
                bigEnoughSizes.add(size)
            } else {
                notBigEnoughSizes.add(size)
            }
        }
        return when {
            bigEnoughSizes.size > 0 -> {
                Collections.min(bigEnoughSizes) { o1, o2 ->
                    java.lang.Long.signum(
                        o1!!.width.toLong() * o1.height -
                                o2!!.width.toLong() * o2.height
                    )
                }
            }
            notBigEnoughSizes.size > 0 -> {
                Collections.max(notBigEnoughSizes) { o1, o2 ->
                    java.lang.Long.signum(
                        o1!!.width.toLong() * o1.height -
                                o2!!.width.toLong() * o2.height
                    )
                }
            }
            else -> {
                Log.d(TAG, "No suitable preview size.")
                sizes[0]
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please grant the camera permissions!", Toast.LENGTH_SHORT).show()
            } else {
                openCamera()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    fun takePhoto(){
        val surfaceTexture = textureViewResult!!.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(textureViewResult!!.width, textureViewResult!!.height)
        val surfaceFromResult = Surface(surfaceTexture)

        previewListToResult = ArrayList()
        previewListToResult!!.add(surfaceFromResult)

        try {
            cameraDevice!!.createCaptureSession(previewListToResult!!, object : CameraCaptureSession.StateCallback() {
                @RequiresApi(Build.VERSION_CODES.M)
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        builder.addTarget(surfaceFromResult)
                        val captureRequest = builder.build()
                        session.capture(captureRequest, object : CaptureCallback() {}, null)
                        Thread.sleep(300);

                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    } finally {
                        openCamera()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    /*TO ERASE
    fun test2(){
        val surfaceTexture = textureViewResult!!.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(textureViewResult!!.width, textureViewResult!!.height)
        val surface = Surface(surfaceTexture)

        val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        builder.addTarget(surface)
        val captureRequest = builder.build()
        globalCaptureSession!!.capture(captureRequest, object : CaptureCallback() {}, null)

        Thread.sleep(300);
    }
    */
}