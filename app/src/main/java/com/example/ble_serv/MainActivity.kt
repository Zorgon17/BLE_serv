package com.example.ble_serv

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.example.ble_serv.ui.navigation.Navigation
import com.example.ble_serv.ui.theme.BLE_servTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BLE_servTheme {
                Navigation(onBluetoothStateChanged = { showBluetoothDialog() })
            }
        }
    }

    /**
     * Внедряет BluetoothAdapter с помощью Hilt и объявляет переменную для доступа к адаптеру Bluetooth.
     */
    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    override fun onStart() {
        super.onStart()
        showBluetoothDialog()
    }

    /**
     * Запрашивает разрешение для аппаратного включения Bluetooth(если он есть)
     */
    private val REQUEST_CONST = 100

    private fun showBluetoothDialog() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_CONST
                )
                return
            } else {
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CONST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                )
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

