package com.leaseting.parkingterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.leaseting.parkingterminal.ui.ParkingTerminalApp
import com.leaseting.parkingterminal.ui.theme.LeasetingParkingTerminalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeasetingParkingTerminalTheme {
                ParkingTerminalApp()
            }
        }
    }
}
