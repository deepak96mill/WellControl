package com.wellcontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WellTheme {
                WellApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.polling = true
    }

    override fun onPause() {
        super.onPause()
        vm.polling = false
    }
}
