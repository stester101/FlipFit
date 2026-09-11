package com.simontester.flipfit.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.simontester.flipfit.BuildConfig
import com.simontester.flipfit.MainActivity
import com.simontester.flipfit.MainViewModel
import com.simontester.flipfit.model.*
import com.simontester.flipfit.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FlipFitApp(vm: MainViewModel, compact: Boolean, wide: Boolean) {
    val active by vm.active.collectAsState()
    val templates by vm.templates.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val history by vm.history.collectAsState()
    val completion by vm.completion.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(active, vm.settings.keepAwake) {
        (context as? MainActivity)?.applyKeepAwake(active != null && vm.settings.keepAwake)
    }

    when {
        completion != null -> CompletionScreen(completion!!, compact) { vm.clearCompletion() }
        active != null -> WorkoutScreen(vm, active!!, exercises, compact)
        else -> HomeScreen(vm, templates, history, compact)
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel, templates: List<WorkoutTemplate>, history: List<WorkoutSession>, compact: Boolean) {
    var keepAwake by remember { mutableStateOf(vm.settings.keepAwake) }
    var haptics by remember { mutableStateOf(vm.settings.haptics) }
    var increment by remember { mutableFloatStateOf(vm.settings.incrementKg) }

    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(if (compact) 14.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp)
    ) {
        Text("FLIPFIT", fontSize = if (compact) 24.sp else 30.sp, fontWeight = FontWeight.Black, color = Accent)
        Text(if (compact) "CHOOSE WORKOUT" else "TEMPLATES", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        templates.forEach { t ->
            Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(if (compact) 12.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = if (compact) 16.sp else 20.sp)
                        Text("${t.exercises.size} exercises • ${t.exercises.sumOf { it.defaultSets }} sets", color = Muted, fontSize = 12.sp)
                    }
                    Button(onClick = { vm.start(t) }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)) {
                        Text("START", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (!compact) {
            Text("HISTORY", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (history.isEmpty()) Text("No workouts yet.", color = Muted)
            else history.take(8).forEach { HistoryCard(it) }

            Text("SETTINGS", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingSwitch("Keep screen awake during workout", keepAwake) {
                        keepAwake = it; vm.settings.keepAwake = it
                    }
                    SettingSwitch("Haptics", haptics) {
                        haptics = it; vm.settings.haptics = it
                    }
                    Text("Weight increment", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 1f, 2f, 2.5f, 5f).forEach { value ->
                            FilterChip(
                                selected = increment == value,
                                onClick = { increment = value; vm.settings.incrementKg = value },
                                label = { Text("${fmt(value.toDouble())}kg") }
                            )
                        }
                    }
                }
            }
            Text("Version ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun WorkoutScreen(vm: MainViewModel, session: WorkoutSession, exercises: List<Exercise>, compact: Boolean) {
    val prFlash by vm.prFlash.collectAsState()
    val current = session.exercises.firstOrNull { it.orderIndex == session.currentOrderIndex }
        ?: session.exercises.firstOrNull { it.sets.size < it.targetSets }
        ?: session.exercises.last()
    val currentIndex = session.exercises.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
    val nextSet = (1..current.targetSets).firstOrNull { n -> current.sets.none { it.setNumber == n } } ?: (current.targetSets + 1)
    val previous = remember(session.id, current.exercise.id, current.sets.size) { vm.previous(current.exercise.id) }
    val sameSetLastTime = previous.firstOrNull { it.setNumber == nextSet }
    val fallback = previous.lastOrNull()
    val suggested = sameSetLastTime ?: fallback
    val increment = current.exercise.incrementKg ?: vm.settings.incrementKg.toDouble()
    var weight by remember(current.id, nextSet) { mutableDoubleStateOf(suggested?.weightKg ?: 10.0) }
    var reps by remember(current.id, nextSet) { mutableIntStateOf(suggested?.reps ?: 8) }
    var editWeight by remember { mutableStateOf(false) }
    var editReps by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var pickerMode by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val allComplete = session.exercises.all { it.sets.size >= it.targetSets }
    val hasAnySets = session.exercises.any { it.sets.isNotEmpty() }

    fun buzz(strong: Boolean = false) { if (vm.settings.haptics) haptic(context, strong) }

    LaunchedEffect(prFlash) {
        if (prFlash) {
            delay(1600)
            vm.clearPrFlash()
        }
    }

    val onLog = {
        if (!allComplete && nextSet <= current.targetSets) {
            vm.log(current.id, nextSet, if (current.exercise.trackingType == "reps_only") 0.0 else weight, reps)
            buzz(true)
        }
    }

    if (compact) {
        val pager = rememberPagerState(pageCount = { 2 })
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().background(Bg)) { page ->
            if (page == 0) {
                CurrentSetCompact(
                    session = session,
                    ex = current,
                    currentIndex = currentIndex,
                    previous = previous,
                    setNo = nextSet,
                    weight = weight,
                    reps = reps,
                    increment = increment,
                    prFlash = prFlash,
                    canFinish = allComplete,
                    canUndo = hasAnySets,
                    onWeight = { weight = (weight + it).coerceAtLeast(0.0); buzz() },
                    onReps = { reps = (reps + it).coerceAtLeast(0); buzz() },
                    onEditWeight = { editWeight = true },
                    onEditReps = { editReps = true },
                    onLog = onLog,
                    onFinish = { vm.finish(); buzz(true) },
                    onUndo = { vm.undo(); buzz() },
                    onHelp = { showHelp = true },
                    onSkipSet = { if (nextSet <= current.targetSets) { vm.skipSet(current.id, nextSet); buzz() } },
                    onSkipExercise = { vm.skipExercise(current.id); buzz() },
                    onAddSet = { vm.adjustSets(current.id, 1) },
                    onRemoveSet = { vm.adjustSets(current.id, -1) },
                    onAddExercise = { pickerMode = "add" },
                    onReplaceExercise = { pickerMode = "replace" },
                    onMoveUp = { vm.moveExercise(current.id, -1) },
                    onMoveDown = { vm.moveExercise(current.id, 1) }
                )
            } else {
                OverviewCompact(session, currentIndex, onJump = vm::jump, onFinish = { vm.finish() })
            }
        }
    } else {
        FullWorkout(
            session = session, ex = current, currentIndex = currentIndex, previous = previous, setNo = nextSet,
            weight = weight, reps = reps, increment = increment, prFlash = prFlash, canFinish = allComplete,
            onWeight = { weight = (weight + it).coerceAtLeast(0.0); buzz() },
            onReps = { reps = (reps + it).coerceAtLeast(0); buzz() },
            onEditWeight = { editWeight = true }, onEditReps = { editReps = true }, onLog = onLog,
            onUndo = { vm.undo() }, onFinish = { vm.finish() }, onHelp = { showHelp = true }
        )
    }

    if (editWeight) NumberEditDialog("Weight (kg)", fmt(weight), true, onDismiss = { editWeight = false }) { value ->
        value.toDoubleOrNull()?.let { weight = it.coerceAtLeast(0.0) }; editWeight = false
    }
    if (editReps) NumberEditDialog("Reps", reps.toString(), false, onDismiss = { editReps = false }) { value ->
        value.toIntOrNull()?.let { reps = it.coerceAtLeast(0) }; editReps = false
    }
    if (showHelp) ExerciseHelpDialog(current.exercise) { showHelp = false }
    pickerMode?.let { mode ->
        ExercisePickerDialog(
            exercises = exercises,
            title = if (mode == "add") "Add exercise" else "Replace exercise",
            onDismiss = { pickerMode = null },
            onPick = { picked ->
                if (mode == "add") vm.addExercise(picked.id) else vm.replaceExercise(current.id, picked.id)
                pickerMode = null
            }
        )
    }
}

@Composable
private fun CurrentSetCompact(
    session: WorkoutSession,
    ex: SessionExercise,
    currentIndex: Int,
    previous: List<PreviousSet>,
    setNo: Int,
    weight: Double,
    reps: Int,
    increment: Double,
    prFlash: Boolean,
    canFinish: Boolean,
    canUndo: Boolean,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onEditWeight: () -> Unit,
    onEditReps: () -> Unit,
    onLog: () -> Unit,
    onFinish: () -> Unit,
    onUndo: () -> Unit,
    onHelp: () -> Unit,
    onSkipSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onAddExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ex.exercise.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Text("${currentIndex + 1}/${session.exercises.size}", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onHelp, contentPadding = PaddingValues(4.dp), modifier = Modifier.size(34.dp)) { Text("?", color = Accent, fontWeight = FontWeight.Black) }
            Box {
                TextButton(onClick = { menuOpen = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(34.dp)) { Text("⋮", color = Color.White, fontSize = 22.sp) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Skip set") }, onClick = { menuOpen = false; onSkipSet() })
                    DropdownMenuItem(text = { Text("Add set") }, onClick = { menuOpen = false; onAddSet() })
                    DropdownMenuItem(text = { Text("Remove uncompleted set") }, onClick = { menuOpen = false; onRemoveSet() })
                    DropdownMenuItem(text = { Text("Skip exercise") }, onClick = { menuOpen = false; onSkipExercise() })
                    DropdownMenuItem(text = { Text("Add exercise") }, onClick = { menuOpen = false; onAddExercise() })
                    DropdownMenuItem(text = { Text("Replace exercise") }, enabled = ex.sets.isEmpty(), onClick = { menuOpen = false; onReplaceExercise() })
                    DropdownMenuItem(text = { Text("Move exercise up") }, onClick = { menuOpen = false; onMoveUp() })
                    DropdownMenuItem(text = { Text("Move exercise down") }, onClick = { menuOpen = false; onMoveDown() })
                    DropdownMenuItem(text = { Text("Finish workout") }, onClick = { menuOpen = false; onFinish() })
                }
            }
        }
        Text(previousLine(previous, ex.exercise.trackingType), color = Muted, fontSize = 11.sp, maxLines = 1)
        if (prFlash) Text("NEW PR", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        else Text("SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        if (ex.exercise.trackingType != "reps_only") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                MiniButton("−${fmt(increment)}") { onWeight(-increment) }
                Text("${fmt(weight)} KG", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditWeight))
                MiniButton("+${fmt(increment)}") { onWeight(increment) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MiniButton("−") { onReps(-1) }
            Text("$reps REPS", color = Color.White, fontSize = if (ex.exercise.trackingType == "reps_only") 38.sp else 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditReps))
            MiniButton("+") { onReps(1) }
        }
        Button(
            onClick = if (canFinish) onFinish else onLog,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (canFinish) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        if (canUndo) TextButton(onClick = onUndo, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(0.dp)) {
            Text("UNDO LAST", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(width = 86.dp, height = 54.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.DarkGray)
    ) { Text(label, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun OverviewCompact(session: WorkoutSession, currentIndex: Int, onJump: (Int) -> Unit, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(horizontal = 14.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(session.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
        Text("WORKOUT OVERVIEW", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        session.exercises.forEachIndexed { i, e ->
            val skipped = e.sets.count { it.skipped }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (i == currentIndex) Surface else Color.Transparent)
                    .clickable { onJump(e.orderIndex) }.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(e.exercise.name, color = Color.White, fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(if (skipped > 0) "${e.sets.size}/${e.targetSets} · ${skipped}S" else "${e.sets.size}/${e.targetSets}", color = if (e.sets.size >= e.targetSets) Accent else Muted, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish early", color = Accent) }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun FullWorkout(
    session: WorkoutSession,
    ex: SessionExercise,
    currentIndex: Int,
    previous: List<PreviousSet>,
    setNo: Int,
    weight: Double,
    reps: Int,
    increment: Double,
    prFlash: Boolean,
    canFinish: Boolean,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onEditWeight: () -> Unit,
    onEditReps: () -> Unit,
    onLog: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit,
    onHelp: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.name.uppercase(), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(ex.exercise.name.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Text("${currentIndex + 1}/${session.exercises.size}", color = Muted, fontWeight = FontWeight.Bold)
            TextButton(onClick = onHelp) { Text("?", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Black) }
        }
        Text(previousLine(previous, ex.exercise.trackingType), color = Muted)
        Text(if (prFlash) "NEW PR" else "SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        if (ex.exercise.trackingType != "reps_only") {
            Text("${fmt(weight)} KG", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditWeight))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniButton("−${fmt(increment)}") { onWeight(-increment) }
                MiniButton("+${fmt(increment)}") { onWeight(increment) }
            }
        }
        Text("$reps REPS", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditReps))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniButton("−") { onReps(-1) }; MiniButton("+") { onReps(1) }
        }
        Button(onClick = if (canFinish) onFinish else onLog, modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(20.dp)) {
            Text(if (canFinish) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        if (session.exercises.any { it.sets.isNotEmpty() }) TextButton(onClick = onUndo) { Text("Undo last set", color = Muted) }
    }
}

@Composable
private fun NumberEditDialog(title: String, initial: String, decimal: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("SAVE") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun ExercisePickerDialog(exercises: List<Exercise>, title: String, onDismiss: () -> Unit, onPick: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) }
        .sortedWith(compareByDescending<Exercise> { it.favorite }.thenBy { it.category }.thenBy { it.name })
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    TextButton(onClick = onDismiss) { Text("CLOSE", color = Muted) }
                }
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search exercises") })
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    filtered.forEach { exercise ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(exercise) }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(if (exercise.favorite) "★ ${exercise.name}" else exercise.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${exercise.category} • ${exercise.equipment}", color = Muted, fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseHelpDialog(exercise: Exercise, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(exercise.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    DiagramFrame("START")
                    Text("→", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    DiagramFrame("FINISH")
                }
                Text(
                    exercise.diagramHint.ifBlank { "Use controlled form through a comfortable range of motion. A richer movement diagram can be added when you edit this exercise." },
                    color = Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("BACK TO SET", color = Color.Black, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun DiagramFrame(label: String) {
    Box(
        Modifier.size(width = 112.dp, height = 105.dp).clip(RoundedCornerShape(16.dp)).background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("●", color = Accent, fontSize = 24.sp)
            Text("╱│╲", color = Color.White, fontSize = 19.sp)
            Text("╱ ╲", color = Color.White, fontSize = 19.sp)
            Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompletionScreen(summary: CompletionSummary, compact: Boolean, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(if (compact) 16.dp else 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("WORKOUT", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("COMPLETE", color = Accent, fontWeight = FontWeight.Black, fontSize = if (compact) 38.sp else 54.sp)
            Text(summary.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Stat("${summary.loggedSets}", "LOGGED")
                Stat("${summary.skippedSets}", "SKIPPED")
                Stat("${summary.prCount}", "PR${if (summary.prCount == 1) "" else "S"}")
            }
            Text("${summary.loggedSets + summary.skippedSets} / ${summary.plannedSets} planned sets accounted for", color = Muted, fontSize = 12.sp)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text("DONE", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryCard(session: WorkoutSession) {
    val logged = session.exercises.sumOf { e -> e.sets.count { !it.skipped } }
    val skipped = session.exercises.sumOf { e -> e.sets.count { it.skipped } }
    val accounted = logged + skipped
    val planned = session.exercises.sumOf { it.targetSets }
    val complete = accounted >= planned
    val date = SimpleDateFormat("d MMM yyyy HH:mm", Locale.UK).format(Date(session.endedAt ?: session.startedAt))
    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(session.name, color = Color.White, fontWeight = FontWeight.Black)
                Text(if (complete) "COMPLETED" else "ENDED EARLY", color = if (complete) Accent else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(date, color = Muted, fontSize = 12.sp)
            Text("$logged sets logged${if (skipped > 0) " • $skipped skipped" else ""}", color = Color.White, fontSize = 13.sp)
        }
    }
}

private fun previousLine(previous: List<PreviousSet>, trackingType: String): String {
    if (previous.isEmpty()) return "FIRST TIME"
    val sets = previous.joinToString(" · ") { p -> if (trackingType == "reps_only") "${p.reps}" else "${fmt(p.weightKg)}×${p.reps}" }
    return "LAST $sets"
}

private fun fmt(value: Double): String = if (value == value.roundToInt().toDouble()) value.roundToInt().toString() else String.format(Locale.UK, "%.1f", value)

private fun haptic(context: Context, strong: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 45L else 18L, if (strong) 150 else 70))
    } else {
        @Suppress("DEPRECATION") vibrator.vibrate(if (strong) 45L else 18L)
    }
}
