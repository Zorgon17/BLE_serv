package com.example.ble_serv.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.ble_serv.data.ConnectionReceiveManager
import com.example.ble_serv.data.ConnectionResult
import com.example.ble_serv.data.ConnectionState
import com.example.ble_serv.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class ConnectionReceiveBLEManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : ConnectionReceiveManager {

    /**
     * #1 MutableSharedFlow подходит для обработки ряда выдаваемых значений, в реалиях BLE по идее must have
     */
    override val data: MutableSharedFlow<Resource<ConnectionResult>>
        get() = MutableSharedFlow()

    /**
     * #2 Создаем сканер BLE-устройств
     */
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    /**
     * #3 Настройка параметров сканирования (я так понимаю тут можно настроить режим, частоту, время сканирования)
     */
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    /**
     * #4 BluetoothGatt представляющий GATT-соединение с устройством BLE
     */
    private var gatt: BluetoothGatt? = null

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * #5 scanCallback используется для обработки результатов сканирования Bluetooth Low Energy (BLE)
     */
    private val DEVICE_NAME = "JJ3J" // изменить название на название клиента, можно использовать result.device.address
    private val CONNECTING_SERVICE_UIID = "VFVFV"
    private val CONNECTING_CHARACTERISTICS_UUID = "VFVFV"

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if(result.device.name == DEVICE_NAME){
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Подключаемся к устройству..."))
                }
                if(isScanning){
                    result.device.connectGatt(context,false, gattCallback)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }
    /**
     * #6 gattCallback обеспечивает механизм для подключения к устройству BLE, обнаружения служб,
     * изменения MTU, чтения данных с характеристик, обработки ошибок и обновления состояния в потоке data.
     * Он также включает повторные попытки подключения в случае неудачи.
     */

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val gattCallback = object : BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Ищем сервисы..."))
                    }
                    gatt.discoverServices()
                    this@ConnectionReceiveBLEManager.gatt = gatt
                } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    coroutineScope.launch {
                        data.emit(Resource.Success(data = ConnectionResult("0ff", ConnectionState.Disconnected)))
                    }
                    gatt.close()
                }
            }else{
                gatt.close()
                currentConnectionAttempt+=1
                coroutineScope.launch {
                    data.emit(
                        Resource.Loading(
                            message = "Пытаемся подключиться: $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"
                        )
                    )
                }
                if(currentConnectionAttempt<=MAXIMUM_CONNECTION_ATTEMPTS){
                    startReceiving()
                }else{
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Не возможно установить соединение с BLE девайсом"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt){
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Настраиваем размер пакета данных..."))
                }
                gatt.requestMtu(160)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic = findCharacteristics(CONNECTING_SERVICE_UIID, CONNECTING_CHARACTERISTICS_UUID)
            if(characteristic == null){
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Не удалось найти источник данных"))
                }
                return
            }
            enableNotification(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic){
                when(uuid){
                    UUID.fromString(TEMP_HUMIDITY_CHARACTERISTICS_UUID) -> {
                        //XX XX XX XX XX XX
                        val multiplicator = if(value.first().toInt()> 0) -1 else 1
                        val temperature = value[1].toInt() + value[2].toInt() / 10f
                        val humidity = value[4].toInt() + value[5].toInt() / 10f
                        val tempHumidityResult = TempHumidityResult(
                            multiplicator * temperature,
                            humidity,
                            ConnectionState.Connected
                        )
                        coroutineScope.launch {
                            data.emit(
                                Resource.Success(data = tempHumidityResult)
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun findCharacteristics(serviceUUID: String, characteristicsUUID:String):BluetoothGattCharacteristic?{
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic, true) == false){
                Log.d("BLEReceiveManager","set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Сканируем BLE девайсы..."))
        }
        isScanning = true
        bleScanner.startScan(null,scanSettings,scanCallback)
    }





















    override fun reconnect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun closeConnection() {
        TODO("Not yet implemented")
    }
}
