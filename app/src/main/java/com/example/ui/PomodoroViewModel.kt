package com.example.ui

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Enumeraciones de sesión y estilos
enum class SessionType(val displayName: String) {
    WORK("Estudio"),
    SHORT_BREAK("Descanso Corto"),
    LONG_BREAK("Descanso Largo")
}

enum class ClockStyle(val displayName: String) {
    PROGRESS_RING("Anillo de Progreso ⭕"),
    CLEAN_MINIMAL("Minimalista Limpio ⏱️"),
    RETRO_CONSOLE("Consola Retro 📟")
}

// Estructura de listas de reproducción de música Lofi Real
data class LofiStation(
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val emoji: String = "🎵"
)

// Estructura para temas de colores mejorados
data class StudyThemePreset(
    val id: String,
    val name: String,
    val emoji: String,
    val primary: Color,
    val secondary: Color,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentColor: Color
)

class PomodoroViewModel(
    private val application: Application,
    private val repository: NoteRepository
) : ViewModel() {

    // Lista de canales Lofi stream públicos y con licencia libre (muy confiables)
    val lofiStations = listOf(
        LofiStation(
            id = "code_radio",
            name = "Code Radio Lofi",
            url = "https://coderadio-admin.freecodecamp.org/radio/8010/radio.mp3",
            description = "música lofi para programar 24/7",
            emoji = "💻"
        ),
        LofiStation(
            id = "chill_flow",
            name = "Flow Chillhop Beats",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            description = "compilación instrumental de chillhop",
            emoji = "☕"
        ),
        LofiStation(
            id = "ambient_zen",
            name = "Ambient Zen Forest",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            description = "melodías tranquilas para hiperenfocarse",
            emoji = "🌿"
        )
    )

    // Lista de Temas de color mejorados
    val themes = listOf(
        StudyThemePreset(
            id = "deep_focus",
            name = "Deep Focus (Dark)",
            emoji = "🌌",
            primary = Color(0xFFD0BCFF), // Violeta suave (M3 Lavender)
            secondary = Color(0xFF381E72), // Violeta oscuro
            backgroundStart = Color(0xFF0F1115), // Fondo ultra obscuro
            backgroundEnd = Color(0xFF14161D),
            cardBackground = Color(0xFF1D1B20), // Fondo de tarjeta oscuro
            textPrimary = Color(0xFFE2E2E6),
            textSecondary = Color(0xFF919196),
            accentColor = Color(0xFF232429)
        ),
        StudyThemePreset(
            id = "sunset_cafe",
            name = "Cozy Café",
            emoji = "☕",
            primary = Color(0xFFFF7043), // Terracotta
            secondary = Color(0xFFFFB74D), // Miel
            backgroundStart = Color(0xFF1E1410), // Espresso oscuro
            backgroundEnd = Color(0xFF2E1C16), 
            cardBackground = Color(0xFF2E1C16),
            textPrimary = Color(0xFFFFF3E0),
            textSecondary = Color(0xFFFFCC80),
            accentColor = Color(0xFFFFAB40)
        ),
        StudyThemePreset(
            id = "cherry_chill",
            name = "Cherry Chill",
            emoji = "🍒",
            primary = Color(0xFFFF2A6D), // Neon Cherry Red
            secondary = Color(0xFFD13D9C), // Rosa profundo
            backgroundStart = Color(0xFF12030B), // Cereza oscuro medianoche
            backgroundEnd = Color(0xFF240616),
            cardBackground = Color(0xFF240616),
            textPrimary = Color(0xFFFFF0F5),
            textSecondary = Color(0xFFFFB6C1),
            accentColor = Color(0xFF00F0FF) // Cyber Accent
        ),
        StudyThemePreset(
            id = "forest_matcha",
            name = "Matcha Zen (Relaxing)",
            emoji = "🌿",
            primary = Color(0xFF81C784), // Matcha verde claro
            secondary = Color(0xFF4CAF50), // Verde bosque
            backgroundStart = Color(0xFF0F1A12), // Te verde oscuro
            backgroundEnd = Color(0xFF1B2F21),
            cardBackground = Color(0xFF1B2F21),
            textPrimary = Color(0xFFE8F5E9),
            textSecondary = Color(0xFFA5D6A7),
            accentColor = Color(0xFFC8E6C9)
        ),
        StudyThemePreset(
            id = "light_aesthetic",
            name = "Aura Light (Claro)",
            emoji = "☀️",
            primary = Color(0xFF6750A4), // M3 Royal Blue
            secondary = Color(0xFFE8DDFF), // Lavanda claro
            backgroundStart = Color(0xFFF9F9FC), // Fondo claro estético
            backgroundEnd = Color(0xFFF1F0F5),
            cardBackground = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF1C1B1F),
            textSecondary = Color(0xFF605D62),
            accentColor = Color(0xFF7D5260)
        )
    )

    // Estados de configuración de sesión (minutos)
    private val _workMinutes = MutableStateFlow(25)
    val workMinutes = _workMinutes.asStateFlow()

    private val _shortBreakMinutes = MutableStateFlow(5)
    val shortBreakMinutes = _shortBreakMinutes.asStateFlow()

    private val _longBreakMinutes = MutableStateFlow(15)
    val longBreakMinutes = _longBreakMinutes.asStateFlow()

    // Estado del temporizador
    private val _timeRemaining = MutableStateFlow(25L * 60)
    val timeRemaining = _timeRemaining.asStateFlow()

    private val _totalDuration = MutableStateFlow(25L * 60)
    val totalDuration = _totalDuration.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _currentSession = MutableStateFlow(SessionType.WORK)
    val currentSession = _currentSession.asStateFlow()

    private val _completedSessionsCount = MutableStateFlow(0)
    val completedSessionsCount = _completedSessionsCount.asStateFlow()

    // Temas y Relojes
    private val _activeTheme = MutableStateFlow(themes[0])
    val activeTheme = _activeTheme.asStateFlow()

    private val _activeClockStyle = MutableStateFlow(ClockStyle.PROGRESS_RING)
    val activeClockStyle = _activeClockStyle.asStateFlow()

    // Sistema MediaPlayer Lofi Real
    private var mediaPlayer: android.media.MediaPlayer? = null

    private val _isPlayingMusic = MutableStateFlow(false)
    val isPlayingMusic = _isPlayingMusic.asStateFlow()

    private val _selectedStation = MutableStateFlow<String>("code_radio")
    val selectedStation = _selectedStation.asStateFlow()

    // Ambience & Sound Toggle
    private val _activeAmbientSound = MutableStateFlow<String?>(null)
    val activeAmbientSound = _activeAmbientSound.asStateFlow()

    // Notes Data List
    val notesUiState: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var timerJob: Job? = null

    // Modificar configuraciones de tiempo e inmediatamente reiniciar
    fun updateSessionDurations(work: Int, short: Int, long: Int) {
        _workMinutes.value = work.coerceIn(1, 120)
        _shortBreakMinutes.value = short.coerceIn(1, 120)
        _longBreakMinutes.value = long.coerceIn(1, 120)
        
        // Si no está corriendo, actualizar el tiempo de pantalla para reflejar la sesión actual
        if (!_isRunning.value) {
            resetTimer(force = true)
        }
    }

    // Cambiar de tema
    fun selectTheme(themeId: String) {
        themes.find { it.id == themeId }?.let {
            _activeTheme.value = it
        }
    }

    // Cambiar estilo de reloj
    fun selectClockStyle(style: ClockStyle) {
        _activeClockStyle.value = style
    }

    // Funciones de control de música LOFI Real (No interfiere con el Pomodoro)
    fun playMusic(stationId: String) {
        val station = lofiStations.find { it.id == stationId } ?: return
        _selectedStation.value = stationId
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Liberar reproductor previo
                mediaPlayer?.let {
                    try {
                        if (it.isPlaying) {
                            it.stop()
                        }
                    } catch (e: Exception) {}
                    it.release()
                }
                
                _isPlayingMusic.value = false
                
                val mp = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(station.url)
                    isLooping = true
                    
                    setOnPreparedListener {
                        it.start()
                        _isPlayingMusic.value = true
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlayingMusic.value = false
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                e.printStackTrace()
                _isPlayingMusic.value = false
            }
        }
    }

    fun pauseMusic() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
            _isPlayingMusic.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumeMusic() {
        try {
            mediaPlayer?.let {
                it.start()
                _isPlayingMusic.value = true
            } ?: run {
                playMusic(_selectedStation.value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleMusicPlayback() {
        if (_isPlayingMusic.value) {
            pauseMusic()
        } else {
            resumeMusic()
        }
    }

    fun selectStation(stationId: String) {
        _selectedStation.value = stationId
        if (_isPlayingMusic.value) {
            playMusic(stationId)
        }
    }

    // Toggle Ambient Sound card
    fun toggleAmbientSound(soundId: String) {
        if (_activeAmbientSound.value == soundId) {
            _activeAmbientSound.value = null
        } else {
            _activeAmbientSound.value = soundId
        }
    }

    // Iniciar / Pausar temporizador
    fun toggleTimer() {
        if (_isRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _isRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0) {
                delay(1000)
                _timeRemaining.value -= 1
            }
            onTimerFinish()
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer(force: Boolean = false) {
        pauseTimer()
        val duration = when (_currentSession.value) {
            SessionType.WORK -> _workMinutes.value
            SessionType.SHORT_BREAK -> _shortBreakMinutes.value
            SessionType.LONG_BREAK -> _longBreakMinutes.value
        }
        _totalDuration.value = duration * 60L
        _timeRemaining.value = duration * 60L
    }

    fun skipSession() {
        pauseTimer()
        val nextSession = when (_currentSession.value) {
            SessionType.WORK -> {
                // Alternar entre breves o largos descansos
                if ((_completedSessionsCount.value + 1) % 4 == 0) {
                    SessionType.LONG_BREAK
                } else {
                    SessionType.SHORT_BREAK
                }
            }
            SessionType.SHORT_BREAK -> SessionType.WORK
            SessionType.LONG_BREAK -> SessionType.WORK
        }
        
        _currentSession.value = nextSession
        resetTimer(force = true)
    }

    private fun onTimerFinish() {
        _isRunning.value = false
        triggerAlarmAndVibration()
        
        when (_currentSession.value) {
            SessionType.WORK -> {
                _completedSessionsCount.value += 1
                if (_completedSessionsCount.value % 4 == 0) {
                    _currentSession.value = SessionType.LONG_BREAK
                } else {
                    _currentSession.value = SessionType.SHORT_BREAK
                }
            }
            SessionType.SHORT_BREAK -> {
                _currentSession.value = SessionType.WORK
            }
            SessionType.LONG_BREAK -> {
                _currentSession.value = SessionType.WORK
            }
        }
        resetTimer(force = true)
    }

    // Disparar alarma nativa y vibración sutil
    private fun triggerAlarmAndVibration() {
        viewModelScope.launch {
            try {
                // Alarma sonora
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(application, notificationUri)
                ringtone?.play()
            } catch (e: Exception) {
                // Silencioso si ocurre error
            }

            try {
                // Vibración sutil
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(800)
                    }
                }
            } catch (e: Exception) {
                // Silencioso si ocurre error
            }
        }
    }

    // Operaciones con notas
    fun addNote(title: String, content: String, category: String, colorHex: String) {
        viewModelScope.launch {
            if (title.isNotBlank() || content.isNotBlank()) {
                val newNote = Note(
                    title = title.trim(),
                    content = content.trim(),
                    category = category,
                    tagColorHex = colorHex,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertNote(newNote)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun toggleNoteCompletion(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isCompleted = !note.isCompleted))
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }
}

// Factory para ViewModel
class PomodoroViewModelFactory(
    private val application: Application,
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            return PomodoroViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
