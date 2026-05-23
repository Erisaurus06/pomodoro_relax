package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PomodoroStudyApp(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Estados observados desde el ViewModel
    val activeTheme by viewModel.activeTheme.collectAsState()
    val activeClockStyle by viewModel.activeClockStyle.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val completedSessions by viewModel.completedSessionsCount.collectAsState()
    val activeAmbientSound by viewModel.activeAmbientSound.collectAsState()
    val notes by viewModel.notesUiState.collectAsState()

    // Configuraciones de minutos
    val workMins by viewModel.workMinutes.collectAsState()
    val shortMins by viewModel.shortBreakMinutes.collectAsState()
    val longMins by viewModel.longBreakMinutes.collectAsState()

    // Estados de edición de notas locales
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("Estudio") }
    var noteColorSelected by remember { mutableStateOf("#D0BCFF") }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    // Dialogo de configuración de tiempos
    var showConfigDialog by remember { mutableStateOf(false) }

    // Color del fondo animado basado en el tema activo
    val animatedBgStart by animateColorAsState(targetValue = activeTheme.backgroundStart, label = "bgStart")
    val animatedBgEnd by animateColorAsState(targetValue = activeTheme.backgroundEnd, label = "bgEnd")

    // Layout principal con gradiente
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedBgStart, animatedBgEnd)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (isLandscape) {
            // Layout horizontal de dos columnas
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Columna izquierda: Reloj Pomodoro y controles de música
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppHeader(completedSessions, activeTheme) { showConfigDialog = true }

                    PomodoroClockSection(
                        activeClockStyle = activeClockStyle,
                        currentSession = currentSession,
                        timeRemaining = timeRemaining,
                        totalDuration = totalDuration,
                        isRunning = isRunning,
                        theme = activeTheme,
                        onToggle = { viewModel.toggleTimer() },
                        onReset = { viewModel.resetTimer() },
                        onSkip = { viewModel.skipSession() },
                        onClockStyleChange = { viewModel.selectClockStyle(it) }
                    )

                    ThemeSelectorSection(
                        themes = viewModel.themes,
                        activeTheme = activeTheme,
                        onThemeSelect = { viewModel.selectTheme(it) }
                    )

                    LofiMusicSection(
                        activeAmbientSound = activeAmbientSound,
                        theme = activeTheme,
                        onToggleSound = { viewModel.toggleAmbientSound(it) },
                        viewModel = viewModel
                    )
                }

                // Columna derecha: Listado de notas y adición
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NotesSectionHeader(theme = activeTheme)

                    NoteAddForm(
                        title = noteTitle,
                        content = noteContent,
                        category = noteCategory,
                        colorHex = noteColorSelected,
                        theme = activeTheme,
                        editingNote = editingNote,
                        onCancelEdit = {
                            editingNote = null
                            noteTitle = ""
                            noteContent = ""
                            noteCategory = "Estudio"
                            noteColorSelected = "#D0BCFF"
                        },
                        onTitleChange = { noteTitle = it },
                        onContentChange = { noteContent = it },
                        onCategoryChange = { noteCategory = it },
                        onColorChange = { noteColorSelected = it },
                        onAddNote = {
                            if (editingNote != null) {
                                viewModel.updateNote(
                                    editingNote!!.copy(
                                        title = noteTitle,
                                        content = noteContent,
                                        category = noteCategory,
                                        tagColorHex = noteColorSelected
                                    )
                                )
                                editingNote = null
                            } else {
                                viewModel.addNote(noteTitle, noteContent, noteCategory, noteColorSelected)
                            }
                            noteTitle = ""
                            noteContent = ""
                        }
                    )

                    NotesList(
                        notes = notes,
                        theme = activeTheme,
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onEditNote = { note ->
                            editingNote = note
                            noteTitle = note.title
                            noteContent = note.content
                            noteCategory = note.category
                            noteColorSelected = note.tagColorHex
                        },
                        onToggleComplete = { note ->
                            viewModel.toggleNoteCompletion(note)
                        }
                    )
                }
            }
        } else {
            // Layout vertical para móviles
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AppHeader(completedSessions, activeTheme) { showConfigDialog = true }

                PomodoroClockSection(
                    activeClockStyle = activeClockStyle,
                    currentSession = currentSession,
                    timeRemaining = timeRemaining,
                    totalDuration = totalDuration,
                    isRunning = isRunning,
                    theme = activeTheme,
                    onToggle = { viewModel.toggleTimer() },
                    onReset = { viewModel.resetTimer() },
                    onSkip = { viewModel.skipSession() },
                    onClockStyleChange = { viewModel.selectClockStyle(it) }
                )

                ThemeSelectorSection(
                    themes = viewModel.themes,
                    activeTheme = activeTheme,
                    onThemeSelect = { viewModel.selectTheme(it) }
                )

                LofiMusicSection(
                    activeAmbientSound = activeAmbientSound,
                    theme = activeTheme,
                    onToggleSound = { viewModel.toggleAmbientSound(it) },
                    viewModel = viewModel
                )

                Divider(color = activeTheme.textSecondary.copy(alpha = 0.15f), thickness = 1.dp)

                NotesSectionHeader(theme = activeTheme)

                NoteAddForm(
                    title = noteTitle,
                    content = noteContent,
                    category = noteCategory,
                    colorHex = noteColorSelected,
                    theme = activeTheme,
                    editingNote = editingNote,
                    onCancelEdit = {
                        editingNote = null
                        noteTitle = ""
                        noteContent = ""
                        noteCategory = "Estudio"
                        noteColorSelected = "#D0BCFF"
                    },
                    onTitleChange = { noteTitle = it },
                    onContentChange = { noteContent = it },
                    onCategoryChange = { noteCategory = it },
                    onColorChange = { noteColorSelected = it },
                    onAddNote = {
                        if (editingNote != null) {
                            viewModel.updateNote(
                                editingNote!!.copy(
                                    title = noteTitle,
                                    content = noteContent,
                                    category = noteCategory,
                                    tagColorHex = noteColorSelected
                                )
                            )
                            editingNote = null
                        } else {
                            viewModel.addNote(noteTitle, noteContent, noteCategory, noteColorSelected)
                        }
                        noteTitle = ""
                        noteContent = ""
                    }
                )

                // Altura fija en vista vertical para que no se extienda infinitamente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    NotesList(
                        notes = notes,
                        theme = activeTheme,
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onEditNote = { note ->
                            editingNote = note
                            noteTitle = note.title
                            noteContent = note.content
                            noteCategory = note.category
                            noteColorSelected = note.tagColorHex
                        },
                        onToggleComplete = { note ->
                            viewModel.toggleNoteCompletion(note)
                        }
                    )
                }
            }
        }

        // Dialogo de personalización fina de tiempo (Pomodoro/Descansos)
        if (showConfigDialog) {
            TimeSettingsDialog(
                currentWork = workMins,
                currentShort = shortMins,
                currentLong = longMins,
                theme = activeTheme,
                onDismiss = { showConfigDialog = false },
                onSave = { w, s, l ->
                    viewModel.updateSessionDurations(w, s, l)
                    showConfigDialog = false
                }
            )
        }
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
fun AppHeader(
    completedSessions: Int,
    theme: StudyThemePreset,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "PomoLofi Studio | Sesión $completedSessions".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = theme.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (theme.id == "deep_focus") "Deep Focus" else "Estudio Zen",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                ),
                color = theme.primary
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .background(theme.cardBackground, RoundedCornerShape(12.dp))
                .border(1.dp, theme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .size(44.dp)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurar Tiempos",
                tint = theme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PomodoroClockSection(
    activeClockStyle: ClockStyle,
    currentSession: SessionType,
    timeRemaining: Long,
    totalDuration: Long,
    isRunning: Boolean,
    theme: StudyThemePreset,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onClockStyleChange: (ClockStyle) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = theme.primary.copy(alpha = 0.3f),
                clip = false
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackground.copy(alpha = 0.85f)),
        border = BorderStroke(1.2.dp, theme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fila de Modos Pomodoro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.backgroundStart.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionType.values().forEach { type ->
                    val isActive = currentSession == type
                    val stateBg = if (isActive) theme.primary else Color.Transparent
                    val stateTextColor = if (isActive) theme.backgroundStart else theme.textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(30.dp))
                            .background(stateBg)
                            .clickable { /* Mostrar info o no alterar, el temporizador guía auto */ }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = stateTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // El Reloj Central Ajustable
            Box(
                modifier = Modifier
                    .height(210.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (activeClockStyle) {
                    ClockStyle.PROGRESS_RING -> {
                        ProgressRingClock(
                            timeRemaining = timeRemaining,
                            totalDuration = totalDuration,
                            isRunning = isRunning,
                            theme = theme
                        )
                    }
                    ClockStyle.CLEAN_MINIMAL -> {
                        CleanMinimalClock(
                            timeRemaining = timeRemaining,
                            totalDuration = totalDuration,
                            isRunning = isRunning,
                            theme = theme
                        )
                    }
                    ClockStyle.RETRO_CONSOLE -> {
                        RetroConsoleClock(
                            timeRemaining = timeRemaining,
                            currentSession = currentSession,
                            isRunning = isRunning,
                            theme = theme
                        )
                    }
                }
            }

            // Selector de Estilo de Reloj (Cambio de reloj)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estilo de Reloj: ",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = theme.textSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                
                Row(
                    modifier = Modifier
                        .background(theme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ClockStyle.values().forEach { style ->
                        val isSelected = activeClockStyle == style
                        val btnBg = if (isSelected) theme.primary.copy(alpha = 0.25f) else Color.Transparent
                        val textBorder = if (isSelected) theme.primary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(btnBg)
                                .border(0.8.dp, textBorder, RoundedCornerShape(10.dp))
                                .clickable { onClockStyleChange(style) }
                                .padding(vertical = 4.dp, horizontal = 10.dp)
                        ) {
                            Text(
                                text = style.displayName.take(1) + style.displayName.drop(1).take(2), // Primeros caracteres / emoji
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                ),
                                color = if (isSelected) theme.textPrimary else theme.textSecondary
                            )
                        }
                    }
                }
            }

            // Botones de acción principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reiniciar
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .background(theme.textSecondary.copy(alpha = 0.1f), CircleShape)
                        .size(50.dp)
                        .testTag("reset_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar Temporizador",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause
                val playPauseBg = theme.primary
                val buttonScale = rememberInfiniteTransition(label = "").animateFloat(
                    initialValue = 1f,
                    targetValue = if (isRunning) 1.03f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Box(
                    modifier = Modifier
                        .scale(buttonScale.value)
                        .clip(CircleShape)
                        .background(playPauseBg)
                        .clickable { onToggle() }
                        .size(68.dp)
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pausar" else "Iniciar",
                        tint = theme.backgroundStart,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Saltar Sesión
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .background(theme.textSecondary.copy(alpha = 0.1f), CircleShape)
                        .size(50.dp)
                        .testTag("skip_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = "Saltar Sesión",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ------------------------------------------
// ESTILOS DE RELOJ INDIVIDUALES
// ------------------------------------------

@Composable
fun ProgressRingClock(
    timeRemaining: Long,
    totalDuration: Long,
    isRunning: Boolean,
    theme: StudyThemePreset
) {
    val progress = if (totalDuration > 0) timeRemaining.toFloat() / totalDuration.toFloat() else 0f
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    // Detalle de pulso cuando está corriendo
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val alphaPulse = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isRunning) 0.85f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.size(208.dp),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric Glow (Brillo atmosférico del diseño)
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            theme.primary.copy(alpha = if (isRunning) 0.22f else 0.12f),
                            theme.secondary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfWidth = size.width / 2
            val halfHeight = size.height / 2
            val radius = size.minDimension / 2 - 12.dp.toPx()

            // Círculo de fondo con la estética #353439
            drawCircle(
                color = theme.textSecondary.copy(alpha = 0.12f),
                radius = radius,
                center = Offset(halfWidth, halfHeight),
                style = Stroke(width = 4.dp.toPx())
            )

            // Arco sutil de fondo completo
            drawArc(
                color = theme.textSecondary.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Arco de progreso principal (línea fina moderna)
            drawArc(
                color = theme.primary,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Halo luminoso de progreso discreto
            drawArc(
                color = theme.primary.copy(alpha = alphaPulse.value * 0.25f),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-2).sp
                ),
                color = theme.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isRunning) "FOCUSING" else "PAUSE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                color = if (isRunning) theme.primary else theme.textSecondary
            )
        }
    }
}

@Composable
fun CleanMinimalClock(
    timeRemaining: Long,
    totalDuration: Long,
    isRunning: Boolean,
    theme: StudyThemePreset
) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val progress = if (totalDuration > 0) timeRemaining.toFloat() / totalDuration.toFloat() else 0f

    val pulseScale by animateFloatAsState(
        targetValue = if (isRunning) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .scale(pulseScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(theme.primary.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                .border(1.dp, theme.primary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(vertical = 14.dp, horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                ),
                color = theme.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Barra de progreso minimalista lineal
        Column(
            modifier = Modifier.width(220.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = theme.primary,
                trackColor = theme.textSecondary.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "${(progress * 100).toInt()}% Restante",
                style = MaterialTheme.typography.bodySmall,
                color = theme.textSecondary
            )
        }
    }
}

@Composable
fun RetroConsoleClock(
    timeRemaining: Long,
    currentSession: SessionType,
    isRunning: Boolean,
    theme: StudyThemePreset
) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    // Cursor parpadeante terminal
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorVisible = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .background(Color(0xFF070C09), RoundedCornerShape(12.dp))
            .border(2.dp, if (isRunning) theme.primary else theme.textSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stats bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SYS_CLOCK: v2.6",
                fontFamily = FontFamily.Monospace,
                color = theme.primary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "STATUS: ${if (isRunning) "RUNNING" else "HALTED"}",
                fontFamily = FontFamily.Monospace,
                color = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5252),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }

        Divider(color = theme.primary.copy(alpha = 0.2f), thickness = 1.dp)

        // Reloj terminal verde gigante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = theme.primary
            )
        }

        // Línea de comando interactiva
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "pomo@studio:~$ ",
                fontFamily = FontFamily.Monospace,
                color = theme.textSecondary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            )
            
            val statusCommandText = if (isRunning) {
                "studying_${currentSession.name.lowercase()}..."
            } else {
                "waiting_to_spark_focus..."
            }
            
            Text(
                text = statusCommandText,
                fontFamily = FontFamily.Monospace,
                color = theme.textPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )

            if (cursorVisible.value > 0.5f) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 12.dp)
                        .background(theme.primary)
                )
            }
        }
    }
}

// ------------------------------------------
// SELECCIÓN DE TEMAS MEJORADOS
// ------------------------------------------

@Composable
fun ThemeSelectorSection(
    themes: List<StudyThemePreset>,
    activeTheme: StudyThemePreset,
    onThemeSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Selector de Ambiente Visual",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = activeTheme.textPrimary
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(themes) { item ->
                val isSelected = activeTheme.id == item.id
                val borderAlpha = if (isSelected) 1f else 0.25f
                val borderCol = if (isSelected) item.primary else item.textSecondary.copy(alpha = borderAlpha)

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .shadow(
                            elevation = if (isSelected) 4.dp else 0.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = true
                        )
                        .clickable { onThemeSelect(item.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) item.primary.copy(alpha = 0.2f) else item.cardBackground.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.4.dp, borderCol)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(item.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.emoji, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
                        }
                        
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = item.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// LOFI Y ENLACES DIRECTOS A MÚSICA
// ------------------------------------------

@Composable
fun LofiMusicSection(
    activeAmbientSound: String?,
    theme: StudyThemePreset,
    onToggleSound: (String) -> Unit,
    viewModel: PomodoroViewModel
) {
    val context = LocalContext.current
    
    // Estados observados del Reproductor Lofi
    val isPlayingMusic by viewModel.isPlayingMusic.collectAsState()
    val selectedStationId by viewModel.selectedStation.collectAsState()
    val stations = viewModel.lofiStations
    
    val currentStation = remember(selectedStationId) {
        stations.find { it.id == selectedStationId } ?: stations[0]
    }
    
    val currentStationIdx = remember(selectedStationId) {
        stations.indexOfFirst { it.id == selectedStationId }.coerceAtLeast(0)
    }

    val onPrevStation = {
        val prevIdx = if (currentStationIdx == 0) stations.lastIndex else currentStationIdx - 1
        viewModel.selectStation(stations[prevIdx].id)
    }
    val onNextStation = {
        val nextIdx = if (currentStationIdx == stations.lastIndex) 0 else currentStationIdx + 1
        viewModel.selectStation(stations[nextIdx].id)
    }

    // Enlaces directos a plataformas lofi externas
    val links = listOf(
        LofiMusicLink("Lofi Girl 🎧", "https://www.youtube.com/watch?v=jfKfPfyJRdk", Icons.Default.PlayArrow),
        LofiMusicLink("Spotify Lofi ☕", "https://open.spotify.com/playlist/37i9dQZF1DWWQRwui0SqnX", Icons.Default.PlayArrow),
        LofiMusicLink("Lofi.cafe 💻", "https://lofi.cafe", Icons.Default.PlayArrow)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Reproductor de Música Lofi Real",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = theme.textPrimary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground.copy(alpha = 0.65f)),
            border = BorderStroke(1.dp, theme.textSecondary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PANEL PRINCIPAL DEL REPRODUCTOR DE MÚSICA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.backgroundStart.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .border(1.dp, theme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // INFO DE LA PISTA ACTUAL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(theme.primary, theme.secondary)
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentStation.emoji,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentStation.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentStation.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isPlayingMusic) {
                            AmbientWaveAnimation(color = theme.primary)
                        }
                    }

                    // BOTONERA DE CONTROLES MULTIMEDIA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Anterior
                        IconButton(
                            onClick = onPrevStation,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Pista Anterior",
                                tint = theme.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Play/Pause Principal
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(theme.primary, CircleShape)
                                .clickable {
                                    if (isPlayingMusic) {
                                        viewModel.pauseMusic()
                                    } else {
                                        viewModel.playMusic(currentStation.id)
                                    }
                                }
                                .testTag("lofi_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlayingMusic) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(4.dp, 16.dp).background(theme.backgroundStart, RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.size(4.dp, 16.dp).background(theme.backgroundStart, RoundedCornerShape(2.dp)))
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = theme.backgroundStart,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Siguiente
                        IconButton(
                            onClick = onNextStation,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Siguiente Pista",
                                tint = theme.textPrimary
                            )
                        }
                    }
                }

                // LISTA DE CANALES ACORDEÓN / SECTOR RÁPIDO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stations.forEach { station ->
                        val isSelected = station.id == selectedStationId
                        val chipBg = if (isSelected) theme.primary.copy(alpha = 0.22f) else theme.backgroundStart.copy(alpha = 0.3f)
                        val chipBorder = if (isSelected) theme.primary else theme.textSecondary.copy(alpha = 0.15f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectStation(station.id)
                                    if (!isPlayingMusic) {
                                        viewModel.playMusic(station.id)
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = station.emoji,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = station.name.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = theme.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Divider(color = theme.textSecondary.copy(alpha = 0.1f), thickness = 0.8.dp)

                // Enlaces directos a música Lofi real externa
                Text(
                    text = "REPRODUCIR EN PLATAFORMAS EXTERNAS:",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(links) { link ->
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback directo
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primary.copy(alpha = 0.15f),
                                contentColor = theme.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = link.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = theme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = link.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Divider(color = theme.textSecondary.copy(alpha = 0.1f), thickness = 0.8.dp)

                // Generador de sonidos ambientales simulados
                Text(
                    text = "EFECTOS AMBIENTALES DE ESTUDIO:",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )

                val sounds = listOf(
                    AmbientSoundItem("rain", "🌧️ Lluvia"),
                    AmbientSoundItem("fire", "🔥 Chimenea"),
                    AmbientSoundItem("wind", "🍃 Viento"),
                    AmbientSoundItem("cafe", "☕ Cafetería")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sounds.forEach { sound ->
                        val isPlay = activeAmbientSound == sound.id
                        val soundBg = if (isPlay) theme.primary else theme.textSecondary.copy(alpha = 0.08f)
                        val soundTextCol = if (isPlay) theme.backgroundStart else theme.textPrimary

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(soundBg)
                                .clickable { onToggleSound(sound.id) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = sound.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isPlay) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = soundTextCol,
                                    textAlign = TextAlign.Center
                                )
                                
                                if (isPlay) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AmbientWaveAnimation(color = soundTextCol)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LofiMusicLink(val title: String, val url: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
data class AmbientSoundItem(val id: String, val label: String)

@Composable
fun AmbientWaveAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveHeight1 = infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "w1"
    )
    val waveHeight2 = infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "w2"
    )
    val waveHeight3 = infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "w3"
    )

    Row(
        modifier = Modifier.height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = (waveHeight1.value * 10).dp.coerceAtLeast(2.dp)).background(color, CircleShape))
        Box(modifier = Modifier.size(width = 3.dp, height = (waveHeight2.value * 10).dp.coerceAtLeast(2.dp)).background(color, CircleShape))
        Box(modifier = Modifier.size(width = 3.dp, height = (waveHeight3.value * 10).dp.coerceAtLeast(2.dp)).background(color, CircleShape))
    }
}

// ------------------------------------------
// PANEL DE NOTAS DE ESTUDIO (ROOM)
// ------------------------------------------

@Composable
fun NotesSectionHeader(theme: StudyThemePreset) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = theme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Notas y Metas de Estudio",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
        }
        
        Text(
            text = "Espacio Persistente",
            style = MaterialTheme.typography.labelSmall,
            color = theme.textSecondary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NoteAddForm(
    title: String,
    content: String,
    category: String,
    colorHex: String,
    theme: StudyThemePreset,
    editingNote: Note?,
    onCancelEdit: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onAddNote: () -> Unit
) {
    val predefinedColors = listOf("#D0BCFF", "#FF7043", "#AB7FFC", "#81C784", "#00E5FF")
    val predefinedCategories = listOf("Estudio", "Tareas", "Ideas")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackground.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, theme.textSecondary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera si se está editando
            if (editingNote != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✍️ Editando Nota / Checklist",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.primary
                    )
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = theme.textSecondary,
                        modifier = Modifier
                            .clickable { onCancelEdit() }
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            // Título Note
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("Título de la Nota...", color = theme.textSecondary.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = theme.textPrimary, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            // Contenido Note
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = { Text("Anotar pendiente, fórmulas, ideas...", color = theme.textSecondary.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .testTag("note_content_input"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = theme.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.2f)
                ),
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de categoría rápido
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    predefinedCategories.forEach { cat ->
                        val isCatSel = category == cat
                        val catBg = if (isCatSel) theme.primary.copy(alpha = 0.3f) else Color.Transparent
                        val catBorder = if (isCatSel) theme.primary else theme.textSecondary.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(catBg)
                                .border(1.dp, catBorder, RoundedCornerShape(8.dp))
                                .clickable { onCategoryChange(cat) }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = theme.textPrimary
                            )
                        }
                    }
                }

                // Selector de color pin rápido
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    predefinedColors.forEach { colStr ->
                        val itemCol = Color(android.graphics.Color.parseColor(colStr))
                        val isColSel = colorHex == colStr
                        val borderCol = if (isColSel) theme.textPrimary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(itemCol, CircleShape)
                                .border(1.5.dp, borderCol, CircleShape)
                                .clickable { onColorChange(colStr) }
                        )
                    }
                }
            }

            // Botón de guardar nota
            Button(
                onClick = onAddNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_note_button"),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank() || content.isNotBlank()
            ) {
                Icon(
                    imageVector = if (editingNote != null) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = theme.backgroundStart
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (editingNote != null) "Actualizar Nota" else "Guardar Nota",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.backgroundStart
                )
            }
        }
    }
}

@Composable
fun NotesList(
    notes: List<Note>,
    theme: StudyThemePreset,
    onDeleteNote: (Note) -> Unit,
    onEditNote: (Note) -> Unit,
    onToggleComplete: (Note) -> Unit
) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = theme.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No tienes notas de estudio.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.textSecondary
                )
                Text(
                    text = "Úsalas para guardar tus fórmulas, links o metas mientras temporizas el estudio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(170.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes) { note ->
                NoteCard(
                    note = note,
                    theme = theme,
                    onDelete = { onDeleteNote(note) },
                    onEdit = { onEditNote(note) },
                    onToggleComplete = { onToggleComplete(note) }
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    theme: StudyThemePreset,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val noteColor = remember(note.tagColorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.tagColorHex))
        } catch (e: Exception) {
            theme.primary
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val dateString = remember(note.timestamp) { dateFormatter.format(Date(note.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isCompleted) {
                theme.backgroundStart.copy(alpha = 0.35f)
            } else {
                theme.backgroundStart.copy(alpha = 0.7f)
            }
        ),
        border = BorderStroke(
            1.5.dp,
            if (note.isCompleted) noteColor.copy(alpha = 0.25f) else noteColor.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Checkbox para tareas/notas completables
                    Checkbox(
                        checked = note.isCompleted,
                        onCheckedChange = { _ -> onToggleComplete() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = theme.primary,
                            uncheckedColor = theme.textSecondary.copy(alpha = 0.4f),
                            checkmarkColor = theme.backgroundStart
                        ),
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("note_checkbox_${note.id}")
                    )

                    // Chip de categoría
                    Box(
                        modifier = Modifier
                            .background(noteColor.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                            .padding(vertical = 2.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = note.category,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = noteColor
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Editar
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onEdit() }
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Nota",
                            tint = theme.textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Borrar
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDelete() }
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar Nota",
                            tint = theme.textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (note.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = if (note.isCompleted) theme.textSecondary.copy(alpha = 0.5f) else theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (note.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = if (note.isCompleted) theme.textSecondary.copy(alpha = 0.4f) else theme.textSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = theme.textSecondary.copy(alpha = 0.4f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ------------------------------------------
// DIALOGO DE CONFIGURACIÓN DE TIEMPO
// ------------------------------------------

@Composable
fun TimeSettingsDialog(
    currentWork: Int,
    currentShort: Int,
    currentLong: Int,
    theme: StudyThemePreset,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var work by remember { mutableStateOf(currentWork) }
    var short by remember { mutableStateOf(currentShort) }
    var long by remember { mutableStateOf(currentLong) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Personalizar Tiempos (Min)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Work block
                TimeSettingsSelector(label = "Tiempo de Estudio (min)", value = work, theme = theme, onValueChange = { work = it })
                // Short break block
                TimeSettingsSelector(label = "Descanso Corto (min)", value = short, theme = theme, onValueChange = { short = it })
                // Long break block
                TimeSettingsSelector(label = "Descanso Largo (min)", value = long, theme = theme, onValueChange = { long = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(work, short, long) },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
            ) {
                Text(text = "Guardar", color = theme.backgroundStart, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = theme.textSecondary)
            ) {
                Text(text = "Cancelar")
            }
        },
        containerColor = theme.backgroundEnd,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.dp, theme.primary.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
    )
}

@Composable
fun TimeSettingsSelector(
    label: String,
    value: Int,
    theme: StudyThemePreset,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = theme.textSecondary)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.primary.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                modifier = Modifier
                    .background(theme.primary.copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Bajar", tint = theme.primary)
            }
            
            Text(
                text = "$value min",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = theme.textPrimary
            )

            IconButton(
                onClick = { if (value < 120) onValueChange(value + 1) },
                modifier = Modifier
                    .background(theme.primary.copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = theme.primary)
            }
        }
    }
}

// Icon Wrapper sutil para simplificar llamadas de compatibilidad
@Composable
fun Icon(
    imageName: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = imageName,
        contentDescription = description,
        tint = tint,
        modifier = modifier
    )
}
