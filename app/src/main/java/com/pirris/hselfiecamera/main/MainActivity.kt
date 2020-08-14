package com.pirris.hselfiecamera.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.pirris.hselfiecamera.R
import com.pirris.hselfiecamera.auth.AuthActivity
import kotlinx.android.synthetic.main.activity_home.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnLogout.setOnClickListener {

            logoutHuawei()//Función para cerrar sesión en la aplicación
        }
    }

    override fun onBackPressed() {
        //Llamamos este método para inhabilitar el botón de retroceso del dispositivo
        //De forma que la única forma de salir de la aplicación es cerrándola
        // o dando click al ícono ic_back_arrow
    }

    private fun logoutHuawei(){
        //Traemos los parámetros
        val mAuthParams = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .createParams() //Pasamos los parámetros a objeto

        /**
         * Variable para acceder al servicio de autenticación
         * recibe como parámetros el context del activity y los parámetros
         */
        val mAuthManager = HuaweiIdAuthManager.getService(this, mAuthParams)

        //Declaramos una variable para extraer el servicio de logout del IdAuthManager
        val logoutTask = mAuthManager.signOut()

        /**
         * Validamos si el logout es exitoso o no
         */
        logoutTask.addOnSuccessListener {
            //En caso de ser exitoso, abrimos el layout activity_auth.xml
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }
        logoutTask.addOnFailureListener {
            //En caso de ser fallido mostramos un mensaje
            Toast.makeText(this, "Logout fallido ...", Toast.LENGTH_LONG).show()
        }
    }
}