package com.killer.automation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.killer.automation.ui.theme.KillerAutomationTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Activity principal de la aplicación
 * Anotada con @AndroidEntryPoint para habilitar inyección de dependencias con Hilt
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity created")
        
        setContent {
            KillerAutomationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Killer Automation",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    text = "Phase 1: Hilt + Security Crypto",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✓ Hilt DI Configuration",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                        Text(
                            text = "✓ Encrypted Data Manager",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                        Text(
                            text = "✓ Security Crypto Setup",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
