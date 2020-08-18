package com.pirris.hselfiecamera.utils

import android.content.Context

/**
 * En este objeto declararemos una función para convertir los density-points a pixels
 */
object CommonUtils {
    fun dp2px(context: Context, dipValue: Float): Float{
        return dipValue * context.resources.displayMetrics.density + 0.5f
    }
}