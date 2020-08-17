package com.pirris.hselfiecamera.camera

//Clase que podrá ser utilizada en cualquier parte del código
open class CameraConfiguration{
    var fps = 20.0f
    var previewHeight: Int? = null
    var isAutoFocus = true

    @Synchronized //Sucede en tiempo real
    fun setCameraFacing (facing:  Int){
        require(!(facing != CAMERA_FACING_BACK && facing != CAMERA_FACING_FRONT)) {
            "Invalid Camera: $facing"
        }
        cameraFacing = facing
    }

    companion object{
        val CAMERA_FACING_BACK: Int = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
        val CAMERA_FACING_FRONT: Int = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT

        @get: Synchronized //get funciona al momento de declarar variables
        var cameraFacing = CAMERA_FACING_BACK
        protected set //Esto funciona para indicar que la variable solo debe de ser accedida en
        //tiempo de ejecución, y así evitamos nullpointer exception

    }
}