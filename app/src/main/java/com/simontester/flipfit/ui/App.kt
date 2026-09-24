package com.simontester.flipfit.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
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

private enum class AppPage { HOME, PROGRAMS, EXERCISES, HISTORY }

@Composable
fun FlipFitApp(vm: MainViewModel, compact: Boolean, wide: Boolean) {
    val active by vm.active.collectAsState()
    val templates by vm.templates.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val history by vm.history.collectAsState()
    val programs by vm.programs.collectAsState()
    val muscleGroups by vm.muscleGroups.collectAsState()
    val equipment by vm.equipment.collectAsState()
    val completion by vm.completion.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(active, vm.settings.keepAwake) { (context as? MainActivity)?.applyKeepAwake(active != null && vm.settings.keepAwake) }
    when {
        completion != null -> CompletionScreen(completion!!, compact) { vm.clearCompletion() }
        active != null -> WorkoutScreen(vm, active!!, exercises, compact)
        else -> HomeShell(vm, programs, templates, exercises, history, muscleGroups, equipment, compact)
    }
}

@Composable
private fun HomeShell(vm: MainViewModel, programs: List<WorkoutProgram>, templates: List<WorkoutTemplate>, exercises: List<Exercise>, history: List<WorkoutSession>, muscleGroups: List<LibraryAsset>, equipment: List<LibraryAsset>, compact: Boolean) {
    if (compact) { CompactHome(vm, templates); return }
    var page by remember { mutableStateOf(AppPage.HOME) }
    Scaffold(
        containerColor = Bg,
        bottomBar = { BottomNav(page) { page = it } }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).background(Bg).safeDrawingPadding()) {
            when (page) {
                AppPage.HOME -> HomeContent(vm, templates, history, onTemplates = { page = AppPage.PROGRAMS }, onHistory = { page = AppPage.HISTORY })
                AppPage.PROGRAMS -> ProgramsScreen(vm, programs, templates, exercises)
                AppPage.EXERCISES -> ExercisesScreen(vm, exercises, muscleGroups, equipment)
                AppPage.HISTORY -> HistoryHub(vm, history)
            }
        }
    }
}

@Composable
private fun BottomNav(selected: AppPage, onSelect: (AppPage) -> Unit) {
    Surface(color = Glass, border = BorderStroke(1.dp, OutlineSoft), shadowElevation = 18.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(
                Triple(AppPage.HOME, "⌂", "HOME"),
                Triple(AppPage.PROGRAMS, "▤", "PROGRAMS"),
                Triple(AppPage.EXERCISES, "◇", "EXERCISES"),
                Triple(AppPage.HISTORY, "◷", "HISTORY")
            ).forEach { (page, icon, label) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onSelect(page) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(icon, color = if (selected == page) Accent else Muted, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(label, color = if (selected == page) Color.White else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, lime: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = modifier) {
        Box(
            Modifier.matchParentSize()
                .clip(RoundedCornerShape(26.dp))
                .background(if (lime) LiquidLimeBrush else LiquidGlassBrush)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, if (lime) GlassLimeEdge else GlassEdge),
            shadowElevation = 10.dp
        ) { Column(Modifier.padding(17.dp), content = content) }
    }
}

@Composable
private fun SectionLabel(text: String) { Text(text, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) }

@Composable
private fun CompactHome(vm: MainViewModel, templates: List<WorkoutTemplate>) {
    Column(Modifier.fillMaxSize().background(Bg).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("FLIPFIT", color = Accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
        SectionLabel("CHOOSE WORKOUT")
        templates.forEach { t ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(t.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp); Text("${t.exercises.size} exercises • ${t.exercises.sumOf { it.defaultSets }} sets", color = Muted, fontSize = 11.sp) }
                    Button(onClick = { vm.start(t) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)) { Text("START", color = Color.Black, fontWeight = FontWeight.Black) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeContent(vm: MainViewModel, templates: List<WorkoutTemplate>, history: List<WorkoutSession>, onTemplates: () -> Unit, onHistory: () -> Unit) {
    var settings by remember { mutableStateOf(false) }
    val recent = history.firstOrNull()
    val quick = recent?.let { r -> templates.firstOrNull { it.name == r.name } } ?: templates.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("FLIPFIT", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black); Text("TRAIN SIMPLE. TRACK EVERYTHING.", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
            OutlinedButton(onClick = { settings = true }, modifier = Modifier.size(46.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSoft)) { Text("⚙", color = Muted, fontSize = 18.sp) }
        }

        quick?.let { t ->
            GlassCard(Modifier.fillMaxWidth(), lime = true) {
                SectionLabel(if (recent?.name == t.name) "QUICK START • RECENT" else "QUICK START")
                Spacer(Modifier.height(7.dp))
                Text(t.name, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("${t.exercises.size} exercises   •   ${t.exercises.sumOf { it.defaultSets }} sets", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.start(t) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) { Text("START WORKOUT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp) }
            }
        }

        if (recent != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionLabel("LAST WORKOUT"); Text("VIEW HISTORY", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onHistory)) }
            HistoryCard(recent, onHistory)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionLabel("YOUR TEMPLATES"); Text("SEE ALL", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onTemplates)) }
        templates.take(2).forEach { t ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(t.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black); Text("${t.exercises.size} exercises • ${t.exercises.sumOf { it.defaultSets }} sets", color = Muted, fontSize = 11.sp) }
                    TextButton(onClick = { vm.start(t) }) { Text("START", color = Accent, fontWeight = FontWeight.Black) }
                }
            }
        }
        Text("v${BuildConfig.VERSION_NAME}", color = MutedLow, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
    }
    if (settings) SettingsDialog(vm) { settings = false }
}

@Composable
private fun SettingsDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var keepAwake by remember { mutableStateOf(vm.settings.keepAwake) }
    var haptics by remember { mutableStateOf(vm.settings.haptics) }
    var increment by remember { mutableFloatStateOf(vm.settings.incrementKg) }
    var preview by remember { mutableStateOf<Pair<android.net.Uri, ImportPreview>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { runCatching { vm.exportBackup(it); message = "Backup exported" }.onFailure { message = "Export failed: ${it.message}" } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { preview = it to vm.previewBackup(it) }.onFailure { message = "That file is not a valid FlipFit backup" } }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = GlassDeep, shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GlassEdge), modifier = Modifier.fillMaxWidth().heightIn(max=680.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SETTINGS", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                SettingSwitch("Keep screen awake", keepAwake) { keepAwake = it; vm.settings.keepAwake = it }
                SettingSwitch("Haptics", haptics) { haptics = it; vm.settings.haptics = it }
                SectionLabel("GLOBAL WEIGHT INCREMENT")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0.5f, 1f, 2f, 2.5f, 5f).forEach { v -> FilterChip(selected = increment == v, onClick = { increment = v; vm.settings.incrementKg = v }, label = { Text("${fmt(v.toDouble())}kg") }) } }
                SectionLabel("BACKUP & RESTORE")
                Text("A FlipFit backup contains exercises, programs, templates and complete workout history.", color=Muted, fontSize=12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={exportLauncher.launch("FlipFit-backup-v${BuildConfig.VERSION_NAME}.flipfit")},modifier=Modifier.weight(1f)){Text("EXPORT",color=Accent)}
                    OutlinedButton(onClick={importLauncher.launch(arrayOf("*/*"))},modifier=Modifier.weight(1f)){Text("IMPORT",color=Accent)}
                }
                OutlinedButton(onClick={vm.resetStarterContent();message="Starter content restored"},modifier=Modifier.fillMaxWidth()){Text("RESTORE STARTER CONTENT",color=Muted)}
                message?.let { Text(it,color=Accent,fontSize=11.sp) }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("DONE", color = Color.Black, fontWeight = FontWeight.Black) }
            }
        }
    }
    preview?.let { (uri,p) ->
        AlertDialog(onDismissRequest={preview=null},containerColor=GlassDeep,title={Text("Import FlipFit backup?")},text={Text("${p.exercises} exercises • ${p.templates} templates • ${p.programs} programs • ${p.workouts} workouts\n\nThis will replace the current FlipFit database. Export first if you want to keep it.")},confirmButton={TextButton(onClick={runCatching{vm.importBackup(uri);message="Backup restored";preview=null}.onFailure{message="Import failed: ${it.message}";preview=null}}){Text("REPLACE & IMPORT",color=Danger)}},dismissButton={TextButton(onClick={preview=null}){Text("CANCEL")}})
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = Color.White, modifier = Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onChange) } }

@Composable
private fun WorkoutScreen(vm: MainViewModel, session: WorkoutSession, exercises: List<Exercise>, compact: Boolean) {
    val prFlash by vm.prFlash.collectAsState()
    val current = session.exercises.firstOrNull { it.orderIndex == session.currentOrderIndex } ?: session.exercises.firstOrNull { it.sets.size < it.targetSets } ?: session.exercises.last()
    val currentIndex = session.exercises.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
    val nextSet = (1..current.targetSets).firstOrNull { n -> current.sets.none { it.setNumber == n } } ?: (current.targetSets + 1)
    val previous = remember(session.id, current.exercise.id, current.sets.size) { vm.previous(current.exercise.id) }
    val suggested = previous.firstOrNull { it.setNumber == nextSet } ?: previous.lastOrNull()
    val increment = current.exercise.incrementKg ?: vm.settings.incrementKg.toDouble()
    var weight by remember(current.id, nextSet) { mutableDoubleStateOf(suggested?.weightKg ?: 10.0) }
    var reps by remember(current.id, nextSet) { mutableIntStateOf(suggested?.reps ?: 8) }
    var editWeight by remember { mutableStateOf(false) }; var editReps by remember { mutableStateOf(false) }; var showHelp by remember { mutableStateOf(false) }; var pickerMode by remember { mutableStateOf<String?>(null) }; var quickActions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val allComplete = session.exercises.all { it.sets.size >= it.targetSets }; val hasAnySets = session.exercises.any { it.sets.isNotEmpty() }
    val prStatus = remember(current.exercise.id, weight, reps, current.sets.size) { if (current.exercise.trackingType == "reps_only") null else vm.prStatus(current.exercise.id, weight, reps) }
    fun buzz(strong: Boolean = false) { if (vm.settings.haptics) haptic(context, strong) }
    LaunchedEffect(prFlash) { if (prFlash) { delay(1600); vm.clearPrFlash() } }
    val onLog = { if (!allComplete && nextSet <= current.targetSets) { vm.log(current.id, nextSet, if (current.exercise.trackingType == "reps_only") 0.0 else weight, reps); buzz(true) } }
    if (compact) {
        val pager = rememberPagerState(pageCount = { 2 })
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().background(Bg)) { page ->
            if (page == 0) CurrentSetCompact(session, current, currentIndex, previous, nextSet, weight, reps, increment, prStatus, prFlash, allComplete, hasAnySets,
                onWeight = { weight = (weight + it).coerceAtLeast(0.0); buzz() }, onReps = { reps = (reps + it).coerceAtLeast(0); buzz() }, onEditWeight = { editWeight = true }, onEditReps = { editReps = true }, onLog = onLog, onFinish = { vm.finish(); buzz(true) }, onUndo = { vm.undo(); buzz() }, onHelp = { showHelp = true }, onMenu = { quickActions = true })
            else OverviewCompact(session, currentIndex, vm::jump) { vm.finish() }
        }
    } else FullWorkout(session, current, currentIndex, previous, nextSet, weight, reps, increment, prStatus, prFlash, allComplete,
        onWeight = { weight = (weight + it).coerceAtLeast(0.0); buzz() }, onReps = { reps = (reps + it).coerceAtLeast(0); buzz() }, onEditWeight = { editWeight = true }, onEditReps = { editReps = true }, onLog = onLog, onUndo = { vm.undo() }, onFinish = { vm.finish() }, onHelp = { showHelp = true })
    if (editWeight) NumberEditDialog("Weight (kg)", fmt(weight), true, { editWeight = false }) { v -> v.toDoubleOrNull()?.let { weight = it.coerceAtLeast(0.0) }; editWeight = false }
    if (editReps) NumberEditDialog("Reps", reps.toString(), false, { editReps = false }) { v -> v.toIntOrNull()?.let { reps = it.coerceAtLeast(0) }; editReps = false }
    if (showHelp) ExerciseHelpDialog(current.exercise) { showHelp = false }
    if (quickActions) QuickActionsDialog(current, { quickActions = false }, { quickActions = false; if (nextSet <= current.targetSets) vm.skipSet(current.id, nextSet) }, { quickActions = false; vm.skipExercise(current.id) }, { quickActions = false; vm.adjustSets(current.id, 1) }, { quickActions = false; vm.adjustSets(current.id, -1) }, { quickActions = false; pickerMode = "add" }, { quickActions = false; pickerMode = "replace" }, { quickActions = false; vm.moveExercise(current.id, -1) }, { quickActions = false; vm.moveExercise(current.id, 1) }, { quickActions = false; vm.finish() })
    pickerMode?.let { mode -> ExercisePickerDialog(exercises, if (mode == "add") "Add exercise" else "Replace exercise", { pickerMode = null }) { picked -> if (mode == "add") vm.addExercise(picked.id) else vm.replaceExercise(current.id, picked.id); pickerMode = null } }
}

@Composable
private fun CurrentSetCompact(session: WorkoutSession, ex: SessionExercise, currentIndex: Int, previous: List<PreviousSet>, setNo: Int, weight: Double, reps: Int, increment: Double, prStatus: PrStatus?, prFlash: Boolean, canFinish: Boolean, canUndo: Boolean, onWeight: (Double) -> Unit, onReps: (Int) -> Unit, onEditWeight: () -> Unit, onEditReps: () -> Unit, onLog: () -> Unit, onFinish: () -> Unit, onUndo: () -> Unit, onHelp: () -> Unit, onMenu: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(ex.exercise.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1, modifier = Modifier.weight(1f))
            if (prFlash) Text("PR", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text("${currentIndex + 1}/${session.exercises.size}", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
            TextButton(onClick = onHelp, contentPadding = PaddingValues(4.dp), modifier = Modifier.size(34.dp)) { Text("?", color = Accent, fontWeight = FontWeight.Black) }
            TextButton(onClick = onMenu, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(34.dp)) { Text("⋮", color = Color.White, fontSize = 22.sp) }
        }
        Text(previousLine(previous, ex.exercise.trackingType), color = Muted, fontSize = 11.sp, maxLines = 1)
        Text("SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (ex.exercise.trackingType != "reps_only") Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { MiniButton("−${fmt(increment)}") { onWeight(-increment) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${fmt(weight)} KG", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditWeight)); PrHint(prStatus) }; MiniButton("+${fmt(increment)}") { onWeight(increment) } }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { MiniButton("−") { onReps(-1) }; Text("$reps REPS", color = Color.White, fontSize = if (ex.exercise.trackingType == "reps_only") 38.sp else 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditReps)); MiniButton("+") { onReps(1) } }
        Button(onClick = if (canFinish) onFinish else onLog, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(17.dp)) { Text(if (canFinish) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp) }
        if (canUndo) TextButton(onClick = onUndo, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(0.dp)) { Text("UNDO LAST", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PrHint(status: PrStatus?) { if (status == null) return; val text = when { status.wouldBePr && status.best == null -> "FIRST PR"; status.wouldBePr -> "NEW PR • ${fmt(status.best!!.weightKg)}kg × ${status.best.reps} to beat"; status.best != null && status.proposedWeight == status.best.weightKg -> "BEST @ ${fmt(status.best.weightKg)}KG: ${status.best.reps} REPS"; status.best != null -> "PR ${fmt(status.best.weightKg)}KG × ${status.best.reps}"; else -> "" }; if (text.isNotBlank()) Text(text, color = if (status.wouldBePr) Accent else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) }

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) { OutlinedButton(onClick = onClick, modifier = Modifier.size(width = 86.dp, height = 54.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, OutlineSoft), colors = ButtonDefaults.outlinedButtonColors(containerColor = GlassSoft)) { Text(label, color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun QuickActionsDialog(ex: SessionExercise, onDismiss: () -> Unit, onSkipSet: () -> Unit, onSkipExercise: () -> Unit, onAddSet: () -> Unit, onRemoveSet: () -> Unit, onAddExercise: () -> Unit, onReplaceExercise: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onFinish: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { Surface(color = SurfaceHigh, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp)) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("QUICK ACTIONS", color = Accent, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(8.dp)); ActionRow("Skip set", onSkipSet); ActionRow("Skip exercise", onSkipExercise); HorizontalDivider(color = OutlineSoft); ActionRow("Add set", onAddSet); ActionRow("Remove uncompleted set", onRemoveSet); ActionRow("Add exercise", onAddExercise); ActionRow("Replace exercise", onReplaceExercise, ex.sets.isEmpty()); ActionRow("Move exercise up", onMoveUp); ActionRow("Move exercise down", onMoveDown); HorizontalDivider(color = OutlineSoft); ActionRow("Finish workout", onFinish, danger = true); Spacer(Modifier.height(24.dp)) } } }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false) { TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label, color = if (!enabled) Muted else if (danger) Danger else Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 16.sp) } }

@Composable
private fun OverviewCompact(session: WorkoutSession, currentIndex: Int, onJump: (Int) -> Unit, onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(horizontal = 14.dp, vertical = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(session.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp); SectionLabel("WORKOUT OVERVIEW")
        session.exercises.forEachIndexed { i, e -> val skipped = e.sets.count { it.skipped }; Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (i == currentIndex) Glass else Color.Transparent).clickable { onJump(e.orderIndex) }.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(e.exercise.name, color = Color.White, fontWeight = if (i == currentIndex) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, modifier = Modifier.weight(1f)); Text(if (skipped > 0) "${e.sets.size}/${e.targetSets} · ${skipped}S" else "${e.sets.size}/${e.targetSets}", color = if (e.sets.size >= e.targetSets) Accent else Muted, fontSize = 12.sp) } }
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, OutlineSoft)) { Text("Finish early", color = Accent) }; Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun FullWorkout(session: WorkoutSession, ex: SessionExercise, currentIndex: Int, previous: List<PreviousSet>, setNo: Int, weight: Double, reps: Int, increment: Double, prStatus: PrStatus?, prFlash: Boolean, canFinish: Boolean, onWeight: (Double) -> Unit, onReps: (Int) -> Unit, onEditWeight: () -> Unit, onEditReps: () -> Unit, onLog: () -> Unit, onUndo: () -> Unit, onFinish: () -> Unit, onHelp: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(session.name.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp); Text(ex.exercise.name, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black) }; if (prFlash) Text("PR LOGGED", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black); Text("${currentIndex + 1}/${session.exercises.size}", color = Muted, modifier = Modifier.padding(start = 8.dp)); TextButton(onClick = onHelp) { Text("?", color = Accent, fontWeight = FontWeight.Black) } }
        GlassCard(Modifier.fillMaxWidth(), lime = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(previousLine(previous, ex.exercise.trackingType), color = Muted, fontSize = 12.sp); Text("SET ${setNo.coerceAtMost(ex.targetSets)} / ${ex.targetSets}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(12.dp))
            if (ex.exercise.trackingType != "reps_only") {
                Text("WEIGHT", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("${fmt(weight)} KG", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditWeight)); PrHint(prStatus)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { onWeight(-increment) }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, OutlineSoft)) { Text("−${fmt(increment)}", color = Accent, fontWeight = FontWeight.Black) }; OutlinedButton(onClick = { onWeight(increment) }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, OutlineSoft)) { Text("+${fmt(increment)}", color = Accent, fontWeight = FontWeight.Black) } }
                Spacer(Modifier.height(10.dp))
            }
            Text("REPS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("$reps", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onEditReps))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { onReps(-1) }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, OutlineSoft)) { Text("−", color = Accent, fontWeight = FontWeight.Black) }; OutlinedButton(onClick = { onReps(1) }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, OutlineSoft)) { Text("+", color = Accent, fontWeight = FontWeight.Black) } }
        }
        Button(onClick = if (canFinish) onFinish else onLog, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) { Text(if (canFinish) "FINISH WORKOUT" else "LOG SET", color = Color.Black, fontSize = 19.sp, fontWeight = FontWeight.Black) }
        if (session.exercises.any { it.sets.isNotEmpty() }) TextButton(onClick = onUndo, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("UNDO LAST SET", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun NumberEditDialog(title: String, initial: String, decimal: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length))) }; val focusRequester = remember { FocusRequester() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number), modifier = Modifier.focusRequester(focusRequester)) }, confirmButton = { TextButton(onClick = { onSave(value.text) }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }); LaunchedEffect(Unit) { focusRequester.requestFocus(); value = value.copy(selection = TextRange(0, value.text.length)) }
}

@Composable
private fun ExercisePickerDialog(exercises: List<Exercise>, title: String, onDismiss: () -> Unit, onPick: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }; val filtered = exercises.filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) }.sortedWith(compareByDescending<Exercise> { it.favorite }.thenBy { it.category }.thenBy { it.name })
    Dialog(onDismissRequest = onDismiss) { Surface(color = SurfaceHigh, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp); TextButton(onClick = onDismiss) { Text("CLOSE", color = Muted) } }; OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search exercises") }); Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) { filtered.forEach { exercise -> Row(Modifier.fillMaxWidth().clickable { onPick(exercise) }.padding(vertical = 10.dp, horizontal = 4.dp)) { Column(Modifier.weight(1f)) { Text(if (exercise.favorite) "★ ${exercise.name}" else exercise.name, color = Color.White, fontWeight = FontWeight.Bold); Text("${exercise.category} • ${exercise.equipment}", color = Muted, fontSize = 11.sp) } }; HorizontalDivider(color = OutlineSoft) } } } } }
}

@Composable
private fun ExerciseHelpDialog(exercise: Exercise, onDismiss: () -> Unit) { Dialog(onDismissRequest = onDismiss) { Surface(color = SurfaceHigh, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(exercise.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center); Text("MOVEMENT DIAGRAM • PLACEHOLDER", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { DiagramFrame("START"); Text("→", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Black); DiagramFrame("FINISH") }; Text(exercise.diagramHint.ifBlank { "Exercise-specific artwork will replace this placeholder. Use controlled form through a comfortable range of motion." }, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center); Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("BACK TO SET", color = Color.Black, fontWeight = FontWeight.Black) } } } } }

@Composable
private fun DiagramFrame(label: String) { Box(Modifier.size(width = 112.dp, height = 105.dp).clip(RoundedCornerShape(17.dp)).background(Bg), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("●", color = Accent, fontSize = 24.sp); Text("╱│╲", color = Color.White, fontSize = 19.sp); Text("╱ ╲", color = Color.White, fontSize = 19.sp); Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun PageHeader(title: String, subtitle: String, action: String? = null, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 12.sp) }; if (action != null && onAction != null) Button(onClick = onAction, shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) { Text(action, color = Color.Black, fontWeight = FontWeight.Black) } } }

@Composable
private fun ExercisesScreen(vm: MainViewModel, exercises: List<Exercise>) {
    var query by remember { mutableStateOf("") }; var editing by remember { mutableStateOf<Exercise?>(null) }; var creating by remember { mutableStateOf(false) }; val filtered = exercises.filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) || it.equipment.contains(query, true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("Exercises", "${exercises.size} movements in your library", "NEW") { creating = true }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search exercises") }, shape = RoundedCornerShape(17.dp))
        filtered.groupBy { it.category }.forEach { (category, items) -> SectionLabel(category.uppercase()); items.forEach { ex -> GlassCard(Modifier.fillMaxWidth().clickable { editing = ex }) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (ex.favorite) Color(0x222E4810) else Color(0xFF202323)), contentAlignment = Alignment.Center) { Text(if (ex.favorite) "★" else "◇", color = if (ex.favorite) Accent else Muted) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(ex.name, color = Color.White, fontWeight = FontWeight.Bold); Text("${ex.equipment} • ${if (ex.trackingType == "reps_only") "Reps only" else "Weight + reps"} • ${ex.defaultSets} sets", color = Muted, fontSize = 11.sp) }; Text("EDIT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black) } } } }
        Spacer(Modifier.height(8.dp))
    }
    if (creating) ExerciseEditorDialog(null, { creating = false }, { vm.saveExercise(it); creating = false })
    editing?.let { ex -> ExerciseEditorDialog(ex, { editing = null }, { vm.saveExercise(it); editing = null }, { vm.duplicateExercise(ex.id); editing = null }, { vm.deleteExercise(ex.id); editing = null }) }
}

@Composable
private fun ExerciseEditorDialog(exercise: Exercise?, onDismiss: () -> Unit, onSave: (ExerciseDraft) -> Unit, onDuplicate: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    var name by remember(exercise?.id) { mutableStateOf(exercise?.name ?: "") }; var category by remember(exercise?.id) { mutableStateOf(exercise?.category ?: "Chest") }; var equipment by remember(exercise?.id) { mutableStateOf(exercise?.equipment ?: "Dumbbell") }; var tracking by remember(exercise?.id) { mutableStateOf(exercise?.trackingType ?: "weight_reps") }; var favorite by remember(exercise?.id) { mutableStateOf(exercise?.favorite ?: false) }; var sets by remember(exercise?.id) { mutableIntStateOf(exercise?.defaultSets ?: 3) }; var incrementText by remember(exercise?.id) { mutableStateOf(exercise?.incrementKg?.let(::fmt) ?: "") }; var hint by remember(exercise?.id) { mutableStateOf(exercise?.diagramHint ?: "") }
    Dialog(onDismissRequest = onDismiss) { Surface(color = SurfaceHigh, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)) { Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(if (exercise == null) "NEW EXERCISE" else "EDIT EXERCISE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black); OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(category, { category = it }, label = { Text("Muscle group") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment") }, modifier = Modifier.fillMaxWidth(), singleLine = true); SectionLabel("TRACKING"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = tracking == "weight_reps", onClick = { tracking = "weight_reps" }, label = { Text("Weight + reps") }); FilterChip(selected = tracking == "reps_only", onClick = { tracking = "reps_only" }, label = { Text("Reps only") }) }; if (tracking == "weight_reps") OutlinedTextField(incrementText, { incrementText = it }, label = { Text("Own increment kg (blank = global)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Default sets", color = Color.White); Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { sets = (sets - 1).coerceAtLeast(1) }) { Text("−") }; Text("$sets", color = Color.White, fontWeight = FontWeight.Black); TextButton(onClick = { sets += 1 }) { Text("+") } } }; SettingSwitch("Favourite", favorite) { favorite = it }; OutlinedTextField(hint, { hint = it }, label = { Text("Diagram / form hint") }, modifier = Modifier.fillMaxWidth(), minLines = 2); Button(enabled = name.isNotBlank(), onClick = { onSave(ExerciseDraft(exercise?.id, name.trim(), category.trim().ifBlank { "Other" }, equipment.trim().ifBlank { "Other" }, tracking, incrementText.toDoubleOrNull(), favorite, hint.trim(), sets)) }, modifier = Modifier.fillMaxWidth()) { Text("SAVE", color = Color.Black, fontWeight = FontWeight.Black) }; if (exercise != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onDuplicate?.invoke() }, modifier = Modifier.weight(1f)) { Text("DUPLICATE", color = Accent) }; OutlinedButton(onClick = { onDelete?.invoke() }, modifier = Modifier.weight(1f)) { Text("DELETE", color = Danger) } }; TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CANCEL", color = Muted) } } } }
}

@Composable
private fun TemplatesScreen(vm: MainViewModel, templates: List<WorkoutTemplate>, exercises: List<Exercise>) {
    var selectedId by remember { mutableStateOf<Long?>(null) }; var creating by remember { mutableStateOf(false) }; var addToTemplateId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("Templates", "Build and edit your workouts", "NEW") { creating = true }
        templates.forEach { t -> GlassCard(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black); Text("${t.exercises.size} exercises • ${t.exercises.sumOf { it.defaultSets }} sets", color = Muted, fontSize = 12.sp) }; TextButton(onClick = { selectedId = t.id }) { Text("EDIT", color = Accent, fontWeight = FontWeight.Black) } } } }
        Spacer(Modifier.height(8.dp))
    }
    if (creating) TextEntryDialog("New template", "", { creating = false }) { name -> if (name.isNotBlank()) vm.createTemplate(name.trim()); creating = false }
    val selected = selectedId?.let { id -> templates.firstOrNull { it.id == id } }
    selected?.let { template -> TemplateEditorDialog(template, { selectedId = null }, { vm.renameTemplate(template.id, it) }, { vm.duplicateTemplate(template.id); selectedId = null }, { vm.deleteTemplate(template.id); selectedId = null }, { exerciseId, direction -> vm.moveTemplateExercise(template.id, exerciseId, direction) }, { exerciseId, delta -> vm.adjustTemplateSets(template.id, exerciseId, delta) }, { exerciseId -> vm.removeTemplateExercise(template.id, exerciseId) }, { addToTemplateId = template.id }) }
    addToTemplateId?.let { tid -> val template = templates.firstOrNull { it.id == tid }; ExercisePickerDialog(exercises, "Add to ${template?.name ?: "template"}", { addToTemplateId = null }) { exercise -> vm.addTemplateExercise(tid, exercise.id); addToTemplateId = null } }
}

@Composable
private fun TemplateEditorDialog(template: WorkoutTemplate, onDismiss: () -> Unit, onRename: (String) -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit, onMove: (Long, Int) -> Unit, onSets: (Long, Int) -> Unit, onRemove: (Long) -> Unit, onAdd: () -> Unit) {
    var rename by remember(template.id, template.name) { mutableStateOf(template.name) }
    Dialog(onDismissRequest = onDismiss) { Surface(color = SurfaceHigh, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp)) { Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { SectionLabel("EDIT TEMPLATE"); Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(rename, { rename = it }, modifier = Modifier.weight(1f), singleLine = true); TextButton(onClick = { if (rename.isNotBlank()) onRename(rename.trim()) }) { Text("SAVE") } }; template.exercises.sortedBy { it.orderIndex }.forEach { te -> Surface(color = Bg, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSoft), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) { Text(te.exercise.name, color = Color.White, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row { TextButton(onClick = { onMove(te.exercise.id, -1) }) { Text("↑") }; TextButton(onClick = { onMove(te.exercise.id, 1) }) { Text("↓") } }; Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { onSets(te.exercise.id, -1) }) { Text("−") }; Text("${te.defaultSets} sets", color = Color.White); TextButton(onClick = { onSets(te.exercise.id, 1) }) { Text("+") } }; TextButton(onClick = { onRemove(te.exercise.id) }) { Text("REMOVE", color = Muted, fontSize = 10.sp) } } } } }; Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ADD EXERCISE", color = Color.Black, fontWeight = FontWeight.Black) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) { Text("DUPLICATE", color = Accent) }; OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("DELETE", color = Danger) } }; TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE", color = Muted) } } } }
}

@Composable
private fun TextEntryDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) { var text by remember { mutableStateOf(initial) }; AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(text, { text = it }, singleLine = true) }, confirmButton = { TextButton(onClick = { onSave(text) }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }) }

@Composable
private fun HistoryHub(vm: MainViewModel, history: List<WorkoutSession>) {
    var prs by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(if (prs) "Personal Records" else "History", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black); Text(if (prs) "Your strongest recorded sets" else "Your completed sessions", color = Muted, fontSize = 12.sp) }
            FilterChip(selected = prs, onClick = { prs = !prs }, label = { Text(if (prs) "WORKOUTS" else "PRS") })
        }
        if (prs) PrsScreen(vm, history, embedded = true) else HistoryScreen(vm, history, embedded = true)
    }
}

@Composable
private fun HistoryScreen(vm: MainViewModel, history: List<WorkoutSession>, embedded: Boolean = false) {
    var selected by remember { mutableStateOf<WorkoutSession?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = if (embedded) 0.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { if (!embedded) PageHeader("History", "Your completed sessions"); if (history.isEmpty()) Text("No workouts yet.", color = Muted); history.forEach { session -> HistoryCard(session) { selected = session } }; Spacer(Modifier.height(8.dp)) }
    selected?.let { session ->
        val fresh = history.firstOrNull { it.id == session.id }
        if (fresh == null) selected = null
        else HistoryDetailDialog(
            session = fresh,
            onDismiss = { selected = null },
            onRename = { vm.renameHistoryWorkout(fresh.id, it) },
            onEditSet = { setId, weight, reps -> vm.updateHistorySet(setId, weight, reps) },
            onDeleteSet = { setId -> vm.deleteHistorySet(setId) },
            onDeleteWorkout = { vm.deleteHistoryWorkout(fresh.id); selected = null }
        )
    }
}

@Composable
private fun HistoryDetailDialog(
    session: WorkoutSession,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onEditSet: (Long, Double, Int) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onDeleteWorkout: () -> Unit
) {
    val logged = session.exercises.sumOf { e -> e.sets.count { !it.skipped } }
    val skipped = session.exercises.sumOf { e -> e.sets.count { it.skipped } }
    val volume = session.exercises.sumOf { e -> e.sets.filter { !it.skipped }.sumOf { it.weightKg * it.reps } }
    var rename by remember(session.id, session.name) { mutableStateOf(session.name) }
    var editing by remember { mutableStateOf<Pair<LoggedSet, Exercise>?>(null) }
    var deleteSet by remember { mutableStateOf<LoggedSet?>(null) }
    var confirmDeleteWorkout by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = GlassDeep, shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, GlassEdge), modifier = Modifier.fillMaxWidth().heightIn(max = 690.dp), shadowElevation = 18.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("WORKOUT DETAILS")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(rename, { rename = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Workout name") })
                    TextButton(onClick = { if (rename.isNotBlank()) onRename(rename.trim()) }) { Text("SAVE", color = Accent, fontWeight = FontWeight.Black) }
                }
                Text("$logged logged • $skipped skipped • ${fmt(volume)}kg volume", color = Muted)
                session.exercises.forEach { e ->
                    SectionLabel(e.exercise.name.uppercase())
                    if (e.sets.isEmpty()) Text("No sets", color = Muted)
                    else e.sets.sortedBy { it.setNumber }.forEach { set ->
                        Surface(color = GlassSoft, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, GlassEdge), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (set.skipped) "Set ${set.setNumber}  •  SKIPPED"
                                    else if (e.exercise.trackingType == "reps_only") "Set ${set.setNumber}  •  ${set.reps} reps"
                                    else "Set ${set.setNumber}  •  ${fmt(set.weightKg)}kg × ${set.reps}${if (set.isPr) "  •  PR" else ""}",
                                    color = if (set.isPr) Accent else Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold
                                )
                                if (!set.skipped) TextButton(onClick = { editing = set to e.exercise }) { Text("EDIT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                                TextButton(onClick = { deleteSet = set }) { Text("×", color = Danger, fontSize = 18.sp) }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { confirmDeleteWorkout = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0x66FF7474))) { Text("DELETE WORKOUT", color = Danger, fontWeight = FontWeight.Bold) }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("DONE", color = Color.Black, fontWeight = FontWeight.Black) }
            }
        }
    }
    editing?.let { (set, exercise) ->
        HistorySetEditDialog(set, exercise, { editing = null }) { weight, reps -> onEditSet(set.id, weight, reps); editing = null }
    }
    deleteSet?.let { set ->
        AlertDialog(onDismissRequest = { deleteSet = null }, containerColor = GlassDeep, title = { Text("Delete set?") }, text = { Text("This removes the set from this old workout and recalculates PR markers.") }, confirmButton = { TextButton(onClick = { onDeleteSet(set.id); deleteSet = null }) { Text("DELETE", color = Danger) } }, dismissButton = { TextButton(onClick = { deleteSet = null }) { Text("CANCEL") } })
    }
    if (confirmDeleteWorkout) AlertDialog(onDismissRequest = { confirmDeleteWorkout = false }, containerColor = GlassDeep, title = { Text("Delete workout?") }, text = { Text("This permanently removes this workout and all its logged sets.") }, confirmButton = { TextButton(onClick = { confirmDeleteWorkout = false; onDeleteWorkout() }) { Text("DELETE", color = Danger) } }, dismissButton = { TextButton(onClick = { confirmDeleteWorkout = false }) { Text("CANCEL") } })
}

@Composable
private fun HistorySetEditDialog(set: LoggedSet, exercise: Exercise, onDismiss: () -> Unit, onSave: (Double, Int) -> Unit) {
    var weight by remember(set.id) { mutableStateOf(fmt(set.weightKg)) }
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = GlassDeep, shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GlassEdge), modifier = Modifier.fillMaxWidth(), shadowElevation = 18.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("EDIT SET ${set.setNumber}")
                Text(exercise.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (exercise.trackingType != "reps_only") OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Button(onClick = { onSave(if (exercise.trackingType == "reps_only") 0.0 else (weight.toDoubleOrNull() ?: set.weightKg), reps.toIntOrNull() ?: set.reps) }, modifier = Modifier.fillMaxWidth()) { Text("SAVE CHANGES", color = Color.Black, fontWeight = FontWeight.Black) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CANCEL", color = Muted) }
            }
        }
    }
}

@Composable
private fun PrsScreen(vm: MainViewModel, history: List<WorkoutSession>, embedded: Boolean = false) {
    val bests = remember(history) { vm.personalBests() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = if (embedded) 0.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!embedded) PageHeader("Personal Records", "Highest weight, then most reps at that weight")
        if (bests.isEmpty()) Text("Log some weighted sets to create PRs.", color = Muted)
        bests.forEach { best -> GlassCard(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x222E4810)), contentAlignment = Alignment.Center) { Text("PR", color = Accent, fontWeight = FontWeight.Black, fontSize = 11.sp) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(best.exerciseName, color = Color.White, fontWeight = FontWeight.Black); Text("${best.category} • ${fmt(best.weightKg)}kg × ${best.reps}", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }; val oneRm = best.weightKg * (1.0 + best.reps / 30.0); Column(horizontalAlignment = Alignment.End) { Text("EST. 1RM", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("${fmt(oneRm)}kg", color = Color.White, fontWeight = FontWeight.Bold) } } } }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CompletionScreen(summary: CompletionSummary, compact: Boolean, onDone: () -> Unit) { Box(Modifier.fillMaxSize().background(Bg).safeDrawingPadding().padding(if (compact) 16.dp else 28.dp), contentAlignment = Alignment.Center) { GlassCard(Modifier.fillMaxWidth(), lime = true) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { SectionLabel("WORKOUT"); Text("COMPLETE", color = Accent, fontWeight = FontWeight.Black, fontSize = if (compact) 36.sp else 48.sp); Text(summary.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Stat("${summary.loggedSets}", "LOGGED"); Stat("${summary.skippedSets}", "SKIPPED"); Stat("${summary.prCount}", "PR${if (summary.prCount == 1) "" else "S"}") }; Text("${summary.loggedSets + summary.skippedSets} / ${summary.plannedSets} planned sets accounted for", color = Muted, fontSize = 12.sp); Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("DONE", color = Color.Black, fontWeight = FontWeight.Black) } } } } }

@Composable
private fun Stat(value: String, label: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black); Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun HistoryCard(session: WorkoutSession, onClick: (() -> Unit)?) {
    val logged = session.exercises.sumOf { e -> e.sets.count { !it.skipped } }; val skipped = session.exercises.sumOf { e -> e.sets.count { it.skipped } }; val complete = logged + skipped >= session.exercises.sumOf { it.targetSets }; val date = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.UK).format(Date(session.endedAt ?: session.startedAt)); val modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    GlassCard(modifier) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(session.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp); Text(date, color = Muted, fontSize = 10.sp); Text("$logged sets${if (skipped > 0) " • $skipped skipped" else ""}", color = Color.White, fontSize = 12.sp) }; Text(if (complete) "DONE" else "EARLY", color = if (complete) Accent else Muted, fontSize = 10.sp, fontWeight = FontWeight.Black) } }
}

private fun previousLine(previous: List<PreviousSet>, trackingType: String): String { if (previous.isEmpty()) return "FIRST TIME"; return "LAST " + previous.joinToString(" · ") { p -> if (trackingType == "reps_only") "${p.reps}" else "${fmt(p.weightKg)}×${p.reps}" } }
private fun fmt(value: Double): String = if (value == value.roundToInt().toDouble()) value.roundToInt().toString() else String.format(Locale.UK, "%.1f", value)
private fun haptic(context: Context, strong: Boolean) { val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java)?.defaultVibrator else { @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator } ?: return; if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 45L else 18L, if (strong) 150 else 70)) else { @Suppress("DEPRECATION") vibrator.vibrate(if (strong) 45L else 18L) } }
