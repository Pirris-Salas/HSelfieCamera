package com.pirris.hselfiecamera.push

import android.content.Context
import android.os.Looper
import android.util.Log
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import java.lang.Exception
import kotlin.concurrent.thread

//HMS Message Service se encarga de conectar la aplicación con la nube de Huawei
class HuaweiPushService :HmsMessageService(){

    companion object{
        //Constante para loguear y saber que estamos dentro del servicio push
       private const val TAG = "HuaweiPushService"
    }

    /**
     * Esta clase se va a encargar de decirnos si recibimos o no el mensaje
     *
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje Recibido") // Validación para cuando recibimos el mensaje

       //Validación para detectar en caso de que se reciba un mensaje vacío
        remoteMessage?.let {
            Log.d(TAG, "- Data ${it.data}")
        }
    }

    /**
     * Esta función sirve para renovar el token de la aplicación en caso de que expire
     * Por lo general los tokens se renuevan cada 6 meses
     * Esta función nos ayuda a revisar y renovar en caso de que sea necesario
     * Se activa al iniciar la aplicación
     */
    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Log.d(TAG, "Huawei push token: $token")
    }

}

/**
 * Esta clase la utilizaremos en el Main Activity para loguear nuestro token
 */
class GetTokenAction(){
    //Este Handler se encarga de enviarnos el token de forma síncrona
    //Va a esperar unos segundos para enviarnos el push notification
    private val handle: android.os.Handler = android.os.Handler(Looper.getMainLooper())

    //Con esta funcion traemos el token a nuestro main Activity
    fun getToken(context: Context, callback: (String) -> Unit){

        //Hilo de modo síncrono, se abre un hilo para recibir el token
        thread {
            try {
                //Acá guardamos el ID de nuestra app haciendo la consulta a la nube
                val appID = AGConnectServicesConfig.fromContext(context).getString("client/app_id")
                val token = HmsInstanceId.getInstance(context).getToken(appID, "HCM")
                handle.post {callback(token)} //lambda
            }catch (err: Exception){
                Log.e("Error: ", err.toString())
            }
        }

    }

}