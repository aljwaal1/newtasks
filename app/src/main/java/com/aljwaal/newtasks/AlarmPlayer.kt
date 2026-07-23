package com.aljwaal.newtasks

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/** تشغيل صوت المنبه والاهتزاز دون تنفيذ prepare() المتزامن على الخيط الرئيسي. */
object AlarmPlayer {
    private val lock = Any()
    private val legacyFocusListener = AudioManager.OnAudioFocusChangeListener { }

    private var mediaPlayer: MediaPlayer? = null
    private var preparing = false
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: Any? = null

    fun start(context: Context) = synchronized(lock) {
        if (mediaPlayer != null || preparing) {
            AppLog.write(context, "ALARM_PLAYER_ALREADY_RUNNING", "preparing=$preparing")
            return@synchronized
        }

        val appContext = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        requestAudioFocus(appContext, attributes)
        startSoundAsync(appContext, attributes)
        startVibration(appContext)
    }

    private fun startSoundAsync(context: Context, attributes: AudioAttributes) {
        preparing = true
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: error("لا يوجد صوت منبه في النظام")

            val candidate = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(context, uri)
                isLooping = true
                setOnPreparedListener { preparedPlayer ->
                    synchronized(lock) {
                        if (mediaPlayer !== preparedPlayer) {
                            runCatching { preparedPlayer.release() }
                            return@setOnPreparedListener
                        }
                        preparing = false
                        runCatching { preparedPlayer.start() }
                            .onSuccess { AppLog.write(context, "SOUND_STARTED", "uri=$uri async=true") }
                            .onFailure { error ->
                                AppLog.write(
                                    context,
                                    "SOUND_START_FAILED",
                                    "${error.javaClass.simpleName}: ${error.message}"
                                )
                                releasePlayerLocked(preparedPlayer)
                            }
                    }
                }
                setOnErrorListener { failedPlayer, what, extra ->
                    synchronized(lock) {
                        AppLog.write(context, "SOUND_PLAYER_ERROR", "what=$what extra=$extra")
                        if (mediaPlayer === failedPlayer) releasePlayerLocked(failedPlayer)
                    }
                    true
                }
            }
            mediaPlayer = candidate
            candidate.prepareAsync()
            AppLog.write(context, "SOUND_PREPARING", "uri=$uri")
        }.onFailure { error ->
            preparing = false
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
            AppLog.write(
                context,
                "SOUND_START_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
        }
    }

    private fun startVibration(context: Context) {
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Api31.vibrator(context)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 700, 350, 700, 350)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Api26.vibrate(vibrator, pattern)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            AppLog.write(context, "VIBRATION_STARTED", "legacy=${Build.VERSION.SDK_INT < 26}")
        }.onFailure { error ->
            AppLog.write(context, "VIBRATION_START_FAILED", error.message.orEmpty())
        }
    }

    private fun requestAudioFocus(context: Context, attributes: AudioAttributes) {
        runCatching {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = Api26.requestAudioFocus(audioManager, attributes)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    legacyFocusListener,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        }.onFailure { error ->
            AppLog.write(context, "AUDIO_FOCUS_REQUEST_FAILED", error.message.orEmpty())
        }
    }

    fun stop(context: Context) = synchronized(lock) {
        preparing = false
        mediaPlayer?.setOnPreparedListener(null)
        mediaPlayer?.setOnErrorListener(null)
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

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
        AppLog.write(context, "ALARM_PLAYER_STOPPED")
    }

    private fun releasePlayerLocked(player: MediaPlayer) {
        preparing = false
        runCatching { player.release() }
        if (mediaPlayer === player) mediaPlayer = null
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

        fun vibrate(vibrator: Vibrator?, pattern: LongArray) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31 {
        fun vibrator(context: Context): Vibrator =
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
    }
}
