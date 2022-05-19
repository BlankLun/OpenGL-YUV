package com.lkl.opengl

import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(), Camera.PreviewCallback {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var mCamera: Camera? = null
    // 设定默认的预览宽高
    private var mPreviewWidth = 1920
    private var mPreviewHeight = 1080

    private var mBuffer: ByteArray? = null

    private lateinit var mSurfaceHolder: SurfaceHolder

    private var mDegrees = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                mSurfaceHolder = holder
                initCamera()
            }
        })

        rotateBtn.setOnClickListener {
            mDegrees += 90
            mDegrees %= 360
            openGlSurface?.setDisplayOrientation(mDegrees)
        }
    }

    private fun initCamera() {
        try {
            mCamera = openCamera()

            mCamera?.apply {
                var params: Camera.Parameters = parameters
                val sizes = params.supportedPreviewSizes
                if (sizes != null) {
                    /* Select the size that fits surface considering maximum size allowed */
                    calculateCameraFrameSize(sizes, mPreviewWidth, mPreviewHeight)
                }
                params.previewFormat = ImageFormat.NV21
                params.setPreviewSize(mPreviewWidth, mPreviewHeight)
                Log.d(TAG, "Set preview size to $mPreviewWidth x $mPreviewHeight")

                parameters = params

                openGlSurface.setYuvDataSize(mPreviewWidth, mPreviewHeight)

                var size = mPreviewWidth * mPreviewHeight
                size = size * ImageFormat.getBitsPerPixel(params.previewFormat) / 8
                mBuffer = ByteArray(size)

                addCallbackBuffer(mBuffer)
                setPreviewCallbackWithBuffer(this@MainActivity)

                setPreviewDisplay(mSurfaceHolder)

                startPreview()//开始预览
            }
        } catch (e: Exception) {
            Log.w(TAG, e.message)
        }
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera?) {

//        mPreUtil.feedData(data)
        openGlSurface.feedData(data, 2)
        camera?.addCallbackBuffer(mBuffer)
    }

    /**
     * 打开Camera
     */
    private fun openCamera(): Camera? {
        Log.d(TAG, "Trying to open camera with old open()")
        var camera: Camera? = null
        try {
            camera = Camera.open(1)
        } catch (e: Exception) {
            Log.w(TAG, "Camera is not available (in use or does not exist): ${e.message}")
        }

        if (camera == null) {
            var connected = false
            for (camIdx in 0 until Camera.getNumberOfCameras()) {
                Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")")
                try {
                    camera = Camera.open(camIdx)
                    connected = true
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Camera #$camIdx failed to open: ${e.message}")
                }

                if (connected) break
            }
        }

        return camera
    }

    /**
     * 释放Camera资源
     */
    private fun releaseCamera() {
        mCamera?.apply {
            stopPreview()
            setPreviewCallback(null)
            release()
        }
        mCamera = null
    }

    private fun calculateCameraFrameSize(supportedSizes: List<*>, maxAllowedWidth: Int, maxAllowedHeight: Int) {
        var calcWidth = 0
        var calcHeight = 0

        for (size in supportedSizes) {
            val cameraSize = size as Camera.Size
            val width = cameraSize.width
            val height = cameraSize.height

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    // 找到临近的像素大小
                    calcWidth = width
                    calcHeight = height
                }
            }
        }

        mPreviewWidth = calcWidth
        mPreviewHeight = calcHeight
    }

    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }
}
