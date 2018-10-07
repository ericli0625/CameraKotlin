package com.example.eric.camerakotlin.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.eric.camerakotlin.R
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.widget.Toast

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()

        private var PERMISSION_REQUEST_CAMERA = 0
        private var PERMISSION_REQUEST_SAVE_PHOTO = 2
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var cameraTextureViewLayout: View

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
        cameraTextureViewLayout = view.findViewById<View>(R.id.camera_textureview)

        showCheckPermission()
    }

    private val captureButtonCallback = View.OnClickListener {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Toast.makeText(this.context, "Camera permission is available. Starting preview.", Toast.LENGTH_LONG).show()
            flag3 = true
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this.context, "Camera permission is available. Save Photo.", Toast.LENGTH_LONG).show()
            flag4 = true
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_SAVE_PHOTO)
        }

        if (flag3 && flag4){
            startCamera()
        }
    }

    private fun startCamera() {
        cameraPreview = CameraPreview(this, ImageFormat.JPEG)
    }

}

