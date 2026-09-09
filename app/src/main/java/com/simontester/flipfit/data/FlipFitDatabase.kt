package com.simontester.flipfit.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.simontester.flipfit.model.*

class FlipFitDatabase(context: Context) : SQLiteOpenHelper(context, "flipfit.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE exercise(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,category TEXT NOT NULL,equipment TEXT NOT NULL,notes TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE workout_template(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE template_exercise(id INTEGER PRIMARY KEY AUTOINCREMENT,template_id INTEGER NOT NULL,exercise_id INTEGER NOT NULL,order_index INTEGER NOT NULL,default_sets INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE workout_session(id INTEGER PRIMARY KEY AUTOINCREMENT,template_id INTEGER,name TEXT NOT NULL,started_at INTEGER NOT NULL,ended_at INTEGER)")
        db.execSQL("CREATE TABLE workout_exercise(id INTEGER PRIMARY KEY AUTOINCREMENT,session_id INTEGER NOT NULL,exercise_id INTEGER NOT NULL,order_index INTEGER NOT NULL,target_sets INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE workout_set(id INTEGER PRIMARY KEY AUTOINCREMENT,workout_exercise_id INTEGER NOT NULL,set_number INTEGER NOT NULL,weight_kg REAL NOT NULL,reps INTEGER NOT NULL,logged_at INTEGER NOT NULL)")
        seed(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun seed(db: SQLiteDatabase) {
        val exercises = listOf(
            Triple("Dumbbell Bench Press","Chest","Dumbbell"), Triple("Incline Dumbbell Press","Chest","Dumbbell"),
            Triple("Dumbbell Curl","Biceps","Dumbbell"), Triple("Hammer Curl","Biceps","Dumbbell"),
            Triple("Dumbbell Tricep Extension","Triceps","Dumbbell"), Triple("Bench Press","Chest","Barbell"),
            Triple("Lat Pulldown","Back","Cable"), Triple("Seated Row","Back","Cable"),
            Triple("Lateral Raise","Shoulders","Dumbbell"), Triple("Tricep Pushdown","Triceps","Cable"),
            Triple("Goblet Squat","Legs","Dumbbell"), Triple("Romanian Deadlift","Legs","Dumbbell")
        )
        val ids = exercises.map { (name, category, equipment) ->
            db.insert("exercise", null, ContentValues().apply { put("name",name); put("category",category); put("equipment",equipment) })
        }
        val templateId = db.insert("workout_template", null, ContentValues().apply { put("name","CHEST + ARMS") })
        ids.take(5).forEachIndexed { index, exerciseId ->
            db.insert("template_exercise", null, ContentValues().apply { put("template_id",templateId); put("exercise_id",exerciseId); put("order_index",index); put("default_sets",3) })
        }
    }

    fun getTemplates(): List<WorkoutTemplate> {
        val db = readableDatabase
        val result = mutableListOf<WorkoutTemplate>()
        db.rawQuery("SELECT id,name FROM workout_template ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0); val name = c.getString(1)
                val items = mutableListOf<TemplateExercise>()
                db.rawQuery("SELECT e.id,e.name,e.category,e.equipment,e.notes,te.order_index,te.default_sets FROM template_exercise te JOIN exercise e ON e.id=te.exercise_id WHERE te.template_id=? ORDER BY te.order_index", arrayOf(id.toString())).use { ec ->
                    while (ec.moveToNext()) items += TemplateExercise(Exercise(ec.getLong(0),ec.getString(1),ec.getString(2),ec.getString(3),ec.getString(4)),ec.getInt(5),ec.getInt(6))
                }
                result += WorkoutTemplate(id,name,items)
            }
        }
        return result
    }

    fun startSession(template: WorkoutTemplate): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val sessionId = db.insert("workout_session", null, ContentValues().apply { put("template_id",template.id); put("name",template.name); put("started_at",System.currentTimeMillis()) })
            template.exercises.forEach { te -> db.insert("workout_exercise", null, ContentValues().apply { put("session_id",sessionId); put("exercise_id",te.exercise.id); put("order_index",te.orderIndex); put("target_sets",te.defaultSets) }) }
            db.setTransactionSuccessful(); return sessionId
        } finally { db.endTransaction() }
    }

    fun activeSession(): WorkoutSession? = loadSessions("WHERE ws.ended_at IS NULL ORDER BY ws.started_at DESC LIMIT 1").firstOrNull()
    fun history(): List<WorkoutSession> = loadSessions("WHERE ws.ended_at IS NOT NULL ORDER BY ws.ended_at DESC LIMIT 50")

    private fun loadSessions(where: String): List<WorkoutSession> {
        val db = readableDatabase; val out = mutableListOf<WorkoutSession>()
        db.rawQuery("SELECT ws.id,ws.template_id,ws.name,ws.started_at,ws.ended_at FROM workout_session ws $where", null).use { sc ->
            while(sc.moveToNext()) {
                val sid=sc.getLong(0); val templateId=if(sc.isNull(1)) null else sc.getLong(1); val name=sc.getString(2); val started=sc.getLong(3); val ended=if(sc.isNull(4)) null else sc.getLong(4)
                val exs=mutableListOf<SessionExercise>()
                db.rawQuery("SELECT we.id,e.id,e.name,e.category,e.equipment,e.notes,we.order_index,we.target_sets FROM workout_exercise we JOIN exercise e ON e.id=we.exercise_id WHERE we.session_id=? ORDER BY we.order_index", arrayOf(sid.toString())).use { ec ->
                    while(ec.moveToNext()) {
                        val weId=ec.getLong(0); val sets=mutableListOf<LoggedSet>()
                        db.rawQuery("SELECT id,set_number,weight_kg,reps FROM workout_set WHERE workout_exercise_id=? ORDER BY set_number", arrayOf(weId.toString())).use { c -> while(c.moveToNext()) sets += LoggedSet(c.getLong(0),ec.getLong(1),c.getInt(1),c.getDouble(2),c.getInt(3)) }
                        exs += SessionExercise(Exercise(ec.getLong(1),ec.getString(2),ec.getString(3),ec.getString(4),ec.getString(5)),ec.getInt(6),ec.getInt(7),sets)
                    }
                }
                out += WorkoutSession(sid,templateId,name,started,ended,exs)
            }
        }
        return out
    }

    fun logSet(sessionId: Long, exerciseId: Long, setNumber: Int, weightKg: Double, reps: Int): Long {
        val db=writableDatabase
        val weId=db.rawQuery("SELECT id FROM workout_exercise WHERE session_id=? AND exercise_id=?", arrayOf(sessionId.toString(),exerciseId.toString())).use { c -> c.moveToFirst(); c.getLong(0) }
        return db.insert("workout_set", null, ContentValues().apply { put("workout_exercise_id",weId); put("set_number",setNumber); put("weight_kg",weightKg); put("reps",reps); put("logged_at",System.currentTimeMillis()) })
    }
    fun deleteSet(setId: Long) { writableDatabase.delete("workout_set","id=?", arrayOf(setId.toString())) }
    fun finishSession(sessionId: Long) { writableDatabase.update("workout_session", ContentValues().apply { put("ended_at",System.currentTimeMillis()) }, "id=?", arrayOf(sessionId.toString())) }
    fun previousSets(exerciseId: Long, excludingSessionId: Long): List<PreviousSet> {
        val db=readableDatabase
        val previousWeId=db.rawQuery("SELECT we.id FROM workout_exercise we JOIN workout_session ws ON ws.id=we.session_id WHERE we.exercise_id=? AND ws.ended_at IS NOT NULL AND ws.id<>? ORDER BY ws.ended_at DESC LIMIT 1", arrayOf(exerciseId.toString(),excludingSessionId.toString())).use { c -> if(c.moveToFirst()) c.getLong(0) else return emptyList() }
        val out=mutableListOf<PreviousSet>()
        db.rawQuery("SELECT set_number,weight_kg,reps FROM workout_set WHERE workout_exercise_id=? ORDER BY set_number", arrayOf(previousWeId.toString())).use { c -> while(c.moveToNext()) out += PreviousSet(c.getInt(0),c.getDouble(1),c.getInt(2)) }
        return out
    }
}
