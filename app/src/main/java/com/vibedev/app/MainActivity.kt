package com.vibedev.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vibedev.app.journal.JournalApp
import com.vibedev.app.ui.theme.VibeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeAppTheme {
                JournalApp()
                CrashReportDialog()
            }
        }
    }
}
