package com.pirris.hselfiecamera.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.pirris.hselfiecamera.R
import com.pirris.hselfiecamera.auth.AuthActivity
import com.pirris.hselfiecamera.face.LiveFaceActivityCamera
import kotlinx.android.synthetic.main.activity_home.*
import java.lang.RuntimeException
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    companion object {

        private const val PERMISSION_REQUEST = 1 // constante con un valor 1

        /**
         * Con esta funcion podremos saber el estado de los permisos
         * Si fueron concedidos o denegados
         * Función tipo booleana
         */
        private fun isPermissionGranted(context: Context, permission: String?): Boolean {

            //Acá verificamos si el permiso fué concedido, en caso de estarlo retorna true
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission!!
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
            //En caso de que no, retorna false
            return false
        }
    }

    //Acá almacenaremos los permisos, variable tipo array con nullsafety
    private val requiredPermission: Array<String?>
        get() = try {
            val info = this.packageManager     //Acá traemos la información de nuestros permisos
                .getPackageInfo(
                    this.packageName,
                    PackageManager.GET_PERMISSIONS
                ) //Traemos los permisos
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) { //si los permisos no son null y no son vacíos
                ps //retorna los permisos
            } else {
                arrayOfNulls(0) //devuelve un array de null con valor 0
            }
        } catch (e: RuntimeException) { //Tiempo de ejecución
            throw e //Arroja el error
        } catch (e: Exception) { //Este es en caso de que no pueda acceder a los permisos, y por ende retorne null
            arrayOfNulls(0)
        }


    /**
     * Esta función nos dirá si todos los permisos fueron concedidos
     * true: Todos los permisos fueron concedidos
     * false: No todos fueron concedidos
     */
    private fun allPermissionGranted(): Boolean {
        for (permission in requiredPermission) { //recorre todos los permisos del arreglo declarado anteriormente
            if (!isPermissionGranted(
                    this,
                    permission
                )
            ) { //En caso de que los permisos no hayan sido concedidos
                return false
            }
        }
        return true //si ya fueron concedidos retorna un true
    }

    private val runtimePermission: Unit     //Permisos en tiempo de ejecución
        get() {
            //Acá validaremos que ya tenemos los permisos en tiempo de ejecución
            val allPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermission) {
                if (!isPermissionGranted(this, permission)) {
                    allPermissions.add(permission) //Agregamos los permisos
                }
                //Antes debemos de validar que los permisos no sean vacíos
                if (allPermissions.isNotEmpty()) {
                    //Acá solicitamos a la aplicación pedir los permisos
                    ActivityCompat.requestPermissions(
                        this,
                        allPermissions.toTypedArray(),
                        PERMISSION_REQUEST
                    )
                }
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //Validamos el resultado de la petición de los permisos
        if (requestCode != PERMISSION_REQUEST) {
            return // retorna un nulo
        }

        //Si sale del if anterior, quiere decir que efectivamente estamos pidiendo permisos
        //Por lo que creamos la siguiente variable para saber si es necesario o no mostrar un dialogo
        //Este diálogo lo que hará es mostrarnos los permisos en tiempo de ejecución
        var isNeedShowDialog = false

        //Lo metemos en un for ya que son varios permisos
        for (i in permissions.indices) {
            //Validamos si es el tipo de permiso que necesitamos
            //Y valide que el package manager nos haya otorgado los permisos
            if (permissions[i] == android.Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[i]
                != PackageManager.PERMISSION_GRANTED
            ) {
                isNeedShowDialog = true
            }
        }
        //Si nuestro dialogo está en true y no se han brindado los permisos
        //shouldShowRequestPermissionRationale() permite confirmar con el usuario si realmente no
        //desea otorgar los permisos, esto mediante un mensaje en pantalla
        if (isNeedShowDialog && !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            //Mostramos el diálogo
            val dialog: AlertDialog = AlertDialog.Builder(this)
                .setMessage("Esta aplicación requiere acceso a tu carpeta de medios y a tú cámara para poder funcionar")

                //Esto permite ir a las configuraciones de la aplicación en el teléfono,
                //Para así poder asignar los permisos desde ahí manualmente.
                .setPositiveButton("Configuración") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, 200)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { //botón para cancelar la operación y cerrar
                        _, _ ->
                    finish()
                }.create()
            dialog.show() //mostramos el diálogo
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Validamos si cuando la actividad se crea, si ya todos los permisos fueron concedidos
        //En caso de que no, llamamos runtimePermission

        if (!allPermissionGranted()) {
            runtimePermission
        }

        btnLogout.setOnClickListener {

            logoutHuawei()//Función para cerrar sesión en la aplicación
        }


        //Activamos la cámara
        btnMostPeople.setOnClickListener {
            val intent = Intent(this@MainActivity, LiveFaceActivityCamera::class.java)
            intent.putExtra("detect_mode", 1002)
            startActivity(intent)
        }

        //El otro botón para activar la cámara
        btnNearestPerson.setOnClickListener {
            val intent = Intent(this, LiveFaceActivityCamera::class.java)
            intent.putExtra("detect_mode", 1003)
            startActivity(intent)
        }

    }

    override fun onBackPressed() {
        //Llamamos este método para inhabilitar el botón de retroceso del dispositivo
        //De forma que la única forma de salir de la aplicación es cerrándola
        // o dando click al ícono ic_back_arrow
    }

    private fun logoutHuawei() {
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