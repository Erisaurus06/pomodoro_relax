package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.ui.PomodoroStudyApp
import com.example.ui.PomodoroViewModel
import com.example.ui.PomodoroViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Inicializar Room persistente
    val database = AppDatabase.getDatabase(applicationContext)
    val noteDao = database.noteDao()
    val repository = NoteRepository(noteDao)

    // Crear ViewModel usando Factory
    val viewModelFactory = PomodoroViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[PomodoroViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          PomodoroStudyApp(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}
