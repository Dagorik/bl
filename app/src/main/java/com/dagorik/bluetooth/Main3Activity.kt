package com.dagorik.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import com.dagorik.bluetooth.bluedroid.*
import java.nio.charset.Charset

class Main3Activity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1234

    private var bt: BlueDago? = null
    private val textoRecebido = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        bt = BlueDago(this, ConnectionDevice.OTHER, ConnectionSecure.SECURE)

        if (!bt!!.isAvailable()) {
            finish()
            return
        }


        bt!!.addDiscoveryListener(object : BlueDago.DiscoveryListener{
            override fun onDiscoveryStarted() {
                Toast.makeText(this@Main3Activity, "Busqueda iniciada", Toast.LENGTH_SHORT).show()
            }

            override fun onDiscoveryFinished() {
                Toast.makeText(this@Main3Activity, "Busqueda finalizada", Toast.LENGTH_SHORT).show()
            }

            override fun onNoDevicesFound() {
                Toast.makeText(this@Main3Activity, "Ningún dispositivo encontrado", Toast.LENGTH_SHORT).show()
            }


            override fun onDeviceFound(device: Device) {

                Toast.makeText(this@Main3Activity, "Encontrado: " + device.getName(), Toast.LENGTH_SHORT).show()
            }

            override fun onDiscoveryFailed() {
                Toast.makeText(this@Main3Activity, "La busca fallo", Toast.LENGTH_SHORT).show()
            }
        })

        bt!!.addConnectionListener(object : BlueDago.ConnectionListener {
            override fun onDeviceConnecting() {
                Toast.makeText(this@Main3Activity, "Conectando...", Toast.LENGTH_SHORT).show()
            }

            override fun onDeviceConnected() {
                Toast.makeText(this@Main3Activity, "Conectado", Toast.LENGTH_SHORT).show()
            }

            override fun onDeviceDisconnected() {
                Toast.makeText(this@Main3Activity, "Desconectado", Toast.LENGTH_SHORT).show()
            }

            override fun onDeviceConnectionFailed() {
                Toast.makeText(this@Main3Activity, "Falla al conectar", Toast.LENGTH_SHORT).show()
            }
        })


        bt!!.addDataReceivedListener(object : BlueDago.DataReceivedListener {
            override fun onDataReceived(data: Byte) {
                textoRecebido.append(data.toChar())
                (findViewById(R.id.received_text) as TextView).text = textoRecebido.toString()
            }
        })

        findViewById<Button>(R.id.btnProcurar).setOnClickListener {
            BlueDiscoveryDialog(
                this@Main3Activity,
                bt!!
            ).show()
        }

        findViewById<Button>(R.id.btnDesconectar).setOnClickListener { bt!!.disconnect() }

        findViewById<Button>(R.id.btnEnviar).setOnClickListener {
            val text = (findViewById(R.id.send_text) as EditText).text.toString()
            if (text.length > 0) {
                bt!!.send(text.toByteArray(Charset.forName("US-ASCII")), LineBreakType.UNIX)
            }
        }

    }

    override fun onStart() {
        super.onStart()

        if (!bt!!.isEnabled()) {
            Toast.makeText(this@Main3Activity, "Bluetooth desabilitado", Toast.LENGTH_SHORT).show()
            val i = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(i, REQUEST_ENABLE_BT)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bt!!.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        bt!!.checkDiscoveryPermissionRequest(requestCode, permissions, grantResults)
    }
}
