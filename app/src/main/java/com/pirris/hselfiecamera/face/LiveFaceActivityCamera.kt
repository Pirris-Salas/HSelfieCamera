package com.pirris.hselfiecamera.face

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLResultTrailer
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.huawei.hms.mlsdk.face.MLMaxSizeFaceTransactor
import com.pirris.hselfiecamera.R
import com.pirris.hselfiecamera.camera.LensEnginePreview
import com.pirris.hselfiecamera.overlay.GraphicOverlay
import com.pirris.hselfiecamera.overlay.LocalFaceGraphic
import java.io.IOException

class LiveFaceActivityCamera : AppCompatActivity(), View.OnClickListener {
    private var analyzer: MLFaceAnalyzer? = null
    private var mLensEngine: LensEngine? = null
    private var mPreview: LensEnginePreview? = null
    private var overlay: GraphicOverlay? = null
    private var lensType = LensEngine.FRONT_LENS

    //Promedio de sonrisas grupal, acorde a la documentación, el porcentaje va del 70% al 90%,
    // asignamos un 80%
    private val smilingRate = 0.8f

    //Con esta variable controlamos si la cámara está en la parte trasera o delantera
    private var isFront = false

    private var detectMode = 0  // Variable que almacenará el valor de detect mode de cada botón

    //Según la documentación de Huawei este es el valor efectivo, el cual oscila entre el 80% y el 99%
    //Acorde a la documentación el 95% es el que posee un rate más exitoso de detección
    private val smillingPossibility = 0.95f

    //Utilizaremos esta variable para saber si es seguro o no tomar la foto. Dependiendo de
    //las probabilidades de detectar una sonrisa, el código decidirá si tomar la foto o no
    private var safeToTakePicture = false

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
        findViewById<View>(R.id.btnFacingSwitch).setOnClickListener(this) //llamamos al botón de switch

        createFaceAnalyzer() //Avalizar el rostro
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
     * MLFaceAnalyzerSrtting es una herramienta de Machine Learning para analizar rostros
     * MinFaceProportion analiza en que posición se encuentra el rostro
     * TracingAllowed permite seguir el rostro a pesar de que se encuentre en movimiento
     */
    private fun createFaceAnalyzer(){
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1f)
            .setTracingAllowed(true)
            .create()

        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)

        if(detectMode == 1003){
            val transactor = MLMaxSizeFaceTransactor.Creator(analyzer, object : MLResultTrailer<MLFace?>() {
                override fun objectCreateCallback(itemId: Int, obj: MLFace?) {
                    overlay!!.clear()
                    //Si no detecta un rostro retorna null
                    if(obj == null){
                        return
                    }
                    val faceGraphic = LocalFaceGraphic(overlay!!, obj, this@LiveFaceActivityCamera)
                    overlay!!.addGraphic(faceGraphic)

                    val emotion = obj.emotions //asignamos el paquete de emociones de Huawei
                    //Si las probabilidades de que sea una sonrisa lo que capta el lente son mayores del
                    //95% es seguro tomar una foto
                    if(emotion.smilingProbability > smillingPossibility){
                        safeToTakePicture = true
                    }
                }

                //En caso de que necesitemos tomar la foto nuevamente
                override fun objectUpdateCallback(var1: MLAnalyzer.Result<MLFace?>?, obj: MLFace?) {
                    overlay!!.clear()
                    //Si no detecta un rostro retorna null
                    if(obj == null){
                        return
                    }

                    val faceGraphic = LocalFaceGraphic(overlay!!, obj, this@LiveFaceActivityCamera)
                    overlay!!.addGraphic(faceGraphic)

                    val emotion = obj.emotions //asignamos el paquete de emociones de Huawei

                    //Si las probabilidades de que sea una sonrisa lo que capta el lente son mayores del
                    //95% es seguro tomar una foto
                    //En este caso como es para volver a utilizar la cámara asignamos la variable
                    //safeToTake picture la estará en true solo después de que la cámara haya sido utilizada
                    if(emotion.smilingProbability > smillingPossibility && safeToTakePicture){
                        safeToTakePicture = true
                    }
                }

                //Callback en caso de que perdamos la conexión con nuestra cámara
                override fun lostCallback(result: MLAnalyzer.Result<MLFace?>?) {
                    overlay!!.clear()
                }

                override fun completeCallback() {
                       overlay!!.clear()

                }

            }).create() //Creamos la variable transactor
            analyzer!!.setTransactor(transactor) //Acá tenemos la validación de transactor
        }
        else{
            //Acá validaremos el botón del código 1002, del botón mostPeople

            analyzer!!.setTransactor(object : MLAnalyzer.MLTransactor<MLFace>{
                override fun destroy() {}

                override fun transactResult(result: MLAnalyzer.Result<MLFace>?) {

                    val faceSparseArray = result!!.analyseList
                    var flag = 0
                    for(i in 0 until faceSparseArray.size()){
                       val emotion = faceSparseArray.valueAt(i).emotions
                        if(emotion.smilingProbability > smillingPossibility){
                            flag++
                        }
                    }
                    if(flag > faceSparseArray.size() * smilingRate && safeToTakePicture ){
                        safeToTakePicture = false
                    }
                }

            })

        }
    }


    /**
     * Esta función, hace el papel de motor del lente.
     */
    private fun createLensEngine(){
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
                if(detectMode == 1003){
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
        createFaceAnalyzer()// Analizar el rostro
        createLensEngine()
        startLensEngine()
    }

    override fun onClick(p0: View?) {
        isFront = !isFront
        if (isFront){
            lensType = LensEngine.FRONT_LENS
        }else{
            lensType = LensEngine.BACK_LENS
        }
        if (mLensEngine != null){
            mLensEngine!!.close()
        }
        startPreview(p0)
    }
}