package com.example.jackvoiceassistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceAssistantService : Service(), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private val channelId = "jack_voice_channel"
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.9f)
                textToSpeech?.setPitch(0.7f)
                selectDeepMaleVoice()
                speak("Hello sir, I am listening")
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(this)
        startListening()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jack Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jack is listening")
            .setContentText("Say a command anytime")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun selectDeepMaleVoice() {
        val voices = textToSpeech?.voices ?: return
        val maleVoice = voices.firstOrNull {
            it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true)
        }
        if (maleVoice != null) {
            textToSpeech?.voice = maleVoice
        }
    }

    private fun muteSystemBeep() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            0
        )
    }

    private fun unmuteSystemBeep() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE,
            0
        )
    }

    private fun startListening() {
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 8000)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 8000)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 30000)
        }
        isListening = true
        speechRecognizer?.cancel()
        speechRecognizer?.startListening(intent)
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.firstOrNull()?.lowercase(Locale.US) ?: ""
        handleCommand(spokenText)
        android.os.Handler(mainLooper).postDelayed({
            startListening()
        }, 1000)
    }

    private fun handleCommand(command: String) {
        Logger.log("handleCommand received: '$command'")
        if (command.isBlank()) {
            Logger.log("Command was blank, ignoring")
            return
        }

        val appMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "settings" to "com.android.settings",
            "camera" to "com.android.camera",
            "gallery" to "com.google.android.apps.photos",
            "maps" to "com.google.android.apps.maps",
            "phone" to "com.android.dialer"
        )

        val matchedApp = appMap.entries.firstOrNull { command.contains(it.key) }
        Logger.log("startsWith('open')=${command.startsWith("open")}, matchedApp=${matchedApp?.key}")

        val typeTriggers = listOf("type for ", "type ", "search for ", "search ")
        val matchingTrigger = typeTriggers.firstOrNull { command.startsWith(it) }

        if (command.startsWith("open") && matchedApp != null) {
            speak("Yes sir, opening ${matchedApp.key}")
            launchApp(matchedApp.value, matchedApp.key)
        } else if (matchingTrigger != null) {
            val textToType = command.removePrefix(matchingTrigger).trim()
            val serviceInstance = MyAccessibilityService.instance
            if (serviceInstance == null) {
                speak("Service is null")
            } else {
                val typed = serviceInstance.typeTextIntoFocusedField(textToType)
                if (typed) {
                    speak("Typed $textToType, sir")
                } else {
                    speak("Found no text box")
                }
            }
        } else {
            speak("You said: $command")
        }
}
    }

    private fun launchApp(packageName: String, appLabel: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            speak("Sorry sir, $appLabel is not installed")
        }
    }

    override fun onError(error: Int) {
        isListening = false
        val delayMillis = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 300L
            else -> 1500L
        }
        android.os.Handler(mainLooper).postDelayed({
            startListening()
        }, delayMillis)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
