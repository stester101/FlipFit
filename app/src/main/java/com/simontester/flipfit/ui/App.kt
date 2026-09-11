package com.simontester.flipfit.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simontester.flipfit.BuildConfig
import com.simontester.flipfit.MainActivity
import com.simontester.flipfit.MainViewModel
import com.simontester.flipfit.model.*
import com.simontester.flipfit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun FlipFitApp(vm: MainViewModel, compact: Boolean, wide: Boolean) {
    val active by vm.active.collectAsState()
    val templates by vm.templates.collectAsState()
    val history by vm.history.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(active, vm.settings.keepAwake) {
        (context as? MainActivity)?.applyKeepAwake(active != null && vm.settings.keepAwake)
    }

    if (active != null) WorkoutScreen(vm, active!!, compact, wide)
    else HomeScreen(vm, templates, history, compact)
}

@Composable
private fun HomeScreen(
    vm: MainViewModel,
    templates: List<WorkoutTemplate>,
    history: List<WorkoutSession>,
    compact: Boolean
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(if (compact) 14.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FLIPFIT", fontSize = if (compact) 22.sp else 30.sp, fontWeight = FontWeight.Black, color = Accent)
        Text("TODAY", color = Muted, fontWeight = FontWeight.Bold)

        templates.firstOrNull()?.let { t ->
            Button(
                onClick = { vm.start(t) },
                modifier = Modifier.fillMaxWidth().height(if (compact) 64.dp else 72.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("START ${t.name}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }

        if (!compact) {
            Text("TEMPLATES", color = Muted, fontWeight = FontWeight.Bold)
            templates.forEach { t ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(t.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("${t.exercises.size} exercises • ${t.exercises.sumOf { it.defaultSets }} sets", color = Muted)
                    }
                }
            }

            Text("HISTORY", color = Muted, fontWeight = FontWeight.Bold)
            if (history.isEmpty()) Text("No completed workouts yet.", color = Muted)
            else history.take(8).forEach { HistoryCard(it) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Keep screen awake", color = Color.White)
                Switch(checked = vm.settings.keepAwake, onCheckedChange = { vm.settings.keepAwake = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Haptics", color = Color.White)
                Switch(checked = vm.settings.haptics, onCheckedChange = { vm.settings.haptics = it })
            }
            Text("Version ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WorkoutScreen(vm: MainViewModel, session: WorkoutSession, compact: Boolean, wide: Boolean) {
    val currentIndex = session.exercises.indexOfFirst { it.sets.size < it.targetSets }
        .let { if (it == -1) session.exercises.lastIndex else it }
    val current = session.exercises[currentIndex]
    val previous = remember(session.id, current.exercise.id, current.sets.size) { vm.previous(current.exercise.id) }
    val nextSet = current.sets.size + 1
    val suggested = previous.getOrNull((nextSet - 1).coerceAtLeast(0)) ?: previous.lastOrNull()
    var weight by remember(current.exercise.id, nextSet) {
        mutableDoubleStateOf(suggested?.weightKg ?: current.sets.lastOrNull()?.weightKg ?: 10.0)
    }
    var reps by remember(current.exercise.id, nextSet) {
        mutableIntStateOf(suggested?.reps ?: current.sets.lastOrNull()?.reps ?: 8)
    }
    val context = LocalContext.current
    fun buzz(strong: Boolean = false) { if (vm.settings.haptics) haptic(context, strong) }

    if (compact) {
        val pager = rememberPagerState(pageCount = { 2 })
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().background(Bg)) { page ->
            if (page == 0) {
                CurrentSetCompact(
                    current, previous, nextSet, weight, reps,
                    onWeight = { weight = (weight + it).coerceAtLeast(0.0); buzz() },
                    onReps = { reps = (reps + it).coerceAtLeast(0); buzz() },
                    onLog = { if (nextSet <= current.targetSets) { vm.log(current.exercise.id, nextSet, weight, reps); buzz(true) } },
                    canFinish = session.exercises.all { it.sets.size >= it.targetSets },
                    onFinish = { vm.finish(); buzz(true) },
                    onUndo = { vm.undo() }
                )
            } else {
                OverviewCompact(session, currentIndex, onFinish = { vm.finish() })
            }
        }
    } else {
        FullWorkout(
            session, currentIndex, current, previous, nextSet, weight, reps, wide,
            onWeight = { weight = (weight + it).coerceAtLeast(0.0); buzz() },
            onReps = { reps = (reps + it).coerceAtLeast(0); buzz() },
            onLog = { if (nextSet <= current.targetSets) { vm.log(current.exercise.id, nextSet, weight, reps); buzz(true) } },
            onUndo = { vm.undo() },
            onFinish = { vm.finish(); buzz(true) }
        )
    }
}

@Composable
private fun CurrentSetCompact(
    ex: SessionExercise,
    previous: List<PreviousSet>,
    setNo: Int,
    weight: Double,
    reps: Int,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onLog: () -> Unit,
    canFinish: Boolean,
    onFinish: () -> Unit,
    onUndo: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(ex.exercise.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
        Text(previous.getOrNull(setNo - 1)?.let { "LAST ${fmt(it.weightKg)}kg × ${it.reps}" }
            ?: previous.lastOrNull()?.let { "LAST ${fmt(it.weightKg)}kg × ${it.reps}" }
            ?: "FIRST TIME", color = Muted, fontSize = 12.sp)
        Text("SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MiniButton("−2.5") { onWeight(-2.5) }
            Text("${fmt(weight)} KG", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            MiniButton("+2.5") { onWeight(2.5) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            MiniButton("−") { onReps(-1) }
            Text("$reps REPS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            MiniButton("+") { onReps(1) }
        }
        Button(
            onClick = if (canFinish) onFinish else onLog,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (canFinish) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        if (ex.sets.isNotEmpty()) TextButton(onClick = onUndo, modifier = Modifier.height(34.dp)) {
            Text("Undo last set", fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(width = 72.dp, height = 50.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.DarkGray)
    ) {
        Text(label, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OverviewCompact(session: WorkoutSession, currentIndex: Int, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(14.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(session.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
        Text("WORKOUT OVERVIEW", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        session.exercises.forEachIndexed { i, e ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (i == currentIndex) Surface else Color.Transparent).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(e.exercise.name, color = Color.White, fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                Text("${e.sets.size}/${e.targetSets}", color = if (e.sets.size >= e.targetSets) Accent else Muted)
            }
        }
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish early", color = Accent) }
    }
}

@Composable
private fun FullWorkout(
    session: WorkoutSession,
    currentIndex: Int,
    ex: SessionExercise,
    previous: List<PreviousSet>,
    setNo: Int,
    weight: Double,
    reps: Int,
    wide: Boolean,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onLog: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    if (wide) {
        WideWorkout(session, currentIndex, ex, previous, setNo, weight, reps, onWeight, onReps, onLog, onUndo, onFinish)
    } else {
        PortraitWorkout(session, currentIndex, ex, previous, setNo, weight, reps, onWeight, onReps, onLog, onUndo, onFinish)
    }
}

@Composable
private fun PortraitWorkout(
    session: WorkoutSession,
    currentIndex: Int,
    ex: SessionExercise,
    previous: List<PreviousSet>,
    setNo: Int,
    weight: Double,
    reps: Int,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onLog: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    val complete = session.exercises.all { it.sets.size >= it.targetSets }
    Column(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.name.uppercase(), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(ex.exercise.name.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Text("${currentIndex + 1}/${session.exercises.size}", color = Muted, fontWeight = FontWeight.Bold)
        }

        Text("SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Black)

        Text("${fmt(weight)} KG", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MiniButton("−2.5") { onWeight(-2.5) }
            MiniButton("+2.5") { onWeight(2.5) }
        }

        Text("$reps REPS", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MiniButton("−") { onReps(-1) }
            MiniButton("+") { onReps(1) }
        }

        Button(
            onClick = if (complete) onFinish else onLog,
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(if (complete) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }

        if (ex.sets.isNotEmpty()) TextButton(onClick = onUndo) { Text("Undo last set", color = Muted) }

        Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("LAST TIME", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (previous.isEmpty()) Text("No previous sets", color = Muted)
                else previous.forEach { Text("Set ${it.setNumber} — ${fmt(it.weightKg)}kg × ${it.reps}", color = Color.White) }
            }
        }

        Text("WORKOUT", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        session.exercises.forEachIndexed { i, e ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (i == currentIndex) Surface else Color.Transparent).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(e.exercise.name, color = Color.White, fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                Text("${e.sets.size}/${e.targetSets}", color = if (e.sets.size >= e.targetSets) Accent else Muted, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish workout", color = Accent) }
    }
}

@Composable
private fun WideWorkout(
    session: WorkoutSession,
    currentIndex: Int,
    ex: SessionExercise,
    previous: List<PreviousSet>,
    setNo: Int,
    weight: Double,
    reps: Int,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onLog: () -> Unit,
    onUndo: () -> Unit,
    onFinish: () -> Unit
) {
    val complete = session.exercises.all { it.sets.size >= it.targetSets }
    Row(
        Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(session.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            session.exercises.forEachIndexed { i, e ->
                Card(colors = CardDefaults.cardColors(containerColor = if (i == currentIndex) Surface else Color.Transparent), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(e.exercise.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${e.exercise.category} • ${e.exercise.equipment}", color = Muted, fontSize = 12.sp)
                        }
                        Text("${e.sets.size}/${e.targetSets}", color = if (e.sets.size >= e.targetSets) Accent else Muted)
                    }
                }
            }
            OutlinedButton(onClick = onFinish) { Text("Finish workout", color = Accent) }
        }

        Column(
            Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(ex.exercise.name.uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("${fmt(weight)} KG", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniButton("−2.5") { onWeight(-2.5) }
                MiniButton("+2.5") { onWeight(2.5) }
            }
            Spacer(Modifier.height(10.dp))
            Text("$reps REPS", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniButton("−") { onReps(-1) }
                MiniButton("+") { onReps(1) }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = if (complete) onFinish else onLog,
                modifier = Modifier.fillMaxWidth().height(68.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (complete) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            if (ex.sets.isNotEmpty()) TextButton(onClick = onUndo) { Text("Undo last set", color = Muted) }
            Spacer(Modifier.height(12.dp))
            Text("LAST TIME", color = Muted, fontWeight = FontWeight.Bold)
            if (previous.isEmpty()) Text("No previous sets", color = Muted)
            else previous.forEach { Text("Set ${it.setNumber} — ${fmt(it.weightKg)}kg × ${it.reps}", color = Color.White) }
        }
    }
}

@Composable
private fun HistoryCard(s: WorkoutSession) {
    val fmtDate = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.UK) }
    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(s.name, color = Color.White, fontWeight = FontWeight.Black)
            Text(fmtDate.format(Date(s.startedAt)), color = Muted, fontSize = 12.sp)
            Text("${s.exercises.sumOf { it.sets.size }} sets logged", color = Muted)
        }
    }
}

private fun fmt(v: Double) = if (v == v.roundToInt().toDouble()) v.roundToInt().toString() else String.format(Locale.UK, "%.1f", v)

private fun haptic(context: Context, strong: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    }
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 45 else 20, if (strong) 110 else 55))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(if (strong) 45 else 20)
    }
}
