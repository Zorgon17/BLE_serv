package com.example.ble_serv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


import androidx.navigation.compose.rememberNavController
import com.example.ble_serv.ui.navigation.Screens.HomeScreen
import com.example.ble_serv.ui.screens.ConnectionScreen
import com.example.ble_serv.ui.screens.HomeScreen

@Composable
fun Navigation(onBluetoothStateChanged: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeScreen.route){
        composable(HomeScreen.route){
            HomeScreen(navController)
        }

        composable(Screens.ConnectionScreen.route){
            ConnectionScreen(onBluetoothStateChanged)
        }
    }

}

sealed class Screens(val route: String){
    object HomeScreen: Screens("home")
    object ConnectionScreen: Screens("connection")
}