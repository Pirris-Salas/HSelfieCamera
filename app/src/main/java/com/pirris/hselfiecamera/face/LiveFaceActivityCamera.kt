package com.pirris.hselfiecamera.face

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
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
import kotlinx.android.synthetic.main.activity_live_face_camera.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI

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
                        safeToTakePicture = false
                        mHandler.sendEmptyMessage(TAKE_PHOTO)
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
                        safeToTakePicture = false
                        mHandler.sendEmptyMessage(TAKE_PHOTO)
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
                        mHandler.sendEmptyMessage(TAKE_PHOTO)
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
                safeToTakePicture = true
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
     fun startPreview(view: View?) {
        mPreview!!.release()
        createFaceAnalyzer()// Analizar el rostro
        createLensEngine()
        startLensEngine()
    }

    override fun onClick(v: View?) {
        //isFront = !isFront
        if (isFront){
            lensType = LensEngine.FRONT_LENS
            isFront = !isFront
        }else{
            lensType = LensEngine.BACK_LENS
            isFront = !isFront
        }
        if (mLensEngine != null){
            mLensEngine!!.close()
        }
        startPreview(v)
    }

    //Declaramos 2 variables constantes para los métodos takePhoto y stopPreview
    //Códigos para toma de foto y retoma de foto
    companion object{
        private const val STOP_PREVIEW = 1
        private const val TAKE_PHOTO = 2
    }

    private fun stopPreview(){
        btnRestart!!.setVisibility(View.VISIBLE)
        if (mLensEngine != null){
            mLensEngine!!.release()
            safeToTakePicture = false
        }
        if (analyzer != null){
            try {
                analyzer!!.stop()
            }catch (e: IOException){
                Log.e("Error", "No pudimos detener la cámara")
            }
        }
        }

    private fun takePhoto(){
        mLensEngine!!.photograph(null, LensEngine.PhotographListener{bytes ->
            //handler para la foto
            mHandler.sendEmptyMessage(STOP_PREVIEW)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            //Guardamos el bitmap mediante la funcion saveBitmapToGallery
            saveBitmapToGallery(bitmap)
        } )
    }

    /**
     * Función para guardar el bitmap en la Galería del teléfono
     * recibe un bitmap
     */
    private fun saveBitmapToGallery(bitmap: Bitmap): String{
        //Ruta por defecto de la carpeta de galería de los teléfonos Huawei
        val appDir = File("/storage/emulated/0/DCIM/Camera")

        if(!appDir.exists()){ //Si el directorio no exite
            //Procedemos a crear el directorio, y guardamos el resultado de la creación
            //false o true
            val res: Boolean = appDir.mkdir()
            if (!res){
                Log.e("Error:", "No pudimos crear el directorio")
                return "" //retornamos un vacío
            }
        }
        //Nombre por defecto de la foto al ser guardada
        //EJ: HSelfieCamera 23/08/2020 22:43:25.jpg
        val fileName = "HSelfieCamera " + System.currentTimeMillis() + ".jpg"

        //Procedemos a crear el archivo, mediante la función de Java File
        //Le pasamos la ruta del directorio, nombre del archivo
        val file = File(appDir, fileName)
        try {
            //Creamos un file output stream, y le pasamos file
            val fos = FileOutputStream(file)

            //Ahora tomamos el bitmap, y lo comprimimos a formato .jpg
            //A una calidad del 100%
            //Y pasamos el FileOutputStream(ruta y nombre del archivo)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush() // Liberamos la memoria ram
            fos.close() // Cerramos/Borramos el file output stream para evitar duplicados

            //Procedemos a crear una uri, la cual nos permitirá acceder a la galería del teléfono
            //pasamos la ruta y el nombre del archivo
            val uri: Uri = Uri.fromFile(file)

            //Procedemos a crear un broadcast para indicarle a Android dónde guardaremos la foto
            //Intent es la forma en la que Android se comunica con otras aplicaciones dentro
            //del dispositivo. ACTION_MEDIA_SCANNER_SCAN_FILE se encarga de revisar el formato
            //de la imagen y si todo está bien lo guarda en la galería
            this.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))


        }catch (e: IOException){
            e.printStackTrace()
        }
        return file.absolutePath //retornamos el archivo con su ruta definitiva
    }

    private val mHandler: Handler = object :Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                STOP_PREVIEW -> stopPreview()
                TAKE_PHOTO -> takePhoto()
                else -> {

                }
            }
        }
    }

}
