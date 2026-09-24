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
    val defaultSets: Int = 3,
    val secondaryMuscles: String = "",
    val aliases: String = "",
    val imageUri: String = ""
)

data class ExerciseDraft(
    val id: Long? = null,
    val name: String,
    val category: String,
    val equipment: String,
    val trackingType: String = "weight_reps",
    val incrementKg: Double? = null,
    val favorite: Boolean = false,
    val diagramHint: String = "",
    val defaultSets: Int = 3,
    val notes: String = "",
    val secondaryMuscles: String = "",
    val aliases: String = "",
    val imageUri: String = ""
)

data class TemplateExercise(
    val exercise: Exercise,
    val orderIndex: Int,
    val defaultSets: Int,
    val targetMinReps: Int? = null,
    val targetMaxReps: Int? = null,
    val perSide: Boolean = false
) {
    val targetLabel: String get() {
        val reps = when {
            targetMinReps == null -> ""
            targetMaxReps == null || targetMaxReps == targetMinReps -> "$targetMinReps"
            else -> "$targetMinReps–$targetMaxReps"
        }
        return buildString {
            append(defaultSets).append(" × ").append(reps)
            if (perSide) append(" each side")
        }.trim()
    }
}
data class WorkoutTemplate(val id: Long, val name: String, val exercises: List<TemplateExercise>, val archived: Boolean = false)

data class ProgramTemplate(val template: WorkoutTemplate, val orderIndex: Int)
data class WorkoutProgram(val id: Long, val name: String, val templates: List<ProgramTemplate>, val archived: Boolean = false)
data class LibraryAsset(val id: Long, val name: String)
data class ImportPreview(val exercises: Int, val templates: Int, val programs: Int, val workouts: Int, val schemaVersion: Int)

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
data class PersonalBest(val weightKg: Double, val reps: Int)
data class PrStatus(val proposedWeight: Double, val proposedReps: Int, val best: PersonalBest?, val wouldBePr: Boolean)
data class ExerciseBest(val exerciseId: Long, val exerciseName: String, val category: String, val weightKg: Double, val reps: Int)
data class CompletionSummary(val name: String, val loggedSets: Int, val skippedSets: Int, val plannedSets: Int, val prCount: Int)
