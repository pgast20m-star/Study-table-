package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {
    val allTasks: Flow<List<StudyTask>> = studyDao.getAllTasks()
    val allSessions: Flow<List<StudySession>> = studyDao.getAllSessions()
    val allSchedules: Flow<List<StudySchedule>> = studyDao.getAllSchedules()

    suspend fun insertTask(task: StudyTask) {
        studyDao.insertTask(task)
    }

    suspend fun updateTask(task: StudyTask) {
        studyDao.updateTask(task)
    }

    suspend fun deleteTask(task: StudyTask) {
        studyDao.deleteTask(task)
    }

    suspend fun insertSession(session: StudySession) {
        studyDao.insertSession(session)
    }

    suspend fun deleteSession(session: StudySession) {
        studyDao.deleteSession(session)
    }

    suspend fun insertSchedule(schedule: StudySchedule) {
        studyDao.insertSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: StudySchedule) {
        studyDao.deleteSchedule(schedule)
    }
}
