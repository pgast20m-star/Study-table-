package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.StudyRepository
import com.example.data.StudyTask
import com.example.data.StudySession
import com.example.data.StudySchedule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    // Database Flows
    val tasks: StateFlow<List<StudyTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<StudySchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Timer State
    val isTimerRunning = MutableStateFlow(false)
    val timerDurationMinutes = MutableStateFlow(25) // Default 25 min (Pomodoro)
    val timeLeftSeconds = MutableStateFlow(25 * 60)
    val selectedTaskForTimer = MutableStateFlow<StudyTask?>(null)

    private var timerJob: Job? = null

    init {
        // Populate default data if db is empty (Onboarding / Demo Experience)
        viewModelScope.launch {
            // Check tasks
            tasks.collect { currentTasks ->
                if (currentTasks.isEmpty()) {
                    preloadDefaultData()
                }
            }
        }
    }

    private fun preloadDefaultData() {
        viewModelScope.launch {
            // Add default tasks
            repository.insertTask(StudyTask(title = "Review Calculus Lecture Notes", subject = "Mathematics", targetMinutes = 50))
            repository.insertTask(StudyTask(title = "Read Chapter 4: French Revolution", subject = "History", targetMinutes = 25))
            repository.insertTask(StudyTask(title = "Practice Coding: Binary Search", subject = "Computer Science", targetMinutes = 45))
            
            // Add default schedules
            repository.insertSchedule(StudySchedule(subject = "Mathematics", dayOfWeek = "Monday", time = "10:00", durationMinutes = 90))
            repository.insertSchedule(StudySchedule(subject = "Computer Science", dayOfWeek = "Wednesday", time = "14:00", durationMinutes = 120))
            repository.insertSchedule(StudySchedule(subject = "History", dayOfWeek = "Friday", time = "11:30", durationMinutes = 60))

            // Add a couple of preloaded sessions for demonstration/analytics charts
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            repository.insertSession(StudySession(taskTitle = "Practice Coding", subject = "Computer Science", durationMinutes = 45, timestamp = now - dayMillis * 2))
            repository.insertSession(StudySession(taskTitle = "Review Calculus", subject = "Mathematics", durationMinutes = 50, timestamp = now - dayMillis))
            repository.insertSession(StudySession(taskTitle = "Read Chapter 4", subject = "History", durationMinutes = 25, timestamp = now))
        }
    }

    // Task Actions
    fun addTask(title: String, subject: String, targetMinutes: Int) {
        viewModelScope.launch {
            repository.insertTask(StudyTask(title = title, subject = subject, targetMinutes = targetMinutes))
        }
    }

    fun toggleTaskCompletion(task: StudyTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: StudyTask) {
        viewModelScope.launch {
            if (selectedTaskForTimer.value?.id == task.id) {
                selectedTaskForTimer.value = null
            }
            repository.deleteTask(task)
        }
    }

    // Schedule Actions
    fun addSchedule(subject: String, dayOfWeek: String, time: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.insertSchedule(
                StudySchedule(
                    subject = subject,
                    dayOfWeek = dayOfWeek,
                    time = time,
                    durationMinutes = durationMinutes
                )
            )
        }
    }

    fun deleteSchedule(schedule: StudySchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    // Session Actions
    fun logManualSession(taskTitle: String, subject: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.insertSession(
                StudySession(
                    taskTitle = taskTitle,
                    subject = subject,
                    durationMinutes = durationMinutes
                )
            )
        }
    }

    fun deleteSession(session: StudySession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    // Timer Logic
    fun startTimer() {
        if (isTimerRunning.value) return
        isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (timeLeftSeconds.value > 0 && isTimerRunning.value) {
                delay(1000)
                timeLeftSeconds.value -= 1
            }
            if (timeLeftSeconds.value == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        timeLeftSeconds.value = timerDurationMinutes.value * 60
    }

    fun updateTimerDuration(minutes: Int) {
        pauseTimer()
        timerDurationMinutes.value = minutes
        timeLeftSeconds.value = minutes * 60
    }

    fun selectTaskForTimer(task: StudyTask?) {
        selectedTaskForTimer.value = task
        if (task != null) {
            updateTimerDuration(task.targetMinutes)
        }
    }

    private fun onTimerFinished() {
        isTimerRunning.value = false
        timerJob?.cancel()
        
        // Log the completed session
        viewModelScope.launch {
            val task = selectedTaskForTimer.value
            val taskTitle = task?.title ?: "Focus Session"
            val subject = task?.subject ?: "General Study"
            
            repository.insertSession(
                StudySession(
                    taskTitle = taskTitle,
                    subject = subject,
                    durationMinutes = timerDurationMinutes.value
                )
            )
            
            // Mark task as completed if focusing on a specific task
            if (task != null) {
                repository.updateTask(task.copy(isCompleted = true))
                selectedTaskForTimer.value = null
            }
            
            // Reset to default duration
            resetTimer()
        }
    }
}

class StudyViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
