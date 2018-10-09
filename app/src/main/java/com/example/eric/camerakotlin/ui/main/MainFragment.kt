package com.example.eric.camerakotlin.ui.main

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.PermissionChecker.checkSelfPermission
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.example.eric.camerakotlin.CameraPreview
import com.example.eric.camerakotlin.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()

        private var PERMISSION_REQUEST_CAMERA = 0
        private var PERMISSION_REQUEST_SAVE_PHOTO = 2

        private var TAG: String = "CameraPreview"
    }

    private lateinit var viewModel: MainViewModel
    private var cameraTextureViewLayout: FrameLayout? = null

    private var cameraPreview: CameraPreview?= null

    private var flag1 = false
    private var flag2 = false
    private var flag3 = false
    private var flag4 = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.capture_button).setOnClickListener(captureButtonCallback)
        cameraTextureViewLayout = view.findViewById<View>(R.id.camera_textureview) as FrameLayout

        showCheckPermission()
    }

    private val captureButtonCallback = object: View.OnClickListener {
        override fun onClick(p0: View?) {
            cameraPreview?.takePicture()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this.context, "Camera permission was granted. Starting preview.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this.context, "Camera permission request was denied.", Toast.LENGTH_LONG).show()
                flag1 = true
            }
        } else if (requestCode == PERMISSION_REQUEST_SAVE_PHOTO) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this.context, "Camera permission was granted. Save Photo.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this.context, "Camera permission request was denied.", Toast.LENGTH_LONG).show()
                flag2 = true
            }
        }

        if (flag1 && flag2) {
            startCamera()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun showCheckPermission() {
        // Check if the Camera permission has been granted
        if (checkSelfPermission(activity?.applicationContext!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Camera permission is available. Starting preview.", Toast.LENGTH_LONG).show()
            flag3 = true
        } else {
            // Permission is missing and must be requested.
            requestCameraPermission()
        }

        // Check if the Camera permission has been granted
        if (checkSelfPermission(activity?.applicationContext!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, start camera preview
            Toast.makeText(context, "Camera permission is available. Save Photo.", Toast.LENGTH_LONG).show()
            flag4 = true
        } else {
            // Permission is missing and must be requested.
            requestSavePhotoPermission()
        }

        if (flag3 && flag4){
            startCamera()
        }
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Toast.makeText(context, "Camera permission is available. Starting preview.", Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
        } else {
            Toast.makeText(context, "Permission is not available. Requesting camera permission.", Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
        }
    }

    private fun requestSavePhotoPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(context, "Camera access is required to save the photo.", Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_SAVE_PHOTO)
        } else {
            Toast.makeText(context, "Permission is not available. Requesting camera permission.", Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_SAVE_PHOTO)
        }
    }

    private fun startCamera() {
        cameraPreview = CameraPreview(activity, ImageFormat.JPEG)
        cameraTextureViewLayout?.addView(cameraPreview)
    }

}

