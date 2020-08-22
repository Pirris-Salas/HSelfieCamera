package com.pirris.hselfiecamera.overlay

import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.pirris.hselfiecamera.camera.CameraConfiguration

//Esta clase hereda de la clase view
class GraphicOverlay (context: Context, attributeSet: AttributeSet?): View(context, attributeSet){
    private val lock = Any() // Con esta variable bloqueamos la cámara una vez la foto es tomada
    private var previewWidth = 0
    private var previewHeight = 0
    var widthScaleValue = 1.0f
        private set
    var heightScaleValue = 1.0f
        private set
    var cameraFacing = CameraConfiguration.CAMERA_FACING_FRONT //cuando camera facing esté en front va a tomar la selfie
        private set

    private val graphics: MutableList<BaseGraphic> = ArrayList()

    fun addGraphic(graphic: BaseGraphic){
        synchronized(lock) {graphics.add(graphic)}
    }

    fun clear(){
        synchronized(lock) {graphics.clear()} //Permitimos que la cámara pueda volver a utilizarse
        this.postInvalidate()
    }

    fun setCameraInfo(width: Int, height: Int, facing: Int){
        synchronized(lock){
            previewHeight = width
            previewHeight= height
            cameraFacing = facing
        }
        this.invalidate() //Esto es para cerrar la cámara cuando no se utilize y así que no quede en
        // un loop infinito
    }

    /**
     * Función encargada de crear un cuadro para rodear el rostro del usuario
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        synchronized(lock){
            //Validación para entender si la cámara está captando un rostro
            if (previewWidth != 0 && previewHeight != 0){
                widthScaleValue =
                    width.toFloat() / previewWidth.toFloat()
                heightScaleValue =
                    height.toFloat() / previewHeight.toFloat()
            }
            for (graphic in graphics){ //For para recorrer y dibujar el cuadro que bordeará el rostro
                graphic.draw(canvas)
            }
        }
    }
}