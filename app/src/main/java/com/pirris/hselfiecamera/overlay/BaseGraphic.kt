package com.pirris.hselfiecamera.overlay

import android.graphics.Canvas
import com.huawei.hms.mlsdk.common.LensEngine

abstract class BaseGraphic(private val graphicOverlay: GraphicOverlay) {

    abstract fun draw(canvas: Canvas?) //función de dibujo que recibe un valor tipo canvas de android

    /**
     *Función para escalar en X
     */
    fun scaleX(x: Float): Float{
        return x * graphicOverlay.widthScaleValue
    }

    /**
     * Función para escalar en Y
     */
    fun scaleY(y: Float): Float{
        return y * graphicOverlay.heightScaleValue
    }

    /**
     * Esta función es para realizar la translación en X, con el fin de centrar el rostro del usuario
     * Posee una validación para que solo se realize cuando se esté utilizando la cámara frontal
     * Acá utilizamos el primer componente de Machine Learning Kit
     * LensEngine
     */
    fun translateX(x: Float): Float{
        return if (graphicOverlay.cameraFacing == LensEngine.FRONT_LENS){
            graphicOverlay.width - scaleX(x)
        }else{
            scaleX(x)
        }
    }

    //translamiento en Y
    fun translateY(y: Float): Float{
        return scaleY(y)
    }
}