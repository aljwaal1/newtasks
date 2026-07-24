package com.aljwaal.newtasks

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * مشغل تنبيه واحد وآمن لكل التطبيق.
 * لا يستخدم نغمة منبه الهاتف ولا يسمح باستمرار الصوت أو الاهتزاز دون حد زمني.
 */
object AlarmPlayer {
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val legacyFocusListener = AudioManager.OnAudioFocusChangeListener { }

    private var toneGenerator: ToneGenerator? = null
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
            startVibration(appContext, mode)
            when (mode) {
                AlarmSoundMode.VIBRATE_ONLY -> {
                    AppLog.write(appContext, "ALARM_ALERT_STARTED", "mode=vibrate_only")
                }

                AlarmSoundMode.GENTLE_ONCE -> {
                    requestAudioFocus(
                        appContext,
                        AudioManager.STREAM_NOTIFICATION,
                        alarmUsage = false
                    )
                    toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35)
                    handler.post {
                        synchronized(lock) {
                            if (active && sessionId == currentSession) {
                                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 900)
                                AppLog.write(
                                    appContext,
                                    "SOUND_STARTED",
                                    "mode=gentle_once duration=900"
                                )
                            }
                        }
                    }
                }

                AlarmSoundMode.NORMAL_ALARM -> {
                    requestAudioFocus(
                        appContext,
                        AudioManager.STREAM_ALARM,
                        alarmUsage = true
                    )
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 65)
                    scheduleNormalTone(appContext, currentSession)
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

    private fun scheduleNormalTone(context: Context, currentSession: Long) {
        val cycle = object : Runnable {
            override fun run() {
                synchronized(lock) {
                    if (!active || sessionId != currentSession) return
                    runCatching {
                        toneGenerator?.startTone(
                            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                            1_000
                        )
                    }.onFailure { error ->
                        AppLog.write(context, "SOUND_START_FAILED", error.message.orEmpty())
                    }
                    AppLog.write(context, "SOUND_CYCLE_STARTED", "mode=normal_alarm")
                    handler.postDelayed(this, 1_700L)
                }
            }
        }
        handler.post(cycle)
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

    private fun requestAudioFocus(context: Context, stream: Int, alarmUsage: Boolean) {
        runCatching {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(
                        if (alarmUsage) AudioAttributes.USAGE_ALARM
                        else AudioAttributes.USAGE_NOTIFICATION
                    )
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

        runCatching { toneGenerator?.stopTone() }
        runCatching { toneGenerator?.release() }
        toneGenerator = null

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
