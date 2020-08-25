package com.pirris.hselfiecamera.auth

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.pirris.hselfiecamera.R
import com.pirris.hselfiecamera.main.MainActivity
import com.pirris.hselfiecamera.push.GetTokenAction
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)


        //Establecemos un listener, de forma que cuando el botón sea presionado dé inicio la autenticación
        btnLogin.setOnClickListener {
            loginHuaweiIdAuth()
        }

        //Llamamos la funcion getToken para inicializar la solicitud del token a la nube de Huawei
        GetTokenAction().getToken(this){
            Log.d("PushToken: ", it)
        }

    }

    private fun loginHuaweiIdAuth(){
        val mAuthParams = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM) //capturamos los parámetros
            .setEmail() //correo del usuario
            .setAccessToken()
            .setProfile() //capturamos todas las configuraciones del Huawei ID, nickname, foto de perfil, avatar ...
            .setIdToken() //Se utilizará mas adelante para el push notification
            .setUid() //Identificador único de cada usuario de Huawei
            .setId()

            .createParams()//Se crean todos los parámetros anteriores en un objeto, para luego ser accedidos

        /**
         * Variable para acceder al servicio de autenticación
         * recibe como parámetros el context del activity y los parámetros
         */
        val mAuthManager = HuaweiIdAuthManager.getService(this, mAuthParams)


        /**
         * Llamamos el startActivityForResult y pasamos el servicio en forma de intent y un requestCode
         * requestCode = identificador del servicio de login
         */
        startActivityForResult(mAuthManager.signInIntent, 800)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 800){

            /**
             * Este if, cubre si al momento de acceder, el usuario se queda sin internet o si presiona
             * alguna tecla que haga cerrar o detener el activity
             */
            if (resultCode == Activity.RESULT_CANCELED){

                Toast.makeText(this, "Autenticación Cancelada ...", Toast.LENGTH_LONG).show()
            }else if(resultCode == Activity.RESULT_OK){

                //verificamos que el servicio esté funcionando correctamente
                val authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data)

                //Si el resultado es exitoso, procederemos a autenticar
                if (authHuaweiIdTask.isSuccessful){
                   // Toast.makeText(this, "Autenticación exitosa!!", Toast.LENGTH_LONG).show()

                   //Abrimos el layout activity_home.xml
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                //En caso de que no sea exitoso por que la cuenta ya no está activa, incompleta o
                //por una falla de conexión
                else{
                    Toast.makeText(this, "Autenticación Fallida ...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}