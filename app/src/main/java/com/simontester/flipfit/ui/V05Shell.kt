package com.simontester.flipfit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.simontester.flipfit.MainViewModel
import com.simontester.flipfit.model.Exercise
import com.simontester.flipfit.model.SessionExercise
import com.simontester.flipfit.model.WorkoutSession
import com.simontester.flipfit.ui.theme.*

@Composable
fun FlipFitV05App(vm: MainViewModel, compact: Boolean, wide: Boolean) {
    val active by vm.active.collectAsState()
    val completion by vm.completion.collectAsState()
    val exercises by vm.exercises.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var pickerMode by remember { mutableStateOf<String?>(null) }
    var overviewOpen by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FlipFitApp(vm = vm, compact = compact, wide = wide)
        if (!compact && active != null && completion == null) {
            Row(
                Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(top = 12.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { overviewOpen = true },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, OutlineSoft),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Glass)
                ) { Text("LIST", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, OutlineSoft),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Glass)
                ) { Text("⋮", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) }
            }
        }
    }

    val session = active
    if (session != null) {
        val current = currentExercise(session)
        if (menuOpen && current != null) {
            UnfoldedWorkoutMenu(
                session = session,
                current = current,
                onDismiss = { menuOpen = false },
                onSkipSet = { menuOpen = false; nextOpenSet(current)?.let { vm.skipSet(current.id, it) } },
                onSkipExercise = { menuOpen = false; vm.skipExercise(current.id) },
                onAddSet = { menuOpen = false; vm.adjustSets(current.id, 1) },
                onRemoveSet = { menuOpen = false; vm.adjustSets(current.id, -1) },
                onAddExercise = { menuOpen = false; pickerMode = "add" },
                onReplaceExercise = { menuOpen = false; pickerMode = "replace" },
                onMoveUp = { menuOpen = false; vm.moveExercise(current.id, -1) },
                onMoveDown = { menuOpen = false; vm.moveExercise(current.id, 1) },
                onUndo = { menuOpen = false; vm.undo() },
                onOverview = { menuOpen = false; overviewOpen = true },
                onFinish = { menuOpen = false; confirmFinish = true }
            )
        }

        if (overviewOpen) {
            UnfoldedWorkoutOverview(session, { overviewOpen = false }) { order ->
                overviewOpen = false
                vm.jump(order)
            }
        }

        pickerMode?.let { mode ->
            V05ExercisePicker(
                exercises = exercises,
                title = if (mode == "add") "Add exercise" else "Replace exercise",
                onDismiss = { pickerMode = null }
            ) { picked ->
                if (mode == "add") vm.addExercise(picked.id) else current?.let { vm.replaceExercise(it.id, picked.id) }
                pickerMode = null
            }
        }

        if (confirmFinish) {
            AlertDialog(
                onDismissRequest = { confirmFinish = false },
                containerColor = SurfaceHigh,
                title = { Text("Finish workout?", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("Logged and skipped sets will be saved. Unfinished planned sets will make this an ended-early workout.", color = Muted) },
                confirmButton = { Button(onClick = { confirmFinish = false; vm.finish() }) { Text("FINISH", color = Color.Black, fontWeight = FontWeight.Black) } },
                dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("CANCEL", color = Muted) } }
            )
        }
    }
}

private fun currentExercise(session: WorkoutSession): SessionExercise? =
    session.exercises.firstOrNull { it.orderIndex == session.currentOrderIndex }
        ?: session.exercises.firstOrNull { it.sets.size < it.targetSets }
        ?: session.exercises.lastOrNull()

private fun nextOpenSet(ex: SessionExercise): Int? =
    (1..ex.targetSets).firstOrNull { n -> ex.sets.none { it.setNumber == n } }

@Composable
private fun UnfoldedWorkoutMenu(
    session: WorkoutSession,
    current: SessionExercise,
    onDismiss: () -> Unit,
    onSkipSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onAddExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUndo: () -> Unit,
    onOverview: () -> Unit,
    onFinish: () -> Unit
) {
    val minOrder = session.exercises.minOfOrNull { it.orderIndex } ?: current.orderIndex
    val maxOrder = session.exercises.maxOfOrNull { it.orderIndex } ?: current.orderIndex
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceHigh,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, OutlineSoft),
            modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp)
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("WORKOUT ACTIONS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text(current.exercise.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("${current.sets.size}/${current.targetSets} sets accounted for", color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("CLOSE", color = Muted, fontWeight = FontWeight.Bold) }
                }
                V05Action("Workout overview", onOverview)
                HorizontalDivider(color = OutlineSoft)
                V05Action("Skip current set", onSkipSet, enabled = nextOpenSet(current) != null)
                V05Action("Skip remaining exercise", onSkipExercise, enabled = current.sets.size < current.targetSets)
                V05Action("Add planned set", onAddSet)
                V05Action("Remove uncompleted set", onRemoveSet, enabled = current.targetSets > current.sets.size)
                HorizontalDivider(color = OutlineSoft)
                V05Action("Add exercise", onAddExercise)
                V05Action("Replace exercise", onReplaceExercise, enabled = current.sets.isEmpty())
                V05Action("Move exercise up", onMoveUp, enabled = current.orderIndex > minOrder)
                V05Action("Move exercise down", onMoveDown, enabled = current.orderIndex < maxOrder)
                HorizontalDivider(color = OutlineSoft)
                V05Action("Undo last accounted set", onUndo, enabled = session.exercises.any { it.sets.isNotEmpty() })
                V05Action("Finish workout", onFinish, danger = true)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun V05Action(label: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = if (!enabled) MutedLow else if (danger) Danger else Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UnfoldedWorkoutOverview(session: WorkoutSession, onDismiss: () -> Unit, onJump: (Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = SurfaceHigh, shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("WORKOUT OVERVIEW", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("CLOSE", color = Muted) }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session.exercises.sortedBy { it.orderIndex }.forEachIndexed { index, ex ->
                        val isCurrent = ex.orderIndex == session.currentOrderIndex
                        val skipped = ex.sets.count { it.skipped }
                        Surface(
                            color = if (isCurrent) Color(0xDD151A12) else GlassSoft,
                            shape = RoundedCornerShape(17.dp),
                            border = BorderStroke(1.dp, if (isCurrent) Color(0x554C6A28) else OutlineSoft),
                            modifier = Modifier.fillMaxWidth().clickable { onJump(ex.orderIndex) }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", color = if (isCurrent) Accent else Muted, fontWeight = FontWeight.Black, modifier = Modifier.width(26.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(ex.exercise.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("${ex.sets.size}/${ex.targetSets} sets${if (skipped > 0) " • $skipped skipped" else ""}", color = Muted, fontSize = 11.sp)
                                }
                                Text(if (isCurrent) "NOW" else "›", color = if (isCurrent) Accent else Muted, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V05ExercisePicker(exercises: List<Exercise>, title: String, onDismiss: () -> Unit, onPick: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || it.equipment.contains(query, true) }
        .sortedWith(compareByDescending<Exercise> { it.favorite }.thenBy { it.category }.thenBy { it.name })
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = SurfaceHigh, shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("CLOSE", color = Muted) }
                }
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search name, muscle or equipment") })
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    filtered.forEach { ex ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(horizontal = 5.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${if (ex.favorite) "★ " else ""}${ex.name}", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${ex.category} • ${ex.equipment}", color = Muted, fontSize = 11.sp)
                            }
                            Text(if (title.startsWith("Replace", true)) "USE" else "ADD", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        HorizontalDivider(color = OutlineSoft)
                    }
                }
            }
        }
    }
}
