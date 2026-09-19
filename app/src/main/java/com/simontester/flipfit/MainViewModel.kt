package com.simontester.flipfit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.simontester.flipfit.data.FlipFitDatabase
import com.simontester.flipfit.data.SettingsStore
import com.simontester.flipfit.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = FlipFitDatabase(app)
    val settings = SettingsStore(app)

    private val _templates = MutableStateFlow(db.getTemplates())
    val templates: StateFlow<List<WorkoutTemplate>> = _templates

    private val _exercises = MutableStateFlow(db.getAllExercises())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _active = MutableStateFlow(db.activeSession())
    val active: StateFlow<WorkoutSession?> = _active

    private val _history = MutableStateFlow(db.history())
    val history: StateFlow<List<WorkoutSession>> = _history

    private val _completion = MutableStateFlow<CompletionSummary?>(null)
    val completion: StateFlow<CompletionSummary?> = _completion

    private val _prFlash = MutableStateFlow(false)
    val prFlash: StateFlow<Boolean> = _prFlash

    fun start(template: WorkoutTemplate) {
        if (_active.value == null) db.startSession(template)
        refresh()
    }

    fun log(workoutExerciseId: Long, setNumber: Int, weight: Double, reps: Int) {
        val s = _active.value ?: return
        _prFlash.value = db.logSet(s.id, workoutExerciseId, setNumber, weight, reps)
        refresh()
    }

    fun skipSet(workoutExerciseId: Long, setNumber: Int) {
        val s = _active.value ?: return
        db.skipSet(s.id, workoutExerciseId, setNumber)
        _prFlash.value = false
        refresh()
    }

    fun skipExercise(workoutExerciseId: Long) {
        val s = _active.value ?: return
        db.skipExercise(s.id, workoutExerciseId)
        _prFlash.value = false
        refresh()
    }

    fun adjustSets(workoutExerciseId: Long, delta: Int) {
        db.adjustTargetSets(workoutExerciseId, delta)
        refresh()
    }

    fun jump(orderIndex: Int) {
        val s = _active.value ?: return
        db.jumpToExercise(s.id, orderIndex)
        _prFlash.value = false
        refresh()
    }

    fun addExercise(exerciseId: Long) {
        val s = _active.value ?: return
        db.addExerciseToSession(s.id, exerciseId)
        refresh()
    }

    fun replaceExercise(workoutExerciseId: Long, exerciseId: Long): Boolean {
        val replaced = db.replaceExercise(workoutExerciseId, exerciseId)
        refresh()
        return replaced
    }

    fun moveExercise(workoutExerciseId: Long, direction: Int) {
        val s = _active.value ?: return
        db.moveExercise(s.id, workoutExerciseId, direction)
        refresh()
    }

    fun undo() {
        val s = _active.value ?: return
        db.undoLastSet(s.id)
        _prFlash.value = false
        refresh()
    }

    fun finish() {
        val s = _active.value ?: return
        _completion.value = db.finishSession(s.id)
        _prFlash.value = false
        refresh()
    }

    fun previous(exerciseId: Long): List<PreviousSet> = _active.value?.let { db.previousSets(exerciseId, it.id) } ?: emptyList()
    fun prStatus(exerciseId: Long, weight: Double, reps: Int): PrStatus = db.prStatus(exerciseId, weight, reps)
    fun personalBests(): List<ExerciseBest> = db.personalBests()
    fun updateHistorySet(setId: Long, weight: Double, reps: Int) { db.updateHistorySet(setId, weight, reps); refresh() }
    fun deleteHistorySet(setId: Long) { db.deleteHistorySet(setId); refresh() }
    fun renameHistoryWorkout(sessionId: Long, name: String) { db.renameHistoryWorkout(sessionId, name); refresh() }
    fun deleteHistoryWorkout(sessionId: Long) { db.deleteHistoryWorkout(sessionId); refresh() }

    fun saveExercise(draft: ExerciseDraft) { db.saveExercise(draft); refresh() }
    fun duplicateExercise(exerciseId: Long) { db.duplicateExercise(exerciseId); refresh() }
    fun deleteExercise(exerciseId: Long) { db.deleteExercise(exerciseId); refresh() }

    fun createTemplate(name: String) { db.createTemplate(name); refresh() }
    fun renameTemplate(templateId: Long, name: String) { db.renameTemplate(templateId, name); refresh() }
    fun deleteTemplate(templateId: Long) { db.deleteTemplate(templateId); refresh() }
    fun duplicateTemplate(templateId: Long) { db.duplicateTemplate(templateId); refresh() }
    fun addTemplateExercise(templateId: Long, exerciseId: Long) { db.addTemplateExercise(templateId, exerciseId); refresh() }
    fun removeTemplateExercise(templateId: Long, exerciseId: Long) { db.removeTemplateExercise(templateId, exerciseId); refresh() }
    fun adjustTemplateSets(templateId: Long, exerciseId: Long, delta: Int) { db.adjustTemplateSets(templateId, exerciseId, delta); refresh() }
    fun moveTemplateExercise(templateId: Long, exerciseId: Long, direction: Int) { db.moveTemplateExercise(templateId, exerciseId, direction); refresh() }

    fun clearPrFlash() { _prFlash.value = false }
    fun clearCompletion() { _completion.value = null }

    private fun refresh() {
        _templates.value = db.getTemplates()
        _exercises.value = db.getAllExercises()
        _active.value = db.activeSession()
        _history.value = db.history()
    }
}
