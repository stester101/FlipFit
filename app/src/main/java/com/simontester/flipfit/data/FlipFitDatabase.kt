package com.simontester.flipfit.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.simontester.flipfit.model.*
import kotlin.math.max

class FlipFitDatabase(context: Context) : SQLiteOpenHelper(context, "flipfit.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE exercise(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,category TEXT NOT NULL,equipment TEXT NOT NULL,notes TEXT NOT NULL DEFAULT '',tracking_type TEXT NOT NULL DEFAULT 'weight_reps',increment_kg REAL,favorite INTEGER NOT NULL DEFAULT 0,diagram_hint TEXT NOT NULL DEFAULT '',default_sets INTEGER NOT NULL DEFAULT 3,archived INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE workout_template(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE template_exercise(id INTEGER PRIMARY KEY AUTOINCREMENT,template_id INTEGER NOT NULL,exercise_id INTEGER NOT NULL,order_index INTEGER NOT NULL,default_sets INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE workout_session(id INTEGER PRIMARY KEY AUTOINCREMENT,template_id INTEGER,name TEXT NOT NULL,started_at INTEGER NOT NULL,ended_at INTEGER,current_order_index INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE workout_exercise(id INTEGER PRIMARY KEY AUTOINCREMENT,session_id INTEGER NOT NULL,exercise_id INTEGER NOT NULL,order_index INTEGER NOT NULL,target_sets INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE workout_set(id INTEGER PRIMARY KEY AUTOINCREMENT,workout_exercise_id INTEGER NOT NULL,set_number INTEGER NOT NULL,weight_kg REAL NOT NULL,reps INTEGER NOT NULL,logged_at INTEGER NOT NULL,skipped INTEGER NOT NULL DEFAULT 0,is_pr INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE UNIQUE INDEX idx_workout_set_unique ON workout_set(workout_exercise_id,set_number)")
        ensureCatalog(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE exercise ADD COLUMN tracking_type TEXT NOT NULL DEFAULT 'weight_reps'")
            db.execSQL("ALTER TABLE exercise ADD COLUMN increment_kg REAL")
            db.execSQL("ALTER TABLE exercise ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE exercise ADD COLUMN diagram_hint TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE exercise ADD COLUMN default_sets INTEGER NOT NULL DEFAULT 3")
            db.execSQL("ALTER TABLE workout_session ADD COLUMN current_order_index INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_set ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_set ADD COLUMN is_pr INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_workout_set_exercise_set ON workout_set(workout_exercise_id,set_number)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE exercise ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        }
        ensureCatalog(db)
    }

    private data class ExerciseSeed(
        val name: String,
        val category: String,
        val equipment: String,
        val trackingType: String = "weight_reps",
        val diagram: String = ""
    )

    private fun ensureCatalog(db: SQLiteDatabase) {
        val seeds = listOf(
            ExerciseSeed("Dumbbell Bench Press","Chest","Dumbbell",diagram="Start: dumbbells above chest, elbows softly bent. Finish: lower under control until elbows are just below the bench, then press back up."),
            ExerciseSeed("Incline Dumbbell Press","Chest","Dumbbell",diagram="Start: incline bench, dumbbells above upper chest. Finish: lower beside upper chest, then press up and slightly inward."),
            ExerciseSeed("Barbell Bench Press","Chest","Barbell"),
            ExerciseSeed("Machine Chest Press","Chest","Machine"),
            ExerciseSeed("Cable Fly","Chest","Cable"),
            ExerciseSeed("Push-Up","Chest","Bodyweight","reps_only"),
            ExerciseSeed("Lat Pulldown","Back","Cable"),
            ExerciseSeed("Seated Cable Row","Back","Cable"),
            ExerciseSeed("Bent-Over Row","Back","Barbell"),
            ExerciseSeed("One-Arm Dumbbell Row","Back","Dumbbell"),
            ExerciseSeed("Chest-Supported Dumbbell Row","Back","Dumbbell"),
            ExerciseSeed("Shoulder Press","Shoulders","Barbell"),
            ExerciseSeed("Dumbbell Shoulder Press","Shoulders","Dumbbell"),
            ExerciseSeed("Lateral Raise","Shoulders","Dumbbell"),
            ExerciseSeed("Front Raise","Shoulders","Dumbbell"),
            ExerciseSeed("Rear Delt Fly","Shoulders","Dumbbell"),
            ExerciseSeed("Dumbbell Curl","Biceps","Dumbbell",diagram="Start: arms long at your sides, palms forward. Finish: curl without swinging until forearms approach biceps, then lower slowly."),
            ExerciseSeed("Hammer Curl","Biceps","Dumbbell",diagram="Start: arms long, palms facing inward. Finish: curl with neutral grip while keeping elbows close to your sides."),
            ExerciseSeed("Barbell Curl","Biceps","Barbell"),
            ExerciseSeed("Incline Dumbbell Curl","Biceps","Dumbbell"),
            ExerciseSeed("Cable Curl","Biceps","Cable"),
            ExerciseSeed("Dumbbell Tricep Extension","Triceps","Dumbbell",diagram="Start: dumbbell overhead with elbows pointing forward. Finish: lower behind head, then extend elbows without flaring them wide."),
            ExerciseSeed("Tricep Pushdown","Triceps","Cable"),
            ExerciseSeed("Skull Crusher","Triceps","Barbell"),
            ExerciseSeed("Close-Grip Bench Press","Triceps","Barbell"),
            ExerciseSeed("Bench Dips","Triceps","Bodyweight","reps_only"),
            ExerciseSeed("Goblet Squat","Legs","Dumbbell"),
            ExerciseSeed("Barbell Squat","Legs","Barbell"),
            ExerciseSeed("Leg Press","Legs","Machine"),
            ExerciseSeed("Romanian Deadlift","Legs","Barbell"),
            ExerciseSeed("Dumbbell Romanian Deadlift","Legs","Dumbbell"),
            ExerciseSeed("Leg Curl","Legs","Machine"),
            ExerciseSeed("Leg Extension","Legs","Machine"),
            ExerciseSeed("Walking Lunge","Legs","Dumbbell"),
            ExerciseSeed("Calf Raise","Legs","Machine"),
            ExerciseSeed("Crunches","Core","Bodyweight","reps_only"),
            ExerciseSeed("Sit-Ups","Core","Bodyweight","reps_only"),
            ExerciseSeed("Russian Twists","Core","Bodyweight","reps_only"),
            ExerciseSeed("Hanging Knee Raise","Core","Bodyweight","reps_only")
        )
        seeds.forEach { seed ->
            val exists = db.rawQuery("SELECT id FROM exercise WHERE lower(name)=lower(?) LIMIT 1", arrayOf(seed.name)).use { it.moveToFirst() }
            if (!exists) {
                db.insert("exercise", null, ContentValues().apply {
                    put("name", seed.name); put("category", seed.category); put("equipment", seed.equipment)
                    put("tracking_type", seed.trackingType); put("diagram_hint", seed.diagram); put("default_sets", 3); put("archived", 0)
                })
            } else if (seed.diagram.isNotBlank()) {
                db.execSQL("UPDATE exercise SET diagram_hint=? WHERE lower(name)=lower(?) AND diagram_hint=''", arrayOf(seed.diagram, seed.name))
            }
        }

        ensureStarterTemplate(db, "CHEST + ARMS", listOf("Dumbbell Bench Press","Incline Dumbbell Press","Dumbbell Curl","Hammer Curl","Dumbbell Tricep Extension"))
        ensureStarterTemplate(db, "PUSH", listOf("Dumbbell Bench Press","Incline Dumbbell Press","Dumbbell Shoulder Press","Lateral Raise","Tricep Pushdown"))
        ensureStarterTemplate(db, "PULL", listOf("Lat Pulldown","Seated Cable Row","One-Arm Dumbbell Row","Dumbbell Curl","Hammer Curl"))
        ensureStarterTemplate(db, "LEGS", listOf("Goblet Squat","Romanian Deadlift","Leg Press","Leg Curl","Calf Raise"))
        ensureStarterTemplate(db, "UPPER BODY", listOf("Dumbbell Bench Press","Lat Pulldown","Dumbbell Shoulder Press","Seated Cable Row","Dumbbell Curl","Tricep Pushdown"))
        ensureStarterTemplate(db, "FULL BODY", listOf("Goblet Squat","Dumbbell Bench Press","One-Arm Dumbbell Row","Dumbbell Shoulder Press","Romanian Deadlift"))
    }

    private fun ensureStarterTemplate(db: SQLiteDatabase, name: String, exerciseNames: List<String>) {
        // Starter templates are seeded once. A tombstone keeps a deliberately deleted
        // starter from being silently recreated on the next app/database refresh.
        db.execSQL("CREATE TABLE IF NOT EXISTS deleted_starter_template(name TEXT PRIMARY KEY COLLATE NOCASE)")
        val deleted = db.rawQuery("SELECT 1 FROM deleted_starter_template WHERE lower(name)=lower(?) LIMIT 1", arrayOf(name)).use { it.moveToFirst() }
        if (deleted) return
        val exists = db.rawQuery("SELECT id FROM workout_template WHERE lower(name)=lower(?) LIMIT 1", arrayOf(name)).use { it.moveToFirst() }
        if (exists) return
        val templateId = db.insert("workout_template", null, ContentValues().apply { put("name", name) })
        exerciseNames.forEachIndexed { index, exerciseName ->
            val exerciseId = db.rawQuery("SELECT id FROM exercise WHERE lower(name)=lower(?) LIMIT 1", arrayOf(exerciseName)).use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }
            if (exerciseId > 0) db.insert("template_exercise", null, ContentValues().apply {
                put("template_id", templateId); put("exercise_id", exerciseId); put("order_index", index); put("default_sets", 3)
            })
        }
    }

    private fun exerciseFromCursor(c: Cursor, offset: Int = 0): Exercise = Exercise(
        id = c.getLong(offset), name = c.getString(offset + 1), category = c.getString(offset + 2),
        equipment = c.getString(offset + 3), notes = c.getString(offset + 4), trackingType = c.getString(offset + 5),
        incrementKg = if (c.isNull(offset + 6)) null else c.getDouble(offset + 6),
        favorite = c.getInt(offset + 7) != 0, diagramHint = c.getString(offset + 8), defaultSets = c.getInt(offset + 9)
    )

    fun getAllExercises(): List<Exercise> {
        val db = writableDatabase
        ensureCatalog(db)
        val out = mutableListOf<Exercise>()
        db.rawQuery("SELECT id,name,category,equipment,notes,tracking_type,increment_kg,favorite,diagram_hint,default_sets FROM exercise WHERE archived=0 ORDER BY favorite DESC, category, name", null).use { c ->
            while (c.moveToNext()) out += exerciseFromCursor(c)
        }
        return out
    }

    fun getTemplates(): List<WorkoutTemplate> {
        val db = writableDatabase
        ensureCatalog(db)
        val result = mutableListOf<WorkoutTemplate>()
        db.rawQuery("SELECT id,name FROM workout_template ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0); val name = c.getString(1)
                val items = mutableListOf<TemplateExercise>()
                db.rawQuery("SELECT e.id,e.name,e.category,e.equipment,e.notes,e.tracking_type,e.increment_kg,e.favorite,e.diagram_hint,e.default_sets,te.order_index,te.default_sets FROM template_exercise te JOIN exercise e ON e.id=te.exercise_id WHERE te.template_id=? AND e.archived=0 ORDER BY te.order_index", arrayOf(id.toString())).use { ec ->
                    while (ec.moveToNext()) items += TemplateExercise(exerciseFromCursor(ec), ec.getInt(10), ec.getInt(11))
                }
                result += WorkoutTemplate(id, name, items)
            }
        }
        return result
    }

    fun saveExercise(draft: ExerciseDraft): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", draft.name); put("category", draft.category); put("equipment", draft.equipment); put("notes", draft.notes)
            put("tracking_type", draft.trackingType); if (draft.incrementKg == null) putNull("increment_kg") else put("increment_kg", draft.incrementKg)
            put("favorite", if (draft.favorite) 1 else 0); put("diagram_hint", draft.diagramHint); put("default_sets", draft.defaultSets); put("archived", 0)
        }
        return if (draft.id == null) db.insert("exercise", null, values) else {
            db.update("exercise", values, "id=?", arrayOf(draft.id.toString())); draft.id
        }
    }

    fun duplicateExercise(exerciseId: Long): Long {
        val ex = getAllExercises().firstOrNull { it.id == exerciseId } ?: return -1L
        return saveExercise(ExerciseDraft(
            name = "${ex.name} Copy", category = ex.category, equipment = ex.equipment, trackingType = ex.trackingType,
            incrementKg = ex.incrementKg, favorite = false, diagramHint = ex.diagramHint, defaultSets = ex.defaultSets, notes = ex.notes
        ))
    }

    fun deleteExercise(exerciseId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("template_exercise", "exercise_id=?", arrayOf(exerciseId.toString()))
            db.update("exercise", ContentValues().apply { put("archived", 1); put("favorite", 0) }, "id=?", arrayOf(exerciseId.toString()))
            normalizeTemplateOrders(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun normalizeTemplateOrders(db: SQLiteDatabase) {
        val templateIds = mutableListOf<Long>()
        db.rawQuery("SELECT id FROM workout_template", null).use { c -> while (c.moveToNext()) templateIds += c.getLong(0) }
        templateIds.forEach { templateId ->
            val rows = mutableListOf<Long>()
            db.rawQuery("SELECT id FROM template_exercise WHERE template_id=? ORDER BY order_index,id", arrayOf(templateId.toString())).use { c -> while (c.moveToNext()) rows += c.getLong(0) }
            rows.forEachIndexed { index, rowId -> db.update("template_exercise", ContentValues().apply { put("order_index", index) }, "id=?", arrayOf(rowId.toString())) }
        }
    }

    fun createTemplate(name: String): Long = writableDatabase.insert("workout_template", null, ContentValues().apply { put("name", name) })

    fun renameTemplate(templateId: Long, name: String) {
        writableDatabase.update("workout_template", ContentValues().apply { put("name", name) }, "id=?", arrayOf(templateId.toString()))
    }

    fun deleteTemplate(templateId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_starter_template(name TEXT PRIMARY KEY COLLATE NOCASE)")
            val name = db.rawQuery("SELECT name FROM workout_template WHERE id=? LIMIT 1", arrayOf(templateId.toString())).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (name != null) db.insertWithOnConflict(
                "deleted_starter_template", null,
                ContentValues().apply { put("name", name) },
                SQLiteDatabase.CONFLICT_REPLACE
            )
            db.delete("template_exercise", "template_id=?", arrayOf(templateId.toString()))
            db.delete("workout_template", "id=?", arrayOf(templateId.toString()))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun updateHistorySet(setId: Long, weightKg: Double, reps: Int) {
        val db = writableDatabase
        db.update("workout_set", ContentValues().apply {
            put("weight_kg", weightKg.coerceAtLeast(0.0))
            put("reps", reps.coerceAtLeast(0))
            put("skipped", 0)
            put("is_pr", 0)
        }, "id=?", arrayOf(setId.toString()))
        recalculatePrFlags(db)
    }

    fun deleteHistorySet(setId: Long) {
        val db = writableDatabase
        db.delete("workout_set", "id=?", arrayOf(setId.toString()))
        recalculatePrFlags(db)
    }

    fun renameHistoryWorkout(sessionId: Long, name: String) {
        if (name.isBlank()) return
        writableDatabase.update("workout_session", ContentValues().apply { put("name", name.trim()) }, "id=?", arrayOf(sessionId.toString()))
    }

    fun deleteHistoryWorkout(sessionId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val exerciseRows = mutableListOf<Long>()
            db.rawQuery("SELECT id FROM workout_exercise WHERE session_id=?", arrayOf(sessionId.toString())).use { cursor ->
                while (cursor.moveToNext()) exerciseRows += cursor.getLong(0)
            }
            exerciseRows.forEach { id -> db.delete("workout_set", "workout_exercise_id=?", arrayOf(id.toString())) }
            db.delete("workout_exercise", "session_id=?", arrayOf(sessionId.toString()))
            db.delete("workout_session", "id=?", arrayOf(sessionId.toString()))
            recalculatePrFlags(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun recalculatePrFlags(db: SQLiteDatabase) {
        db.execSQL("UPDATE workout_set SET is_pr=0")
        val exerciseIds = mutableListOf<Long>()
        db.rawQuery("SELECT DISTINCT exercise_id FROM workout_exercise", null).use { cursor ->
            while (cursor.moveToNext()) exerciseIds += cursor.getLong(0)
        }
        exerciseIds.forEach { exerciseId ->
            var bestWeight = -1.0
            var bestReps = -1
            db.rawQuery(
                "SELECT s.id,s.weight_kg,s.reps FROM workout_set s JOIN workout_exercise we ON we.id=s.workout_exercise_id JOIN workout_session ws ON ws.id=we.session_id WHERE we.exercise_id=? AND s.skipped=0 AND ws.ended_at IS NOT NULL ORDER BY s.logged_at,s.id",
                arrayOf(exerciseId.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0); val weight = cursor.getDouble(1); val reps = cursor.getInt(2)
                    if (weight > bestWeight || (weight == bestWeight && reps > bestReps)) {
                        db.update("workout_set", ContentValues().apply { put("is_pr", 1) }, "id=?", arrayOf(id.toString()))
                        bestWeight = weight; bestReps = reps
                    }
                }
            }
        }
    }

    fun duplicateTemplate(templateId: Long): Long {
        val source = getTemplates().firstOrNull { it.id == templateId } ?: return -1L
        val db = writableDatabase
        val newId = db.insert("workout_template", null, ContentValues().apply { put("name", "${source.name} Copy") })
        source.exercises.forEach { te ->
            db.insert("template_exercise", null, ContentValues().apply {
                put("template_id", newId); put("exercise_id", te.exercise.id); put("order_index", te.orderIndex); put("default_sets", te.defaultSets)
            })
        }
        return newId
    }

    fun addTemplateExercise(templateId: Long, exerciseId: Long) {
        val db = writableDatabase
        val order = db.rawQuery("SELECT COALESCE(MAX(order_index),-1)+1 FROM template_exercise WHERE template_id=?", arrayOf(templateId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
        val sets = db.rawQuery("SELECT default_sets FROM exercise WHERE id=?", arrayOf(exerciseId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else 3 }
        db.insert("template_exercise", null, ContentValues().apply {
            put("template_id", templateId); put("exercise_id", exerciseId); put("order_index", order); put("default_sets", sets)
        })
    }

    fun removeTemplateExercise(templateId: Long, exerciseId: Long) {
        val db = writableDatabase
        val row = db.rawQuery("SELECT id FROM template_exercise WHERE template_id=? AND exercise_id=? ORDER BY order_index LIMIT 1", arrayOf(templateId.toString(), exerciseId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) else return }
        db.delete("template_exercise", "id=?", arrayOf(row.toString()))
        normalizeTemplateOrders(db)
    }

    fun adjustTemplateSets(templateId: Long, exerciseId: Long, delta: Int) {
        val db = writableDatabase
        val row = db.rawQuery("SELECT id,default_sets FROM template_exercise WHERE template_id=? AND exercise_id=? ORDER BY order_index LIMIT 1", arrayOf(templateId.toString(), exerciseId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getInt(1) else return }
        db.update("template_exercise", ContentValues().apply { put("default_sets", max(1, row.second + delta)) }, "id=?", arrayOf(row.first.toString()))
    }

    fun moveTemplateExercise(templateId: Long, exerciseId: Long, direction: Int) {
        if (direction == 0) return
        val db = writableDatabase
        val current = db.rawQuery("SELECT id,order_index FROM template_exercise WHERE template_id=? AND exercise_id=? ORDER BY order_index LIMIT 1", arrayOf(templateId.toString(), exerciseId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getInt(1) else return }
        val other = db.rawQuery(
            "SELECT id,order_index FROM template_exercise WHERE template_id=? AND order_index${if (direction < 0) "<" else ">"}? ORDER BY order_index ${if (direction < 0) "DESC" else "ASC"} LIMIT 1",
            arrayOf(templateId.toString(), current.second.toString())
        ).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getInt(1) else null } ?: return
        db.beginTransaction()
        try {
            db.update("template_exercise", ContentValues().apply { put("order_index", other.second) }, "id=?", arrayOf(current.first.toString()))
            db.update("template_exercise", ContentValues().apply { put("order_index", current.second) }, "id=?", arrayOf(other.first.toString()))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun startSession(template: WorkoutTemplate): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val sessionId = db.insert("workout_session", null, ContentValues().apply {
                put("template_id", template.id); put("name", template.name); put("started_at", System.currentTimeMillis()); put("current_order_index", 0)
            })
            template.exercises.forEach { te -> db.insert("workout_exercise", null, ContentValues().apply {
                put("session_id", sessionId); put("exercise_id", te.exercise.id); put("order_index", te.orderIndex); put("target_sets", te.defaultSets)
            }) }
            db.setTransactionSuccessful()
            return sessionId
        } finally { db.endTransaction() }
    }

    fun activeSession(): WorkoutSession? = loadSessions("WHERE ws.ended_at IS NULL ORDER BY ws.started_at DESC LIMIT 1").firstOrNull()
    fun history(): List<WorkoutSession> = loadSessions("WHERE ws.ended_at IS NOT NULL ORDER BY ws.ended_at DESC LIMIT 100")

    private fun loadSessions(where: String): List<WorkoutSession> {
        val db = readableDatabase; val out = mutableListOf<WorkoutSession>()
        db.rawQuery("SELECT ws.id,ws.template_id,ws.name,ws.started_at,ws.ended_at,ws.current_order_index FROM workout_session ws $where", null).use { sc ->
            while (sc.moveToNext()) {
                val sid = sc.getLong(0); val templateId = if (sc.isNull(1)) null else sc.getLong(1)
                val name = sc.getString(2); val started = sc.getLong(3); val ended = if (sc.isNull(4)) null else sc.getLong(4); val currentOrder = sc.getInt(5)
                val exs = mutableListOf<SessionExercise>()
                db.rawQuery("SELECT we.id,e.id,e.name,e.category,e.equipment,e.notes,e.tracking_type,e.increment_kg,e.favorite,e.diagram_hint,e.default_sets,we.order_index,we.target_sets FROM workout_exercise we JOIN exercise e ON e.id=we.exercise_id WHERE we.session_id=? ORDER BY we.order_index", arrayOf(sid.toString())).use { ec ->
                    while (ec.moveToNext()) {
                        val weId = ec.getLong(0); val exercise = exerciseFromCursor(ec, 1); val sets = mutableListOf<LoggedSet>()
                        db.rawQuery("SELECT id,set_number,weight_kg,reps,skipped,is_pr FROM workout_set WHERE workout_exercise_id=? ORDER BY set_number", arrayOf(weId.toString())).use { c ->
                            while (c.moveToNext()) sets += LoggedSet(c.getLong(0), exercise.id, c.getInt(1), c.getDouble(2), c.getInt(3), c.getInt(4) != 0, c.getInt(5) != 0)
                        }
                        exs += SessionExercise(weId, exercise, ec.getInt(11), ec.getInt(12), sets)
                    }
                }
                out += WorkoutSession(sid, templateId, name, started, ended, currentOrder, exs)
            }
        }
        return out
    }

    fun logSet(sessionId: Long, workoutExerciseId: Long, setNumber: Int, weightKg: Double, reps: Int): Boolean {
        val db = writableDatabase
        val exists = db.rawQuery("SELECT id FROM workout_set WHERE workout_exercise_id=? AND set_number=? LIMIT 1", arrayOf(workoutExerciseId.toString(), setNumber.toString())).use { it.moveToFirst() }
        if (exists) return false
        val exerciseId = db.rawQuery("SELECT exercise_id FROM workout_exercise WHERE id=?", arrayOf(workoutExerciseId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) else return false }
        val isPr = isPersonalBest(db, exerciseId, weightKg, reps)
        insertSetInternal(db, workoutExerciseId, setNumber, weightKg, reps, skipped = false, isPr = isPr)
        advanceIfComplete(db, sessionId, workoutExerciseId)
        return isPr
    }

    private fun bestForExercise(db: SQLiteDatabase, exerciseId: Long): PersonalBest? =
        db.rawQuery("SELECT s.weight_kg,s.reps FROM workout_set s JOIN workout_exercise we ON we.id=s.workout_exercise_id WHERE we.exercise_id=? AND s.skipped=0 ORDER BY s.weight_kg DESC,s.reps DESC LIMIT 1", arrayOf(exerciseId.toString())).use { c ->
            if (c.moveToFirst()) PersonalBest(c.getDouble(0), c.getInt(1)) else null
        }

    private fun isPersonalBest(db: SQLiteDatabase, exerciseId: Long, weightKg: Double, reps: Int): Boolean {
        val best = bestForExercise(db, exerciseId) ?: return true
        return weightKg > best.weightKg || (weightKg == best.weightKg && reps > best.reps)
    }

    fun prStatus(exerciseId: Long, weightKg: Double, reps: Int): PrStatus {
        val best = bestForExercise(readableDatabase, exerciseId)
        val isPr = best == null || weightKg > best.weightKg || (weightKg == best.weightKg && reps > best.reps)
        return PrStatus(weightKg, reps, best, isPr)
    }

    fun personalBests(): List<ExerciseBest> {
        val db = readableDatabase
        val out = mutableListOf<ExerciseBest>()
        db.rawQuery("SELECT e.id,e.name,e.category,s.weight_kg,s.reps FROM exercise e JOIN workout_exercise we ON we.exercise_id=e.id JOIN workout_set s ON s.workout_exercise_id=we.id WHERE s.skipped=0 AND e.tracking_type='weight_reps' AND s.id=(SELECT s2.id FROM workout_set s2 JOIN workout_exercise we2 ON we2.id=s2.workout_exercise_id WHERE we2.exercise_id=e.id AND s2.skipped=0 ORDER BY s2.weight_kg DESC,s2.reps DESC,s2.id DESC LIMIT 1) ORDER BY e.category,e.name", null).use { c ->
            while (c.moveToNext()) out += ExerciseBest(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getInt(4))
        }
        return out
    }

    private fun insertSetInternal(db: SQLiteDatabase, workoutExerciseId: Long, setNumber: Int, weightKg: Double, reps: Int, skipped: Boolean, isPr: Boolean): Long =
        db.insert("workout_set", null, ContentValues().apply {
            put("workout_exercise_id", workoutExerciseId); put("set_number", setNumber); put("weight_kg", weightKg); put("reps", reps)
            put("logged_at", System.currentTimeMillis()); put("skipped", if (skipped) 1 else 0); put("is_pr", if (isPr) 1 else 0)
        })

    fun skipSet(sessionId: Long, workoutExerciseId: Long, setNumber: Int) {
        val db = writableDatabase
        val exists = db.rawQuery("SELECT id FROM workout_set WHERE workout_exercise_id=? AND set_number=? LIMIT 1", arrayOf(workoutExerciseId.toString(), setNumber.toString())).use { it.moveToFirst() }
        if (!exists) insertSetInternal(db, workoutExerciseId, setNumber, 0.0, 0, skipped = true, isPr = false)
        advanceIfComplete(db, sessionId, workoutExerciseId)
    }

    fun skipExercise(sessionId: Long, workoutExerciseId: Long) {
        val db = writableDatabase
        val target = db.rawQuery("SELECT target_sets FROM workout_exercise WHERE id=?", arrayOf(workoutExerciseId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else return }
        val existing = mutableSetOf<Int>()
        db.rawQuery("SELECT set_number FROM workout_set WHERE workout_exercise_id=?", arrayOf(workoutExerciseId.toString())).use { c -> while (c.moveToNext()) existing += c.getInt(0) }
        (1..target).filterNot { it in existing }.forEach { insertSetInternal(db, workoutExerciseId, it, 0.0, 0, skipped = true, isPr = false) }
        advanceIfComplete(db, sessionId, workoutExerciseId)
    }

    private fun advanceIfComplete(db: SQLiteDatabase, sessionId: Long, workoutExerciseId: Long) {
        val info = db.rawQuery("SELECT order_index,target_sets FROM workout_exercise WHERE id=?", arrayOf(workoutExerciseId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) to c.getInt(1) else return }
        val count = db.rawQuery("SELECT COUNT(*) FROM workout_set WHERE workout_exercise_id=?", arrayOf(workoutExerciseId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
        if (count < info.second) return
        val nextOrder = db.rawQuery("SELECT we.order_index FROM workout_exercise we WHERE we.session_id=? AND we.order_index>? AND (SELECT COUNT(*) FROM workout_set s WHERE s.workout_exercise_id=we.id)<we.target_sets ORDER BY we.order_index LIMIT 1", arrayOf(sessionId.toString(), info.first.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else null }
            ?: db.rawQuery("SELECT we.order_index FROM workout_exercise we WHERE we.session_id=? AND (SELECT COUNT(*) FROM workout_set s WHERE s.workout_exercise_id=we.id)<we.target_sets ORDER BY we.order_index LIMIT 1", arrayOf(sessionId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else info.first }
        setCurrentExercise(db, sessionId, nextOrder)
    }

    fun jumpToExercise(sessionId: Long, orderIndex: Int) = setCurrentExercise(writableDatabase, sessionId, orderIndex)

    private fun setCurrentExercise(db: SQLiteDatabase, sessionId: Long, orderIndex: Int) {
        db.update("workout_session", ContentValues().apply { put("current_order_index", orderIndex) }, "id=?", arrayOf(sessionId.toString()))
    }

    fun adjustTargetSets(workoutExerciseId: Long, delta: Int) {
        val db = writableDatabase
        val target = db.rawQuery("SELECT target_sets FROM workout_exercise WHERE id=?", arrayOf(workoutExerciseId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else return }
        val maxAccounted = db.rawQuery("SELECT COALESCE(MAX(set_number),0) FROM workout_set WHERE workout_exercise_id=?", arrayOf(workoutExerciseId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
        val updated = max(max(1, maxAccounted), target + delta)
        db.update("workout_exercise", ContentValues().apply { put("target_sets", updated) }, "id=?", arrayOf(workoutExerciseId.toString()))
    }

    fun addExerciseToSession(sessionId: Long, exerciseId: Long) {
        val db = writableDatabase
        val info = db.rawQuery("SELECT default_sets FROM exercise WHERE id=?", arrayOf(exerciseId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else return }
        val order = db.rawQuery("SELECT COALESCE(MAX(order_index),-1)+1 FROM workout_exercise WHERE session_id=?", arrayOf(sessionId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
        db.insert("workout_exercise", null, ContentValues().apply { put("session_id", sessionId); put("exercise_id", exerciseId); put("order_index", order); put("target_sets", info) })
    }

    fun replaceExercise(workoutExerciseId: Long, exerciseId: Long): Boolean {
        val db = writableDatabase
        val count = db.rawQuery("SELECT COUNT(*) FROM workout_set WHERE workout_exercise_id=?", arrayOf(workoutExerciseId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
        if (count > 0) return false
        db.update("workout_exercise", ContentValues().apply { put("exercise_id", exerciseId) }, "id=?", arrayOf(workoutExerciseId.toString()))
        return true
    }

    fun moveExercise(sessionId: Long, workoutExerciseId: Long, direction: Int) {
        if (direction == 0) return
        val db = writableDatabase
        val order = db.rawQuery("SELECT order_index FROM workout_exercise WHERE id=?", arrayOf(workoutExerciseId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else return }
        val other = db.rawQuery("SELECT id,order_index FROM workout_exercise WHERE session_id=? AND order_index${if (direction < 0) "<" else ">"}? ORDER BY order_index ${if (direction < 0) "DESC" else "ASC"} LIMIT 1", arrayOf(sessionId.toString(), order.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getInt(1) else null } ?: return
        db.beginTransaction()
        try {
            db.update("workout_exercise", ContentValues().apply { put("order_index", other.second) }, "id=?", arrayOf(workoutExerciseId.toString()))
            db.update("workout_exercise", ContentValues().apply { put("order_index", order) }, "id=?", arrayOf(other.first.toString()))
            setCurrentExercise(db, sessionId, other.second)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun undoLastSet(sessionId: Long) {
        val db = writableDatabase
        val row = db.rawQuery("SELECT s.id,we.order_index FROM workout_set s JOIN workout_exercise we ON we.id=s.workout_exercise_id WHERE we.session_id=? ORDER BY s.logged_at DESC,s.id DESC LIMIT 1", arrayOf(sessionId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getInt(1) else null } ?: return
        db.delete("workout_set", "id=?", arrayOf(row.first.toString()))
        setCurrentExercise(db, sessionId, row.second)
    }

    fun finishSession(sessionId: Long): CompletionSummary? {
        val session = loadSessions("WHERE ws.id=$sessionId LIMIT 1").firstOrNull() ?: return null
        val allSets = session.exercises.flatMap { it.sets }
        if (allSets.isEmpty()) {
            val db = writableDatabase
            db.delete("workout_exercise", "session_id=?", arrayOf(sessionId.toString()))
            db.delete("workout_session", "id=?", arrayOf(sessionId.toString()))
            return null
        }
        writableDatabase.update("workout_session", ContentValues().apply { put("ended_at", System.currentTimeMillis()) }, "id=?", arrayOf(sessionId.toString()))
        return CompletionSummary(
            name = session.name,
            loggedSets = allSets.count { !it.skipped },
            skippedSets = allSets.count { it.skipped },
            plannedSets = session.exercises.sumOf { it.targetSets },
            prCount = allSets.count { it.isPr }
        )
    }

    fun previousSets(exerciseId: Long, excludingSessionId: Long): List<PreviousSet> {
        val db = readableDatabase
        val previousWeId = db.rawQuery("SELECT we.id FROM workout_exercise we JOIN workout_session ws ON ws.id=we.session_id WHERE we.exercise_id=? AND ws.ended_at IS NOT NULL AND ws.id<>? AND EXISTS(SELECT 1 FROM workout_set s WHERE s.workout_exercise_id=we.id AND s.skipped=0) ORDER BY ws.ended_at DESC LIMIT 1", arrayOf(exerciseId.toString(), excludingSessionId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) else return emptyList() }
        val out = mutableListOf<PreviousSet>()
        db.rawQuery("SELECT set_number,weight_kg,reps FROM workout_set WHERE workout_exercise_id=? AND skipped=0 ORDER BY set_number", arrayOf(previousWeId.toString())).use { c ->
            while (c.moveToNext()) out += PreviousSet(c.getInt(0), c.getDouble(1), c.getInt(2))
        }
        return out
    }
}
