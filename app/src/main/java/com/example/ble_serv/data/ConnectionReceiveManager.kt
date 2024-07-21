package com.example.ble_serv.data

import com.example.ble_serv.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface ConnectionReceiveManager {
    val data: MutableSharedFlow<Resource<ConnectionResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()
}