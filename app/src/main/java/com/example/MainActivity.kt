package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StudyDatabase
import com.example.data.StudyRepository
import com.example.data.StudySchedule
import com.example.data.StudySession
import com.example.data.StudyTask
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.StudyViewModel
import com.example.ui.StudyViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    DASHBOARD, TIMER, SCHEDULE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local DB, Repository, and ViewModel using standard provider
        val database = StudyDatabase.getInstance(this)
        val repository = StudyRepository(database.studyDao())
        val viewModel = ViewModelProvider(
            this,
            StudyViewModelFactory(repository)
        )[StudyViewModel::class.java]

        setContent {
            MyApplicationTheme {
                StudyApp(viewModel)
            }
        }
    }
}

// Helper to determine subject specific brand colors
fun getSubjectColor(subject: String): Color {
    return when (subject.trim().lowercase()) {
        "mathematics", "math" -> Color(0xFF3B82F6) // Ocean Blue
        "history", "social" -> Color(0xFFF97316) // Warm Orange
        "computer science", "cs", "coding" -> Color(0xFF8B5CF6) // Royal Purple
        "english", "literature", "languages" -> Color(0xFFEC4899) // Radiant Pink
        "science", "chemistry", "physics", "biology" -> Color(0xFF10B981) // Emerald Green
        else -> Color(0xFF64748B) // Slate Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyApp(viewModel: StudyViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    
    // Dialog state
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showManualLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Study Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Focus • Analytics • Schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "App Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { currentScreen = Screen.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.TIMER,
                    onClick = { currentScreen = Screen.TIMER },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Focus Timer") },
                    label = { Text("Timer") },
                    modifier = Modifier.testTag("tab_timer")
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.SCHEDULE,
                    onClick = { currentScreen = Screen.SCHEDULE },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
                    label = { Text("Schedules") },
                    modifier = Modifier.testTag("tab_schedule")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTimer = { currentScreen = Screen.TIMER },
                    onShowAddTask = { showAddTaskDialog = true },
                    onShowManualLog = { showManualLogDialog = true }
                )
                Screen.TIMER -> TimerScreen(
                    viewModel = viewModel
                )
                Screen.SCHEDULE -> ScheduleScreen(
                    viewModel = viewModel,
                    onShowAddSchedule = { showAddScheduleDialog = true }
                )
            }

            // Dialogs
            if (showAddTaskDialog) {
                AddTaskDialog(
                    onDismiss = { showAddTaskDialog = false },
                    onConfirm = { title, subject, duration ->
                        viewModel.addTask(title, subject, duration)
                        showAddTaskDialog = false
                    }
                )
            }

            if (showAddScheduleDialog) {
                AddScheduleDialog(
                    onDismiss = { showAddScheduleDialog = false },
                    onConfirm = { subject, day, time, duration ->
                        viewModel.addSchedule(subject, day, time, duration)
                        showAddScheduleDialog = false
                    }
                )
            }

            if (showManualLogDialog) {
                ManualLogDialog(
                    onDismiss = { showManualLogDialog = false },
                    onConfirm = { title, subject, duration ->
                        viewModel.logManualSession(title, subject, duration)
                        showManualLogDialog = false
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DASHBOARD SCREEN
// -----------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToTimer: () -> Unit,
    onShowAddTask: () -> Unit,
    onShowManualLog: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    // Aggregate statistics
    val totalSessions = sessions.size
    val totalMinutes = sessions.sumOf { it.durationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Welcome & Analytics Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Your Study Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Studied",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${totalMinutes} min",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(40.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                        )

                        Column {
                            Text(
                                text = "Sessions completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$totalSessions sessions",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "💡 Study tip: Alternate subjects every 45 minutes to prevent brain fatigue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onShowAddTask,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_task_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Task", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShowManualLog,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("log_session_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = "Log Session")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Session", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Tasks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Tasks Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tasks.filter { !it.isCompleted }.size} active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val activeTasks = tasks.filter { !it.isCompleted }
        if (activeTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "No tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No study tasks yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a task above to plan your next study focus session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeTasks, key = { it.id }) { task ->
                TaskItemRow(
                    task = task,
                    onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                    onStartFocus = {
                        viewModel.selectTaskForTimer(task)
                        onNavigateToTimer()
                    },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
        }

        // Recent Activity History Section
        item {
            Text(
                text = "Recent Study Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    text = "No study sessions recorded yet. Start the focus timer to record your progress!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(sessions.take(5), key = { it.id }) { session ->
                SessionHistoryRow(
                    session = session,
                    onDelete = { viewModel.deleteSession(session) }
                )
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: StudyTask,
    onCheckedChange: (Boolean) -> Unit,
    onStartFocus: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectColor = getSubjectColor(task.subject)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehindSubjectIndicator(subjectColor)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("task_check_${task.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subject pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(subjectColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.subject,
                            color = subjectColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Target minutes badge
                    Text(
                        text = "⏱️ ${task.targetMinutes}m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick focus button
            IconButton(
                onClick = onStartFocus,
                modifier = Modifier.testTag("focus_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Focus",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete task button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SessionHistoryRow(
    session: StudySession,
    onDelete: () -> Unit
) {
    val subjectColor = getSubjectColor(session.subject)
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.timestamp) { formatter.format(Date(session.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(subjectColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalLibrary,
                    contentDescription = null,
                    tint = subjectColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.taskTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${session.subject} • $formattedDate",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "+${session.durationMinutes}m",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Draw a elegant vertical color indicator on the left side of the row
fun Modifier.drawBehindSubjectIndicator(color: Color): Modifier = this.drawBehind {
    drawRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(12f, size.height)
    )
}

// -----------------------------------------------------------------------------
// TIMER SCREEN
// -----------------------------------------------------------------------------
@Composable
fun TimerScreen(viewModel: StudyViewModel) {
    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsStateWithLifecycle()
    val totalDurationMinutes by viewModel.timerDurationMinutes.collectAsStateWithLifecycle()
    val selectedTask by viewModel.selectedTaskForTimer.collectAsStateWithLifecycle()

    val totalDurationSeconds = totalDurationMinutes * 60
    val progress = if (totalDurationSeconds > 0) {
        timeLeftSeconds.toFloat() / totalDurationSeconds.toFloat()
    } else 1f

    // Format remaining time as MM:SS
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active focus details
        Text(
            text = "Focus Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalActivity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedTask != null) {
                        "Focusing on: ${selectedTask?.title} (${selectedTask?.subject})"
                    } else {
                        "General Study Focus"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Large Circle Timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .padding(12.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background track
                drawCircle(
                    color = trackColor,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                // Countdown sweep
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isRunning) "Stay focused!" else "Ready to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Adjust/Preset Row (Only allow when timer is paused/stopped)
        AnimatedVisibility(
            visible = !isRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    val isSelected = totalDurationMinutes == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateTimerDuration(mins) },
                        label = { Text("$mins m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("preset_$mins")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Timer Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtract 5 mins button
            IconButton(
                onClick = {
                    val currentMins = timeLeftSeconds / 60
                    if (currentMins > 5) {
                        viewModel.updateTimerDuration(currentMins - 5)
                    }
                },
                enabled = !isRunning,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("subtract_time_button")
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove 5 Mins")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main Play/Pause Button
            Button(
                onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("toggle_timer_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Add 5 mins button
            IconButton(
                onClick = {
                    val currentMins = timeLeftSeconds / 60
                    viewModel.updateTimerDuration(currentMins + 5)
                },
                enabled = !isRunning,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_time_button")
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add 5 Mins")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Button
        OutlinedButton(
            onClick = { viewModel.resetTimer() },
            modifier = Modifier
                .width(120.dp)
                .testTag("reset_timer_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset")
        }
    }
}

// -----------------------------------------------------------------------------
// SCHEDULE SCREEN
// -----------------------------------------------------------------------------
@Composable
fun ScheduleScreen(
    viewModel: StudyViewModel,
    onShowAddSchedule: () -> Unit
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf("Monday") }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Study Agenda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onShowAddSchedule,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_schedule_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Day Selector scrollable row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysOfWeek) { day ->
                val isSelected = day == selectedDay
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDay = day },
                    label = { Text(day.take(3)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("day_chip_$day")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filtered Schedules
        val daySchedules = schedules.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }

        if (daySchedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Empty Schedule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No study blocks scheduled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add custom study slots for $selectedDay to establish a highly structured, productive routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(daySchedules, key = { it.id }) { schedule ->
                    ScheduleRowItem(
                        schedule = schedule,
                        onDelete = { viewModel.deleteSchedule(schedule) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleRowItem(
    schedule: StudySchedule,
    onDelete: () -> Unit
) {
    val subjectColor = getSubjectColor(schedule.subject)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehindSubjectIndicator(subjectColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(subjectColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = subjectColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🕒 ${schedule.time}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⏱️ ${schedule.durationMinutes} mins",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_schedule_${schedule.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Scheduled Slot",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DIALOGS
// -----------------------------------------------------------------------------

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Mathematics") }
    var durationText by remember { mutableStateOf("25") }

    val subjects = listOf("Mathematics", "History", "Computer Science", "English", "Science", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Study Task",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g., Read calculus Chapter 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_task_title")
                )

                // Subject Select list
                Column {
                    Text("Select Subject", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { sub ->
                            val isSelected = subject == sub
                            val color = getSubjectColor(sub)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                    .clickable { subject = sub }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sub,
                                    color = if (isSelected) Color.White else color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_task_duration")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 25
                            if (title.isNotBlank()) {
                                onConfirm(title, subject, duration)
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("dialog_task_confirm")
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int) -> Unit
) {
    var subject by remember { mutableStateOf("Mathematics") }
    var selectedDay by remember { mutableStateOf("Monday") }
    var hour by remember { mutableStateOf("10") }
    var minute by remember { mutableStateOf("00") }
    var durationText by remember { mutableStateOf("60") }

    val subjects = listOf("Mathematics", "History", "Computer Science", "English", "Science", "Other")
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Schedule Study Block",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Subject Select List
                Column {
                    Text("Subject", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { sub ->
                            val isSelected = subject == sub
                            val color = getSubjectColor(sub)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                    .clickable { subject = sub }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sub,
                                    color = if (isSelected) Color.White else color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Day Select List
                Column {
                    Text("Day of the week", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(days) { day ->
                            val isSelected = selectedDay == day
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedDay = day }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = day.take(3),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Start Time block
                Column {
                    Text("Start Time (24h format)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = hour,
                            onValueChange = { hour = it.take(2).filter { c -> c.isDigit() } },
                            label = { Text("Hour") },
                            placeholder = { Text("e.g. 14") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("dialog_schedule_hour")
                        )
                        OutlinedTextField(
                            value = minute,
                            onValueChange = { minute = it.take(2).filter { c -> c.isDigit() } },
                            label = { Text("Min") },
                            placeholder = { Text("e.g. 30") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("dialog_schedule_minute")
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_schedule_duration")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 60
                            val hr = hour.padStart(2, '0')
                            val min = minute.padStart(2, '0')
                            onConfirm(subject, selectedDay, "$hr:$min", duration)
                        },
                        modifier = Modifier.testTag("dialog_schedule_confirm")
                    ) {
                        Text("Schedule")
                    }
                }
            }
        }
    }
}

@Composable
fun ManualLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Mathematics") }
    var durationText by remember { mutableStateOf("45") }

    val subjects = listOf("Mathematics", "History", "Computer Science", "English", "Science", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Log Study Session Manually",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Study Activity / Topic") },
                    placeholder = { Text("e.g., Reviewed exam flashcards") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_log_title")
                )

                // Subject Select List
                Column {
                    Text("Subject", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { sub ->
                            val isSelected = subject == sub
                            val color = getSubjectColor(sub)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                    .clickable { subject = sub }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sub,
                                    color = if (isSelected) Color.White else color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_log_duration")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 45
                            if (title.isNotBlank()) {
                                onConfirm(title, subject, duration)
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("dialog_log_confirm")
                    ) {
                        Text("Log")
                    }
                }
            }
        }
    }
}
