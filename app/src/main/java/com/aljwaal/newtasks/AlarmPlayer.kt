package com.aljwaal.newtasks

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * مشغل تنبيه واحد وآمن لكل التطبيق.
 * يولد الصوت محليًا عبر AudioTrack بدل ToneGenerator أو نغمة منبه الهاتف.
 */
object AlarmPlayer {
    private const val SAMPLE_RATE = 16_000

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val legacyFocusListener = AudioManager.OnAudioFocusChangeListener { }

    private var audioTrack: AudioTrack? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: Any? = null
    private var active = false
    private var sessionId = 0L

    fun maxDurationMillis(context: Context): Long =
        maxDurationMillis(AppPreferences.alarmSoundMode(context.applicationContext))

    private fun maxDurationMillis(mode: AlarmSoundMode): Long = when (mode) {
        AlarmSoundMode.VIBRATE_ONLY -> 10_000L
        AlarmSoundMode.GENTLE_ONCE -> 3_000L
        AlarmSoundMode.NORMAL_ALARM -> 30_000L
    }

    fun start(context: Context) = synchronized(lock) {
        if (active) {
            AppLog.write(context, "ALARM_PLAYER_ALREADY_RUNNING", "session=$sessionId")
            return@synchronized
        }

        val appContext = context.applicationContext
        val mode = AppPreferences.alarmSoundMode(appContext)
        active = true
        sessionId++
        val currentSession = sessionId

        runCatching {
            logAudioState(appContext, mode)
            startVibration(appContext, mode)

            when (mode) {
                AlarmSoundMode.VIBRATE_ONLY -> {
                    AppLog.write(appContext, "ALARM_ALERT_STARTED", "mode=vibrate_only")
                }

                AlarmSoundMode.GENTLE_ONCE -> {
                    requestAudioFocus(appContext, AudioManager.STREAM_ALARM)
                    startGeneratedTone(
                        context = appContext,
                        samples = gentleSamples(),
                        looping = false,
                        volume = 0.42f,
                        mode = mode
                    )
                }

                AlarmSoundMode.NORMAL_ALARM -> {
                    requestAudioFocus(appContext, AudioManager.STREAM_ALARM)
                    startGeneratedTone(
                        context = appContext,
                        samples = normalAlarmSamples(),
                        looping = true,
                        volume = 0.78f,
                        mode = mode
                    )
                }
            }

            scheduleAutoStop(
                appContext,
                currentSession,
                maxDurationMillis(mode)
            )
            AppLog.write(
                appContext,
                "ALARM_ALERT_MODE_STARTED",
                "mode=${mode.storageValue} maxDuration=${maxDurationMillis(mode)}"
            )
        }.onFailure { error ->
            AppLog.write(
                appContext,
                "ALARM_PLAYER_START_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
            stopLocked(appContext, reason = "start_failure")
        }
    }

    private fun startGeneratedTone(
        context: Context,
        samples: ShortArray,
        looping: Boolean,
        volume: Float,
        mode: AlarmSoundMode
    ) {
        val requiredBytes = samples.size * Short.SIZE_BYTES
        val minimumBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(0)
        val bufferBytes = max(requiredBytes, minimumBytes)

        @Suppress("DEPRECATION")
        val track = AudioTrack(
            AudioManager.STREAM_ALARM,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
            AudioTrack.MODE_STATIC
        )

        check(track.state == AudioTrack.STATE_INITIALIZED) {
            "AudioTrack initialization failed"
        }

        val written = track.write(samples, 0, samples.size)
        check(written > 0) { "AudioTrack write failed: $written" }

        track.setVolume(volume.coerceIn(0f, 1f))
        if (looping) {
            val loopResult = track.setLoopPoints(0, written, -1)
            check(loopResult == AudioTrack.SUCCESS) {
                "AudioTrack loop setup failed: $loopResult"
            }
        }

        audioTrack = track
        track.play()

        AppLog.write(
            context,
            "SOUND_STARTED",
            "engine=AudioTrack mode=${mode.storageValue} looping=$looping samples=$written volume=$volume"
        )
    }

    private fun gentleSamples(): ShortArray {
        val durationSeconds = 1.10
        val count = (SAMPLE_RATE * durationSeconds).toInt()
        return ShortArray(count) { index ->
            val time = index.toDouble() / SAMPLE_RATE
            var value = 0.0
            value += decayingTone(time, start = 0.00, frequency = 660.0, amplitude = 0.52)
            value += decayingTone(time, start = 0.32, frequency = 880.0, amplitude = 0.42)
            pcm16(value)
        }
    }

    private fun normalAlarmSamples(): ShortArray {
        val durationSeconds = 1.45
        val count = (SAMPLE_RATE * durationSeconds).toInt()
        val starts = doubleArrayOf(0.00, 0.38, 0.76)
        val beepDuration = 0.24

        return ShortArray(count) { index ->
            val time = index.toDouble() / SAMPLE_RATE
            var value = 0.0
            starts.forEach { start ->
                val local = time - start
                if (local in 0.0..beepDuration) {
                    val envelope = sin(PI * local / beepDuration)
                    value += 0.72 * envelope * (
                        sin(2.0 * PI * 760.0 * local) +
                            0.20 * sin(2.0 * PI * 1_140.0 * local)
                        )
                }
            }
            pcm16(value)
        }
    }

    private fun decayingTone(
        time: Double,
        start: Double,
        frequency: Double,
        amplitude: Double
    ): Double {
        if (time < start) return 0.0
        val local = time - start
        val envelope = exp(-5.3 * local)
        return amplitude * envelope * (
            sin(2.0 * PI * frequency * local) +
                0.18 * sin(2.0 * PI * frequency * 2.0 * local)
            )
    }

    private fun pcm16(value: Double): Short =
        (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun logAudioState(context: Context, mode: AlarmSoundMode) {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maximum = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val muted = current <= 0
        AppLog.write(
            context,
            "ALARM_AUDIO_STATE",
            "mode=${mode.storageValue} alarmVolume=$current/$maximum muted=$muted ringerMode=${manager.ringerMode}"
        )
        if (muted && mode != AlarmSoundMode.VIBRATE_ONLY) {
            AppLog.write(context, "ALARM_SOUND_MUTED_BY_DEVICE", "alarmVolume=0")
        }
    }

    private fun scheduleAutoStop(
        context: Context,
        currentSession: Long,
        delayMillis: Long
    ) {
        handler.postDelayed({
            synchronized(lock) {
                if (active && sessionId == currentSession) {
                    stopLocked(context, reason = "timeout")
                }
            }
        }, delayMillis)
    }

    private fun startVibration(context: Context, mode: AlarmSoundMode) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31.vibrator(context)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = when (mode) {
            AlarmSoundMode.VIBRATE_ONLY -> longArrayOf(0, 500, 350, 500, 800)
            AlarmSoundMode.GENTLE_ONCE -> longArrayOf(0, 250, 120, 250)
            AlarmSoundMode.NORMAL_ALARM -> longArrayOf(0, 600, 300, 600, 500)
        }
        val repeat = when (mode) {
            AlarmSoundMode.GENTLE_ONCE -> -1
            AlarmSoundMode.VIBRATE_ONLY,
            AlarmSoundMode.NORMAL_ALARM -> 0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Api26.vibrate(vibrator, pattern, repeat)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, repeat)
        }
        AppLog.write(
            context,
            "VIBRATION_STARTED",
            "mode=${mode.storageValue} repeat=$repeat legacy=${Build.VERSION.SDK_INT < 26}"
        )
    }

    private fun requestAudioFocus(context: Context, stream: Int) {
        runCatching {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                focusRequest = Api26.requestAudioFocus(audioManager, attributes)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    legacyFocusListener,
                    stream,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        }.onFailure { error ->
            AppLog.write(context, "AUDIO_FOCUS_REQUEST_FAILED", error.message.orEmpty())
        }
    }

    fun stop(context: Context) = synchronized(lock) {
        stopLocked(context.applicationContext, reason = "user")
    }

    private fun stopLocked(context: Context, reason: String) {
        active = false
        sessionId++
        handler.removeCallbacksAndMessages(null)

        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.release() }
        audioTrack = null

        runCatching { vibrator?.cancel() }
        vibrator = null

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Api26.abandonAudioFocus(audioManager, focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(legacyFocusListener)
            }
        }
        focusRequest = null
        audioManager = null
        AppLog.write(context, "ALARM_PLAYER_STOPPED", "reason=$reason")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private object Api26 {
        fun requestAudioFocus(
            manager: AudioManager?,
            attributes: AudioAttributes
        ): AudioFocusRequest? {
            if (manager == null) return null
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            manager.requestAudioFocus(request)
            return request
        }

        fun abandonAudioFocus(manager: AudioManager?, request: Any?) {
            if (manager != null && request is AudioFocusRequest) {
                manager.abandonAudioFocusRequest(request)
            }
        }

        fun vibrate(vibrator: Vibrator?, pattern: LongArray, repeat: Int) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31 {
        fun vibrator(context: Context): Vibrator =
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
    }
}
