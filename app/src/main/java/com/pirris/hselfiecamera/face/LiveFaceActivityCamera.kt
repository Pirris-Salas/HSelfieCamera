package com.pirris.hselfiecamera.face

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.pirris.hselfiecamera.R
import com.pirris.hselfiecamera.camera.LensEnginePreview
import com.pirris.hselfiecamera.overlay.GraphicOverlay

class LiveFaceActivityCamera : AppCompatActivity() {
    private var analyzer: MLFaceAnalyzer? = null
    private var mLensEngine: LensEngine? = null
    private var mPreview: LensEnginePreview? = null
    private var overlay: GraphicOverlay? = null
    private var lensType = LensEngine.FRONT_LENS
    private var restartButton: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_face_camera)

        //Verificamos que nuestro estado puede cambiar tanto en portrait como en landscape
        if(savedInstanceState != null){
            lensType = savedInstanceState.getInt("lensType")
        }
        mPreview = findViewById(R.id.preview)
        overlay = findViewById(R.id.faceOverlay)
        restartButton = findViewById(R.id.btnRestart)

        createLensEngine()
    }

    /**
     * Esta función, hace el papel de motor del lente.
     * MLFaceAnalyzerSrtting es una herramienta de Machine Learning para analizar rostros
     * MinFaceProportion analiza en que posición se encuentra el rostro
     * TracingAllowed permite seguir el rostro a pesar de que se encuentre en movimiento
     *
     */
    private fun createLensEngine(){
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1F)
            .setTracingAllowed(true)
            .create()

        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)
        val context: Context = this.applicationContext
        mLensEngine = LensEngine.Creator(context, analyzer).setLensType(lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(20.0f)
            .enableAutomaticFocus(true)
            .create()
    }
}