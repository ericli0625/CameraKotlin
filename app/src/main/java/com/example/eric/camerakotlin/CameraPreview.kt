package com.example.eric.camerakotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.checkSelfPermission
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraPreview: TextureView, TextureView.SurfaceTextureListener {

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
        Log.i(TAG, "onSurfaceTextureUpdated...")
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
        Log.i(TAG, "onSurfaceTextureDestroyed()...")
        stopBackgroundThread()
        closeCamera()
        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
        Log.i(TAG, "onSurfaceTextureAvailable()...")
        startBackgroundThread()
        openCamera(width, height)
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
        Log.i(TAG, "onSurfaceTextureSizeChanged()...")
    }

    companion object {
        private var TAG: String = "CameraPreview"
    }

    private var mFragment: FragmentActivity? = null

    private var mFormat: Int? = null

    private var mCameraDevice: CameraDevice? = null

    private lateinit var mCameraId: String

    private lateinit var mCameraCharacteristics: CameraCharacteristics

    private lateinit var mMapScaleStreamConfig: StreamConfigurationMap

    private lateinit var mPreviewSize: Size

    private lateinit var mPreviewRequestBuilder: CaptureRequest.Builder

    private var mImageReader: ImageReader? = null

    private var mCaptureBuilder: CaptureRequest.Builder? = null

    private lateinit var mCaptureResult: CaptureResult

    private var mBackgroundThread: HandlerThread? = null

    private var mBackgroundHandler: Handler? = null

    private var mCaptureSession: CameraCaptureSession? = null


    constructor(context: FragmentActivity?, format: Int) : super(context) {
        surfaceTextureListener = this
        mFragment = context
        mFormat = format
    }

    private fun openCamera(width: Int, height: Int) {
        val cameraManager = mFragment?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facingID = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                if (facingID != null && facingID == CameraCharacteristics.LENS_FACING_BACK && map != null) {
                    this.mCameraId = cameraId
                    this.mCameraCharacteristics = cameraCharacteristics
                    this.mMapScaleStreamConfig = map
                    break
                }
            }
        } catch (e: CameraAccessException) {
            Log.d(TAG, e.printStackTrace().toString())
        }

        setUpCameraOutputs(width, height)
        if (checkSelfPermission(mFragment?.applicationContext!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            cameraManager.openCamera(mCameraId, stateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            Log.d(TAG, e.printStackTrace().toString())
        }

    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            this@CameraPreview.mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            this@CameraPreview.mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraDevice.close()
            this@CameraPreview.mCameraDevice = null
        }

    }

    private fun createCameraPreviewSession() {

        // 1.
        // get SurfaceTexture
        val surfaceTexture = surfaceTexture
        // We configure the size of default buffer to be the size of camera preview we want.
        surfaceTexture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
        // This is the output Surface we need to start preview.
        val mSurface = Surface(surfaceTexture)

        // 2.
        // We set up a CaptureRequest.Builder with the output Surface.
        try {
            mPreviewRequestBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)!!
            mPreviewRequestBuilder.addTarget(mSurface)
        } catch (e: CameraAccessException) {
            Log.d(TAG, e.printStackTrace().toString())
        }

        // 3.
        // Here, we create a CameraCaptureSession for camera preview.
        try {
            mCameraDevice?.createCaptureSession(Arrays.asList(mSurface, mImageReader?.surface), mSessionStateCallback, mBackgroundHandler);
        } catch (e: CameraAccessException) {
            Log.d(TAG, e.printStackTrace().toString())
        }

    }

    private val mSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            try {
                updatePreview(session)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

        }

        override fun onConfigureFailed(session: CameraCaptureSession) {

        }
    }

    private fun updatePreview(session: CameraCaptureSession) {
        // When the session is ready, we start displaying the preview.
        mCaptureSession = session
        try {
            // Auto focus should be continuous for camera preview.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

            // 4.
            // Finally, we start displaying the camera preview.
            mCaptureSession?.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            Log.d(TAG, e.printStackTrace().toString())
        }
    }

    private var mCaptureCallback = object: CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            Log.i(TAG, "onCaptureCompleted()...")
            mCaptureResult = result
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            Log.i(TAG, "onCaptureFailed()...")
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground").also { it.start() }
        mBackgroundHandler = Handler(mBackgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun closeCamera() {

        if (null != mCaptureSession) {
            mCaptureSession?.close()
            mCaptureSession = null
        }
        if (null != mCameraDevice) {
            mCameraDevice?.close()
            mCameraDevice = null
        }
        if (null != mImageReader) {
            mImageReader?.close()
            mImageReader = null
        }

    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        setUpPreviewOutputs(width, height)
        setUpPhotoOutputs()
    }

    private fun setUpPhotoOutputs() {
        var picFormat = ImageFormat.UNKNOWN

        when (mFormat) {
            ImageFormat.JPEG -> {
                for (size: Size in mMapScaleStreamConfig.getOutputSizes(ImageFormat.JPEG)) {
                    Log.i(TAG, "Size = $size")
                }
                picFormat = ImageFormat.JPEG
            }
            ImageFormat.RAW_SENSOR -> {
                if (mCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                                .contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                    for (size: Size in mMapScaleStreamConfig.getOutputSizes(ImageFormat.RAW_SENSOR)) {
                        Log.i(TAG, "Size = $size")
                    }
                    picFormat = ImageFormat.RAW_SENSOR;
                } else {
                    Log.i(TAG, "Not support RAW")
                    Toast.makeText(mFragment?.applicationContext, "Not support RAW, so use the JPEG Format.", Toast.LENGTH_LONG).show()
                    picFormat = ImageFormat.JPEG
                }
            }
        }

        // create the photo size
        val outputSize = mMapScaleStreamConfig.getOutputSizes(picFormat)

        // For still image captures, we use the largest available size.
        mImageReader = ImageReader.newInstance(outputSize[0].width, outputSize[0].height, picFormat, /*maxImages*/2)

        mImageReader?.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)
    }

    private fun setUpPreviewOutputs(width: Int, height: Int) {
        val windowManager = mFragment?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val displayRotation = windowManager.defaultDisplay.rotation
        val sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

        var swappedDimensions = false

        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> if (sensorOrientation == 90 || sensorOrientation == 270) swappedDimensions = true
            Surface.ROTATION_90, Surface.ROTATION_270 -> if (sensorOrientation == 0 || sensorOrientation == 180) swappedDimensions = true
            else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
        }

        val rotatedPreviewWidth = if (swappedDimensions) height else width
        val rotatedPreviewHeight = if (swappedDimensions) width else height

        mPreviewSize = Size(rotatedPreviewWidth, rotatedPreviewHeight)
    }

    private var mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader?.acquireLatestImage() as Image
        val picPath = getOutputMediaFile()
        val mImageSaver = ImageSaver(image, picPath, mCaptureResult, mCameraCharacteristics)
        mImageSaver.setContext(mFragment?.applicationContext)
        mBackgroundHandler?.post(mImageSaver)
    }

    fun takePicture() {

        try {
            // We set up a CaptureRequest.Builder to capture the pic.
            mCaptureBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            mCaptureBuilder?.addTarget(mImageReader?.surface)
            mCaptureSession?.capture(mCaptureBuilder?.build(), mCaptureCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    private fun getOutputMediaFile(): File {
        val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        when (mFormat) {
            ImageFormat.JPEG -> {
                return File(path.path + File.separator + "IMG_" + timeStamp + ".jpg")
            }
            ImageFormat.RAW_SENSOR -> {
                return File(path.path + File.separator + "RAW_" + timeStamp + ".dng")
            }
            else -> {
                Log.e(TAG, "mFormat is invalid: $mFormat")
                return File("")
            }
        }

    }

    class ImageSaver() : Runnable {

        private lateinit var mImage: Image
        private lateinit var mPicPath: File

        private var mContext: Context? = null

        /**
         * The CaptureResult for this image capture.
         */
        private lateinit var mCaptureResult: CaptureResult

        /**
         * The CameraCharacteristics for this camera device.
         */
        private lateinit var mCharacteristics: CameraCharacteristics

        constructor(image: Image,picPath: File, result: CaptureResult, characteristics: CameraCharacteristics) : this() {
            this.mImage = image
            this.mPicPath = picPath
            mCaptureResult = result
            mCharacteristics = characteristics
        }

        override fun run() {
            val format = mImage.format
            var success = false

            when (format) {
                ImageFormat.JPEG -> {
                    val buffer = mImage.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    var output: FileOutputStream? = null

                    var pictureTaken: Bitmap = BitmapFactory.decodeByteArray (bytes, 0, bytes.size)
                    val matrix: Matrix = Matrix()
                    matrix.preRotate(90F)
                    pictureTaken = Bitmap.createBitmap(pictureTaken, 0, 0, pictureTaken.width, pictureTaken.height, matrix, true);

                    try {
                        output = FileOutputStream (mPicPath.path)
                        pictureTaken.compress(Bitmap.CompressFormat.JPEG, 50, output)
                        pictureTaken.recycle()
                        output.write(bytes)
                        output.close()
                        success = true
                    } catch (e: IOException) {
                        Log.d(TAG, e.printStackTrace().toString())
                    } finally {
                        mImage.close()
                        if (null != output) {
                            try {
                                output.close()
                            } catch (e: IOException) {
                                Log.d(TAG, e.printStackTrace().toString())
                            }
                        }
                    }
                }
                ImageFormat.RAW_SENSOR -> {
                    val dngCreator = DngCreator(mCharacteristics, mCaptureResult)
                    var output: FileOutputStream? = null
                    try {
                        output = FileOutputStream (mPicPath.path)
                        dngCreator.writeImage(output, mImage)
                        success = true
                    } catch (e: IOException) {
                        Log.d(TAG, e.printStackTrace().toString())
                    } finally {
                        mImage.close()
                        closeOutput(output)
                    }
                }
                else -> {
                    Log.e(TAG, "Cannot save image, unexpected image format:$format")
                }

            }

            if(success){
                galleryAddPic(mPicPath)
            }

        }

        private fun closeOutput(outputStream: OutputStream?) {
            if (null != outputStream) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        private fun galleryAddPic(photoPath: File) {
            val contentUri = Uri.fromFile(photoPath)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
            mContext?.applicationContext?.sendBroadcast(mediaScanIntent)
        }

        fun setContext(applicationContext: Context?) {
            mContext = applicationContext
        }

    }


}