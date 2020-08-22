package com.pirris.hselfiecamera.camera

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.huawei.hms.common.size.Size
import com.huawei.hms.mlsdk.common.LensEngine
import com.pirris.hselfiecamera.overlay.GraphicOverlay
import java.io.IOException

/**
 * Motor del lente que le da vida a la selfieCam
 */
class LensEnginePreview (context: Context, attributeSet: AttributeSet?): ViewGroup(context, attributeSet) {

    private val mContext: Context = context
    private val mSurfaceView: SurfaceView
    private var mStartRequested: Boolean
    private var mSurfaceAvailable: Boolean
    private var mLensEngine: LensEngine? = null
    private var mOverlay: GraphicOverlay? = null

    init {
        mStartRequested = false
        mSurfaceAvailable = false
        mSurfaceView = SurfaceView(context)
        // Nuestra vista va a pedir un callback para agregar el lente
        mSurfaceView.holder.addCallback(SurfaceCallback())
        this.addView(mSurfaceView)
    }


    /**
     * Esta función nos permitirá implementar en nuestro XML la personalización de la cámara
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var previewWidth = 320
        var previewHeight = 240
        //Verificamos si existe el lente
        if (mLensEngine != null) {
            val size: Size? = mLensEngine!!.displayDimension
            if (size != null) {
                previewHeight = size.height
                previewWidth = size.width

            }
        }
        //Verificamos si context existe
        if (mContext.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {

            //Esto se hace con el objetivo de que el layout sea lo más concreto posible en cuanto
            //al tamaño
            val tmp = previewWidth
            previewWidth = previewHeight
            previewHeight = tmp
        }

        val viewWidth = right - left
        val viewHeight = bottom - top
        val childWidth: Int
        val childHeight: Int
        var childXOffSet = 0
        var childYOffSet = 0
        val widthRatio = viewWidth.toFloat() / previewWidth.toFloat()
        val heightRatio = viewHeight.toFloat() / previewHeight.toFloat()

        if (widthRatio > heightRatio) {
            childWidth = viewWidth
            childHeight = (previewHeight.toFloat() * heightRatio).toInt()
            childYOffSet = (childHeight - viewHeight) / 2
        } else {
            childWidth = (previewWidth.toFloat() * heightRatio).toInt()
            childHeight = viewHeight
            childXOffSet = (childWidth - viewWidth) / 2
        }
        for (i in 0 until this.childCount) {
            getChildAt(i).layout(
                -1 * childXOffSet,
                -1 * childYOffSet,
                childWidth - childXOffSet,
                childHeight - childYOffSet
            )
        }
        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e("Error", "No se pudo iniciar la cámara")
        }
    }

    @Throws(IOException::class)
    fun start(lensEngine: LensEngine?, overlay: GraphicOverlay?) {
        mOverlay = overlay
        start(lensEngine)
    }

    //Función para inicializar la cámara, esta función es una sobreescritura de la función start anterior
    @Throws(IOException::class)
    fun start(lensEngine: LensEngine?) {
        if (lensEngine == null) {
            stop()
        }
        mLensEngine = lensEngine
        if (mLensEngine != null) {
            mStartRequested = true
            //Vamos a crear una función que nos va a decir si la cámara está lista
        }
    }

    //Función para detener el lente de la cámara
    fun stop() {
        if (mLensEngine != null) {
            mLensEngine!!.close()
        }
    }

    //Función para liberar la cámara, verifica que la cámara o el motor del lente se libere cuando no sea nulo
    fun release() {
        if (mLensEngine != null) {
            mLensEngine!!.release()
            mLensEngine = null
        }
    }

    //Función para verificar que la cámara se encuentra lista para funcionar
    @Throws(IOException::class)
    fun startIfReady() {
        if (mStartRequested && mSurfaceAvailable) {
            mLensEngine!!.run(mSurfaceView.holder)
            if (overlay != null) {
                val size: Size = mLensEngine!!.displayDimension
                val min: Int = size.width.coerceAtMost(size.height)
                val max: Int = size.width.coerceAtLeast((size.height))
                if (Configuration.ORIENTATION_PORTRAIT == mContext.resources.configuration.orientation) {
                    mOverlay!!.setCameraInfo(min, max, mLensEngine!!.lensType)
                } else {
                    mOverlay!!.setCameraInfo(max, min, mLensEngine!!.lensType)
                }
                mOverlay!!.clear()
            }
            mStartRequested = false
        }
    }

    private inner class SurfaceCallback: SurfaceHolder.Callback{
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            heigth: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            mSurfaceAvailable = false
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            mSurfaceAvailable = true
            try {
                startIfReady()
            }catch (e: IOException){
                Log.e("Error:", "No pudimos iniciar la camara $e")
            }
        }

    }
}


