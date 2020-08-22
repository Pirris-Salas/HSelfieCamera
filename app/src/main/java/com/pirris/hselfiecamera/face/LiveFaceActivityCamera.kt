package com.pirris.hselfiecamera.face

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.pirris.hselfiecamera.R
import com.pirris.hselfiecamera.camera.LensEnginePreview
import com.pirris.hselfiecamera.overlay.GraphicOverlay
import java.io.IOException

class LiveFaceActivityCamera : AppCompatActivity() {
    private var analyzer: MLFaceAnalyzer? = null
    private var mLensEngine: LensEngine? = null
    private var mPreview: LensEnginePreview? = null
    private var overlay: GraphicOverlay? = null
    private var lensType = LensEngine.FRONT_LENS
    private var detectMode = 0  // Variable que almacenará el valor de detect mode de cada botón
    private var restartButton: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_face_camera)

        //Verificamos que nuestro estado puede cambiar tanto en portrait como en landscape
        if(savedInstanceState != null){
            lensType = savedInstanceState.getInt("lensType")
        }
        mPreview = findViewById(R.id.preview)
        val intent = this.intent
        try {
            detectMode = intent.getIntExtra("detect_mode", 1)
        }catch (e: RuntimeException){
            Log.e("Error:", "No pude traer el código de detección")
        }
        overlay = findViewById(R.id.faceOverlay)
        restartButton = findViewById(R.id.btnRestart)

        createLensEngine()
    }

    override fun onResume() {
        super.onResume()
        startLensEngine()
    }

    override fun onPause() {
        super.onPause()
        mPreview!!.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mLensEngine != null){
            mLensEngine!!.release()
        }
    }

    /**
     * Esta función manejará e estado en el que viene la pantalla, en portarit o landscape
     * A su vez manejará la forma en que viene la cámara, si es frontal o trasera
     * Todo esto al dar click a alguno de los botones
     *
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("lensType", lensType)
        super.onSaveInstanceState(outState)
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

    /**
     * Esta función va a reinicializar nuevamente el motor Lens Engine en caso de que queramos
     * tomar nuevamente la foto
     */
    private fun startLensEngine(){
        restartButton!!.visibility = View.GONE
        if (mLensEngine != null){
            try { //Al traer la cámara debemos de manejar todas las excepciones posibles
                if(detectMode == 1003 || detectMode == 1002){
                    mPreview!!.start(mLensEngine, overlay)
                }else{
                    mPreview!!.start(mLensEngine)
                }
            }catch (e: IOException){
                mLensEngine!!.release()
                mLensEngine = null
            }
        }
    }

    /**
     * Hereda de view
     * Funciona para crear e iniciar de nuevo el motor del lente
     */
    private fun startPreview(view: View?){
        mPreview!!.release()
        createLensEngine()
        startLensEngine()
    }
}