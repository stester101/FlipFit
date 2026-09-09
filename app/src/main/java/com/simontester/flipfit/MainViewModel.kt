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
    private val _templates = MutableStateFlow(db.getTemplates()); val templates: StateFlow<List<WorkoutTemplate>> = _templates
    private val _active = MutableStateFlow(db.activeSession()); val active: StateFlow<WorkoutSession?> = _active
    private val _history = MutableStateFlow(db.history()); val history: StateFlow<List<WorkoutSession>> = _history
    private val _lastLoggedId = MutableStateFlow<Long?>(null); val lastLoggedId: StateFlow<Long?> = _lastLoggedId

    fun start(template: WorkoutTemplate) { db.startSession(template); refresh() }
    fun log(exerciseId: Long, setNumber: Int, weight: Double, reps: Int) { val s=_active.value ?: return; _lastLoggedId.value=db.logSet(s.id,exerciseId,setNumber,weight,reps); refresh() }
    fun undo() { _lastLoggedId.value?.let(db::deleteSet); _lastLoggedId.value=null; refresh() }
    fun finish() { _active.value?.let { db.finishSession(it.id) }; _lastLoggedId.value=null; refresh() }
    fun previous(exerciseId: Long): List<PreviousSet> = _active.value?.let { db.previousSets(exerciseId,it.id) } ?: emptyList()
    fun clearUndo() { _lastLoggedId.value=null }
    private fun refresh() { _active.value=db.activeSession(); _history.value=db.history() }
}
