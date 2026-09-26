package com.simontester.flipfit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

data class BlindLevel(val small: Int, val big: Int)

class TptPokerTimerActivity : ComponentActivity() {

    companion object {
        private const val RECORD_AUDIO_REQUEST = 7001
        private const val PREFS = "tpt_poker_timer"
        private const val PREFIX_START = "start_"
        private const val PREFIX_NEAR = "near_"
        private const val PREFIX_END = "end_"
        private const val PREFIX_SHOUT = "shout_"
        private const val DEFAULT_BLINDS =
            "25/50, 50/100, 75/150, 100/200, 150/300, 200/400, 300/600, 400/800, 600/1200, 800/1600, 1000/2000, 1500/3000, 2000/4000"
    }

    private val bg = Color.rgb(7, 8, 10)
    private val card = Color.rgb(20, 22, 26)
    private val card2 = Color.rgb(30, 32, 37)
    private val white = Color.rgb(245, 245, 247)
    private val muted = Color.rgb(164, 167, 174)
    private val accent = Color.rgb(220, 38, 38)

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var voiceDir: File

    private var levelMinutes = 15
    private var warningSeconds = 60
    private var shoutAverageMinutes = 7
    private var levels = mutableListOf<BlindLevel>()

    private var levelIndex = 0
    private var remainingMs = 15L * 60_000L
    private var running = false
    private var endAtElapsed = 0L
    private var nearAnnouncementFired = false
    private var gameStartAnnouncementFired = false
    private var nextShoutAtElapsed = 0L

    private var timerText: TextView? = null
    private var blindsText: TextView? = null
    private var levelText: TextView? = null
    private var nextText: TextView? = null
    private var progress: ProgressBar? = null
    private var playPauseButton: Button? = null
    private var voiceStatusText: TextView? = null

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingPrefix: String? = null
    private var recordingStartedElapsed = 0L
    private var pendingRecordingPrefix: String? = null
    private var player: MediaPlayer? = null

    private var inSettings = false
    private var draftLevelMinutes = 15
    private var draftWarningSeconds = 60
    private var draftShoutAverageMinutes = 7
    private var draftBlindsText = DEFAULT_BLINDS

    private val tick = object : Runnable {
        override fun run() {
            if (running) {
                val now = SystemClock.elapsedRealtime()
                remainingMs = max(0L, endAtElapsed - now)

                if (!nearAnnouncementFired && remainingMs in 1..(warningSeconds * 1000L)) {
                    nearAnnouncementFired = true
                    playRandom(PREFIX_NEAR)
                }

                if (remainingMs <= 0L) {
                    playRandom(PREFIX_END)
                    advanceLevel(1, keepRunning = true)
                    scheduleNextShout()
                } else if (
                    shoutAverageMinutes > 0 &&
                    nextShoutAtElapsed > 0L &&
                    now >= nextShoutAtElapsed
                ) {
                    if (filesFor(PREFIX_SHOUT).isNotEmpty()) playRandom(PREFIX_SHOUT)
                    scheduleNextShout()
                }
            }
            if (!inSettings) updateTimerUi()
            handler.postDelayed(this, 250L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        voiceDir = File(filesDir, "tpt_voice").apply { mkdirs() }
        loadSettings()
        remainingMs = levelDurationMs()
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showTimer()
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        stopPlayer()
        stopRecording(save = false)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST) {
            val prefix = pendingRecordingPrefix
            pendingRecordingPrefix = null
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && prefix != null) {
                startRecording(prefix)
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required to record announcements.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadSettings() {
        levelMinutes = prefs.getInt("level_minutes", 15).coerceIn(5, 60)
        warningSeconds = prefs.getInt("warning_seconds", 60).coerceIn(15, 300)
        shoutAverageMinutes = prefs.getInt("shout_average", 7).coerceIn(0, 30)
        val stored = prefs.getString("blinds", DEFAULT_BLINDS) ?: DEFAULT_BLINDS
        levels = (parseBlinds(stored) ?: parseBlinds(DEFAULT_BLINDS)!!).toMutableList()
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt("level_minutes", levelMinutes)
            .putInt("warning_seconds", warningSeconds)
            .putInt("shout_average", shoutAverageMinutes)
            .putString("blinds", levels.joinToString(", ") { "${it.small}/${it.big}" })
            .apply()
    }

    private fun showTimer() {
        inSettings = false
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(12))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = text("TPT", 24f, Typeface.BOLD, white).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            gravity = Gravity.CENTER_VERTICAL
        }
        levelText = text("", 12f, Typeface.BOLD, muted).apply { gravity = Gravity.CENTER }
        val settings = button("SET", compact = true) { openSettings() }
        header.addView(brand)
        header.addView(levelText, LinearLayout.LayoutParams(dp(64), dp(38)))
        header.addView(settings, LinearLayout.LayoutParams(dp(58), dp(38)))
        root.addView(
            header,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
        )

        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(card, 18f)
            setPadding(dp(10))
        }
        blindsText = text("", 27f, Typeface.BOLD, white).apply { gravity = Gravity.CENTER }
        timerText = text("", 50f, Typeface.BOLD, white).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        nextText = text("", 12f, Typeface.BOLD, muted).apply { gravity = Gravity.CENTER }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(card2)
        }
        clockCard.addView(
            blindsText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34))
        )
        clockCard.addView(
            timerText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
        )
        clockCard.addView(
            nextText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24))
        )
        clockCard.addView(
            progress,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
        )
        root.addView(
            clockCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(130)
            ).apply { topMargin = dp(5) }
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val previous = button("◀", compact = true) {
            advanceLevel(-1, keepRunning = running)
        }
        playPauseButton = button("START", compact = false) { toggleRunning() }
        val next = button("▶", compact = true) {
            advanceLevel(1, keepRunning = running)
        }
        controls.addView(
            previous,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) }
        )
        controls.addView(
            playPauseButton,
            LinearLayout.LayoutParams(0, dp(48), 1.6f).apply { marginEnd = dp(6) }
        )
        controls.addView(next, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(
            controls,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply { topMargin = dp(7) }
        )

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottom.addView(
            button("RESET", compact = true) { resetTournament() },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(6) }
        )
        bottom.addView(
            button("SHOUT", compact = false, accentButton = true) {
                if (filesFor(PREFIX_SHOUT).isEmpty()) {
                    Toast.makeText(
                        this,
                        "Record a random shoutout first.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    playRandom(PREFIX_SHOUT)
                    scheduleNextShout()
                }
            },
            LinearLayout.LayoutParams(0, dp(44), 1.35f)
        )
        root.addView(
            bottom,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(49)
            ).apply { topMargin = dp(3) }
        )

        voiceStatusText = text("", 11f, Typeface.NORMAL, muted).apply {
            gravity = Gravity.CENTER
        }
        root.addView(
            voiceStatusText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(22))
        )

        setContentView(root)
        updateTimerUi()
    }

    private fun updateTimerUi() {
        val current = levels[levelIndex.coerceIn(0, levels.lastIndex)]
        val next = levels.getOrNull(levelIndex + 1)
        val sec = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
        timerText?.text = String.format(Locale.UK, "%02d:%02d", sec / 60, sec % 60)
        blindsText?.text = "${chips(current.small)} / ${chips(current.big)}"
        levelText?.text = "LEVEL ${levelIndex + 1}"
        nextText?.text = if (next != null) {
            "NEXT  ${chips(next.small)} / ${chips(next.big)}"
        } else {
            "FINAL LEVEL"
        }
        val duration = levelDurationMs().coerceAtLeast(1L)
        progress?.progress =
            ((remainingMs.coerceIn(0L, duration) * 1000L) / duration).toInt()
        playPauseButton?.text =
            if (running) "PAUSE"
            else if (remainingMs < duration) "RESUME"
            else "START"
        timerText?.setTextColor(
            if (remainingMs <= warningSeconds * 1000L) accent else white
        )
        val counts = listOf(
            filesFor(PREFIX_START).size,
            filesFor(PREFIX_NEAR).size,
            filesFor(PREFIX_END).size,
            filesFor(PREFIX_SHOUT).size
        )
        voiceStatusText?.text =
            "VOICE ${counts.take(3).sum()} EVENT • ${counts[3]} SHOUT • RANDOM " +
                if (shoutAverageMinutes == 0) "OFF" else "~${shoutAverageMinutes}m"
    }

    private fun toggleRunning() {
        if (running) {
            remainingMs = max(0L, endAtElapsed - SystemClock.elapsedRealtime())
            running = false
        } else {
            if (remainingMs <= 0L) remainingMs = levelDurationMs()
            endAtElapsed = SystemClock.elapsedRealtime() + remainingMs
            running = true
            if (
                !gameStartAnnouncementFired &&
                levelIndex == 0 &&
                remainingMs >= levelDurationMs() - 1000L
            ) {
                gameStartAnnouncementFired = true
                playRandom(PREFIX_START)
            }
            if (nextShoutAtElapsed == 0L) scheduleNextShout()
        }
        updateTimerUi()
    }

    private fun advanceLevel(delta: Int, keepRunning: Boolean) {
        val target = (levelIndex + delta).coerceIn(0, levels.lastIndex)
        levelIndex = target
        remainingMs = levelDurationMs()
        nearAnnouncementFired = false
        if (keepRunning) {
            running = true
            endAtElapsed = SystemClock.elapsedRealtime() + remainingMs
        } else {
            running = false
        }
        updateTimerUi()
    }

    private fun resetTournament() {
        running = false
        levelIndex = 0
        remainingMs = levelDurationMs()
        nearAnnouncementFired = false
        gameStartAnnouncementFired = false
        nextShoutAtElapsed = 0L
        stopPlayer()
        updateTimerUi()
        Toast.makeText(this, "Tournament reset.", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleNextShout() {
        if (shoutAverageMinutes <= 0 || filesFor(PREFIX_SHOUT).isEmpty()) {
            nextShoutAtElapsed = 0L
            return
        }
        val base = shoutAverageMinutes * 60_000L
        val jitter = Random.nextDouble(0.65, 1.35)
        val delay = (base * jitter).toLong().coerceAtLeast(30_000L)
        nextShoutAtElapsed = SystemClock.elapsedRealtime() + delay
    }

    private fun openSettings() {
        draftLevelMinutes = levelMinutes
        draftWarningSeconds = warningSeconds
        draftShoutAverageMinutes = shoutAverageMinutes
        draftBlindsText = levels.joinToString(", ") { "${it.small}/${it.big}" }
        showSettings()
    }

    private fun showSettings() {
        inSettings = true
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12))
            setBackgroundColor(bg)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            button("BACK", compact = true) { showTimer() },
            LinearLayout.LayoutParams(dp(74), dp(44))
        )
        top.addView(
            text("TPT SETTINGS", 19f, Typeface.BOLD, white).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )
        content.addView(top)

        content.addView(sectionTitle("TOURNAMENT"))
        content.addView(
            stepper(
                label = "Blind level",
                initial = draftLevelMinutes,
                suffix = " min",
                min = 5,
                max = 60,
                step = 5
            ) { draftLevelMinutes = it }
        )
        content.addView(
            stepper(
                label = "Near-end warning",
                initial = draftWarningSeconds,
                suffix = " sec",
                min = 15,
                max = 300,
                step = 15
            ) { draftWarningSeconds = it }
        )
        content.addView(
            stepper(
                label = "Random shoutouts",
                initial = draftShoutAverageMinutes,
                suffix = { if (it == 0) "OFF" else "~${it}m" },
                min = 0,
                max = 30,
                step = 1
            ) { draftShoutAverageMinutes = it }
        )

        content.addView(
            text("BLIND SCHEDULE", 11f, Typeface.BOLD, muted).apply {
                setPadding(0, dp(12), 0, dp(5))
            }
        )
        val blindInput = EditText(this).apply {
            setText(draftBlindsText)
            setTextColor(white)
            setHintTextColor(muted)
            textSize = 14f
            minLines = 3
            maxLines = 5
            gravity = Gravity.TOP
            background = rounded(card, 12f, stroke = true)
            setPadding(dp(10))
            hint = "25/50, 50/100, 75/150..."
            addTextChangedListener(SimpleTextWatcher { draftBlindsText = it })
        }
        content.addView(
            blindInput,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(96))
        )

        content.addView(sectionTitle("VOICE ANNOUNCEMENTS"))
        content.addView(
            text(
                "Record as many clips as you want. Event clips are chosen at random. Everything stays on this phone.",
                12f,
                Typeface.NORMAL,
                muted
            ).apply { setPadding(0, 0, 0, dp(6)) }
        )

        content.addView(
            recordingSection(
                title = "GAME START",
                subtitle = "Played when Level 1 starts.",
                prefix = PREFIX_START
            )
        )
        content.addView(
            recordingSection(
                title = "NEAR LEVEL END",
                subtitle = "Played when the warning threshold is reached.",
                prefix = PREFIX_NEAR
            )
        )
        content.addView(
            recordingSection(
                title = "LEVEL END",
                subtitle = "Played as blinds move to the next level.",
                prefix = PREFIX_END
            )
        )
        content.addView(
            recordingSection(
                title = "RANDOM SHOUTOUTS",
                subtitle = "Used by SHOUT and the random timer.",
                prefix = PREFIX_SHOUT
            )
        )

        val save = button("SAVE SETTINGS", compact = false, accentButton = true) {
            val parsed = parseBlinds(draftBlindsText)
            if (parsed == null) {
                Toast.makeText(
                    this,
                    "Blind schedule must look like 25/50, 50/100, 75/150.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val oldDuration = levelDurationMs()
                levelMinutes = draftLevelMinutes
                warningSeconds = draftWarningSeconds
                shoutAverageMinutes = draftShoutAverageMinutes
                levels = parsed.toMutableList()
                levelIndex = levelIndex.coerceIn(0, levels.lastIndex)
                val newDuration = levelDurationMs()
                if (!running && remainingMs >= oldDuration - 1000L) {
                    remainingMs = newDuration
                } else {
                    remainingMs = remainingMs.coerceAtMost(newDuration)
                    if (running) {
                        endAtElapsed = SystemClock.elapsedRealtime() + remainingMs
                    }
                }
                nearAnnouncementFired = remainingMs <= warningSeconds * 1000L
                saveSettings()
                scheduleNextShout()
                showTimer()
            }
        }
        content.addView(
            save,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(20)
            }
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(content)
        }
        setContentView(scroll)
    }

    private fun recordingSection(title: String, subtitle: String, prefix: String): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14f)
            setPadding(dp(10))
        }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 13f, Typeface.BOLD, white))
            addView(text(subtitle, 11f, Typeface.NORMAL, muted))
        }
        head.addView(
            textBox,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val isThisRecording = recordingPrefix == prefix
        val recLabel = if (isThisRecording) "STOP" else "REC"
        val recButton = button(
            recLabel,
            compact = true,
            accentButton = isThisRecording
        ) {
            if (isThisRecording) {
                stopRecording(save = true)
                showSettings()
            } else if (recordingPrefix != null) {
                Toast.makeText(
                    this,
                    "Stop the current recording first.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                requestOrStartRecording(prefix)
            }
        }
        head.addView(
            recButton,
            LinearLayout.LayoutParams(dp(68), dp(42)).apply { marginStart = dp(6) }
        )
        box.addView(head)

        val clips = filesFor(prefix)
        if (clips.isEmpty()) {
            box.addView(
                text("No recordings yet.", 11f, Typeface.NORMAL, muted).apply {
                    setPadding(0, dp(7), 0, 0)
                }
            )
        } else {
            val playRandomButton = button(
                "PLAY RANDOM (${clips.size})",
                compact = true
            ) { playRandom(prefix) }
            box.addView(
                playRandomButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(40)
                ).apply { topMargin = dp(7) }
            )
            clips.take(8).forEachIndexed { index, file ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val stamp = SimpleDateFormat(
                    "dd MMM HH:mm",
                    Locale.UK
                ).format(Date(file.lastModified()))
                row.addView(
                    text(
                        "Clip ${clips.size - index}  •  $stamp",
                        11f,
                        Typeface.NORMAL,
                        muted
                    ),
                    LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                )
                row.addView(
                    button("PLAY", compact = true) { playFile(file) },
                    LinearLayout.LayoutParams(dp(62), dp(36)).apply {
                        marginEnd = dp(5)
                    }
                )
                row.addView(
                    button("DEL", compact = true) {
                        if (recordingFile?.absolutePath != file.absolutePath && file.delete()) {
                            showSettings()
                        }
                    },
                    LinearLayout.LayoutParams(dp(54), dp(36))
                )
                box.addView(row)
            }
            if (clips.size > 8) {
                box.addView(
                    text(
                        "${clips.size - 8} older clips hidden from this list.",
                        10f,
                        Typeface.NORMAL,
                        muted
                    )
                )
            }
        }

        return box.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
    }

    private fun requestOrStartRecording(prefix: String) {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startRecording(prefix)
        } else {
            pendingRecordingPrefix = prefix
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecording(prefix: String) {
        stopPlayer()
        if (recordingPrefix != null) return
        val file = File(voiceDir, "$prefix${System.currentTimeMillis()}.m4a")
        try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(128_000)
            newRecorder.setAudioSamplingRate(44_100)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            recordingFile = file
            recordingPrefix = prefix
            recordingStartedElapsed = SystemClock.elapsedRealtime()
            Toast.makeText(
                this,
                "Recording… tap STOP when finished.",
                Toast.LENGTH_SHORT
            ).show()
            showSettings()
        } catch (_: Exception) {
            file.delete()
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            recordingFile = null
            recordingPrefix = null
            Toast.makeText(
                this,
                "Could not start recording.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopRecording(save: Boolean) {
        val file = recordingFile
        val duration = SystemClock.elapsedRealtime() - recordingStartedElapsed
        val r = recorder
        recorder = null
        recordingFile = null
        recordingPrefix = null
        if (r != null) {
            try {
                r.stop()
            } catch (_: Exception) {
                file?.delete()
            } finally {
                try { r.release() } catch (_: Exception) {}
            }
        }
        if (!save || duration < 500L) file?.delete()
    }

    private fun filesFor(prefix: String): List<File> =
        voiceDir.listFiles()
            ?.filter {
                it.isFile &&
                    it.name.startsWith(prefix) &&
                    it.extension.equals("m4a", true)
            }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    private fun playRandom(prefix: String) {
        val options = filesFor(prefix)
        if (options.isEmpty()) return
        playFile(options.random())
    }

    private fun playFile(file: File) {
        stopPlayer()
        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stopPlayer() }
                setOnErrorListener { _, _, _ ->
                    stopPlayer()
                    true
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            stopPlayer()
            Toast.makeText(
                this,
                "Could not play that recording.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopPlayer() {
        val p = player
        player = null
        if (p != null) {
            try { p.stop() } catch (_: Exception) {}
            try { p.release() } catch (_: Exception) {}
        }
    }

    private fun parseBlinds(raw: String): List<BlindLevel>? {
        val parts = raw
            .split(Regex("[,;\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val parsed = mutableListOf<BlindLevel>()
        for (part in parts) {
            val pair = part.split("/")
            if (pair.size != 2) return null
            val small = pair[0].trim().replace(",", "").toIntOrNull() ?: return null
            val big = pair[1].trim().replace(",", "").toIntOrNull() ?: return null
            if (small <= 0 || big <= small) return null
            parsed += BlindLevel(small, big)
        }
        return parsed.take(30)
    }

    private fun levelDurationMs(): Long = levelMinutes * 60_000L

    private fun chips(value: Int): String =
        NumberFormat.getIntegerInstance(Locale.UK).format(value)

    private fun sectionTitle(label: String): TextView =
        text(label, 11f, Typeface.BOLD, muted).apply {
            setPadding(0, dp(14), 0, dp(6))
        }

    private fun stepper(
        label: String,
        initial: Int,
        suffix: String,
        min: Int,
        max: Int,
        step: Int,
        onChange: (Int) -> Unit
    ): View = stepper(label, initial, { "$it$suffix" }, min, max, step, onChange)

    private fun stepper(
        label: String,
        initial: Int,
        suffix: (Int) -> String,
        min: Int,
        max: Int,
        step: Int,
        onChange: (Int) -> Unit
    ): View {
        var value = initial
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(card, 12f)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        row.addView(
            text(label, 13f, Typeface.BOLD, white),
            LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        )
        val valueText = text(
            suffix(value),
            13f,
            Typeface.BOLD,
            white
        ).apply { gravity = Gravity.CENTER }
        row.addView(
            button("−", compact = true) {
                value = (value - step).coerceAtLeast(min)
                valueText.text = suffix(value)
                onChange(value)
            },
            LinearLayout.LayoutParams(dp(44), dp(40))
        )
        row.addView(valueText, LinearLayout.LayoutParams(dp(76), dp(40)))
        row.addView(
            button("+", compact = true) {
                value = (value + step).coerceAtMost(max)
                valueText.text = suffix(value)
                onChange(value)
            },
            LinearLayout.LayoutParams(dp(44), dp(40))
        )
        return row.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { bottomMargin = dp(6) }
        }
    }

    private fun text(
        value: String,
        size: Float,
        weight: Int,
        color: Int
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create(Typeface.DEFAULT, weight)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun button(
        label: String,
        compact: Boolean,
        accentButton: Boolean = false,
        action: () -> Unit
    ): Button = Button(this).apply {
        text = label
        textSize = if (compact) 11f else 13f
        setTextColor(white)
        isAllCaps = false
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        background = rounded(if (accentButton) accent else card2, 12f)
        setPadding(dp(6), 0, dp(6), 0)
        minHeight = 0
        minWidth = 0
        setOnClickListener { action() }
    }

    private fun rounded(
        color: Int,
        radiusDp: Float,
        stroke: Boolean = false
    ): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke) setStroke(dp(1), Color.rgb(55, 58, 64))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).toInt()
}

private class SimpleTextWatcher(
    val after: (String) -> Unit
) : android.text.TextWatcher {
    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) = Unit

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) = Unit

    override fun afterTextChanged(s: android.text.Editable?) {
        after(s?.toString().orEmpty())
    }
}
