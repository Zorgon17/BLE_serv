package com.example.ble_serv.ui.screens

import android.bluetooth.BluetoothAdapter
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ble_serv.permissions.SystemBroadcastReceiver
import com.example.ble_serv.ui.navigation.Screens

@Composable
fun ConnectionScreen(
    onBluetoothStateChanged: () -> Unit
) {

    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothStateChanged()
        }
    }
}