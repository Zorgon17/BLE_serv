package com.example.ble_serv

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                Navigation()
            }
        }
    }

    /**
     * Внедряет BluetoothAdapter с помощью Hilt и объявляет переменную для доступа к адаптеру Bluetooth.
     */
    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    /**
     * Приватная переменная, которая отслеживает, было ли уже показано диалоговое окно Bluetooth.
     */
    private var isBluetoothDialogAlreadyShown = false

    override fun onStart() {
        super.onStart()
        showBluetoothDialog()
    }

    /**
     * Вызывает функцию showBluetoothDialog() для отображения диалогового окна с просьбой включить Bluetooth.
     */
    private fun showBluetoothDialog() {
        if (!bluetoothAdapter.isEnabled) { //Проверяет, включен ли Bluetooth.
            if (!isBluetoothDialogAlreadyShown) { //Проверяет, демонстрировался ли диалог с Bluetooth.
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE) //Создаем запрос на включение Bluetooth
                startBluetoothIntentForResult.launch(enableBluetoothIntent) //Запускаем наш запрос
                isBluetoothDialogAlreadyShown = true // Меняем флаг переменной isBluetoothDialogAlreadyShown
            }
        }
    }

    private val startBluetoothIntentForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isBluetoothDialogAlreadyShown = false
            if (result.resultCode != Activity.RESULT_OK) {
                showBluetoothDialog()
            }
        }
}
