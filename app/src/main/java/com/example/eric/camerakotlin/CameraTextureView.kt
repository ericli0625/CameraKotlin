package com.example.eric.camerakotlin

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView

class CameraTextureView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    companion object {
        private var TAG: String = "CameraPreview"
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
        Log.i(TAG, "onSurfaceTextureUpdated...")
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
        Log.i(TAG, "onSurfaceTextureDestroyed()...")
        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
        Log.i(TAG, "onSurfaceTextureAvailable()...")
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
        Log.i(TAG, "onSurfaceTextureSizeChanged()...")
    }

}


