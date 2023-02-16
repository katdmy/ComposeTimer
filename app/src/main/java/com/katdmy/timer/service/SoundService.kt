package com.katdmy.timer.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.*
import android.os.Binder
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.katdmy.timer.MainActivity
import com.katdmy.timer.R
import java.util.*

class SoundService: Service() {

    private val binder = SoundServiceBinder()
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private lateinit var tts: TextToSpeech
    private var ttsEnabled: Boolean = false

    private var mp0: MediaPlayer? = null
    private var mp1: MediaPlayer? = null
    private var mp2: MediaPlayer? = null

    private val listeners = HashSet<Listener>()


    interface Listener {
        fun onClosingSoundEnded()
    }

    fun registerListener(listener: Listener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: Listener) {
        listeners.remove(listener)
    }

    inner class SoundServiceBinder: Binder() {
        fun getService(): SoundService = this@SoundService
    }

    override fun onBind(p0: Intent?): IBinder {
        Log.e("SERVICE", "onBind")
        initialisation()
        startForeground(1, createNotification())
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.e("SERVICE", "onUnbind")
        clearance()
        return super.onUnbind(intent)
    }

    private fun initialisation() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                build()
            }

        if (!ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsInitialized()
                    tts.language = Locale("en", "US")
                    tts.setPitch(1.3f)
                    tts.setSpeechRate(0.7f)
                    ttsEnabled = true
                }
            }
        }
        mp0 = MediaPlayer.create(this, R.raw.short_whistle)
        mp1 = MediaPlayer.create(this, R.raw.single_whistle)
        mp2 = MediaPlayer.create(this, R.raw.long_whistle).apply {
            setOnCompletionListener {
                for (listener in listeners) {
                    listener.onClosingSoundEnded()
                }
            }
        }
    }

    private fun clearance() {
        if (ttsEnabled) {
            ttsEnabled = false
            tts.stop()
            tts.shutdown()
        }
        mp0?.reset()
        mp0?.release()
        mp1?.reset()
        mp1?.release()
        mp2?.reset()
        mp2?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun ttsInitialized() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                audioManager.requestAudioFocus(focusRequest)
            }

            override fun onDone(utteranceId: String?) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        })
    }

    fun playWhistle(soundNum: Int) {
        when (soundNum) {
            0 -> mp0?.start()
            1 -> mp1?.start()
            2 -> mp2?.start()
            else -> {
                throw IllegalArgumentException("Illegal sound id")
            }
        }
    }

    fun playRoundName(number: Int) {
        if (ttsEnabled && number > 1)
            tts.speak("/ / / /  Round $number", TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = createNotificationChannel()

        return Notification.Builder(this, channelId)
            .setContentTitle("Audio Service")
            .setContentText("Playing audio notifications")
            .setSmallIcon(R.drawable.ic_audiotrack_black_24dp)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel(): String {
        val channelId = "Audio Service"
        val chan = NotificationChannel(
            channelId,
            "Audio Background Service", NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}