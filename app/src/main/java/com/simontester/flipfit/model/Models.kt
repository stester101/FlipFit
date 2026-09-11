package com.simontester.flipfit.model

data class Exercise(
    val id: Long,
    val name: String,
    val category: String,
    val equipment: String,
    val notes: String = "",
    val trackingType: String = "weight_reps",
    val incrementKg: Double? = null,
    val favorite: Boolean = false,
    val diagramHint: String = "",
    val defaultSets: Int = 3
)

data class TemplateExercise(val exercise: Exercise, val orderIndex: Int, val defaultSets: Int)
data class WorkoutTemplate(val id: Long, val name: String, val exercises: List<TemplateExercise>)

data class LoggedSet(
    val id: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val skipped: Boolean = false,
    val isPr: Boolean = false
)

data class SessionExercise(
    val id: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    val targetSets: Int,
    val sets: List<LoggedSet>
)

data class WorkoutSession(
    val id: Long,
    val templateId: Long?,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val currentOrderIndex: Int,
    val exercises: List<SessionExercise>
)

data class PreviousSet(val setNumber: Int, val weightKg: Double, val reps: Int)

data class CompletionSummary(
    val name: String,
    val loggedSets: Int,
    val skippedSets: Int,
    val plannedSets: Int,
    val prCount: Int
)
