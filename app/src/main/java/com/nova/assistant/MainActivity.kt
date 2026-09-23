package com.nova.assistant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var chat: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var speech: SpeechRecognizer? = null
    private var torchOn = false

    private val creatorName = "Your Name"
    private val creatorTitle = "Creator & Developer"
    private val creatorBio = "Creator of Nova."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chat = findViewById(R.id.chat)
        input = findViewById(R.id.input)
        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.send).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                input.text.clear()
                respondTo(text)
            }
        }

        findViewById<Button>(R.id.mic).setOnClickListener {
            startVoiceMode()
        }

        findViewById<Button>(R.id.help).setOnClickListener {
            respondTo("help")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    private fun addLine(who: String, text: String) {
        chat.append("$who: $text\n\n")
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova")
    }

    private fun respondTo(raw: String) {
        addLine("You", raw)
        val command = raw.lowercase(Locale.getDefault()).trim()

        val response = when {
            command == "hi" || command == "hello" || command == "hey" ->
                "Hello! 👋 I'm Nova. How can I help you?"

            command.contains("who created you") || command.contains("who is your creator") ->
                "I was created by $creatorName. $creatorTitle. $creatorBio"

            command.contains("your name") ->
                "My name is Nova. 🤖"

            command == "time" || command.contains("what time") ->
                "The time is ${java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."

            command == "date" || command.contains("what is today's date") ->
                "Today is ${java.text.SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())}."

            command.contains("how are you") ->
                "I'm doing great and ready to help!"

            command.contains("turn on flashlight") || command == "flashlight on" -> {
                setTorch(true)
                "Flashlight is ON. 🔦"
            }

            command.contains("turn off flashlight") || command == "flashlight off" -> {
                setTorch(false)
                "Flashlight is OFF. 🔦"
            }

            command.contains("battery") -> batteryStatus()

            command.contains("volume up") -> {
                val audio = getSystemService(AUDIO_SERVICE) as AudioManager
                audio.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "Volume increased. 🔊"
            }

            command.contains("volume down") -> {
                val audio = getSystemService(AUDIO_SERVICE) as AudioManager
                audio.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "Volume lowered. 🔉"
            }

            command.contains("vibrate") -> {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= 26)
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(500)
                "Phone vibrated. 📳"
            }

            command.contains("open settings") -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                "Opening Settings. ⚙️"
            }

            command.contains("open camera") -> {
                startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
                "Opening Camera. 📷"
            }

            command.contains("open browser") -> {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")))
                "Opening the browser. 🌐"
            }

            command == "help" || command.contains("what can you do") ->
                """
                🤖 NOVA COMMANDS

                👑 Who created you
                🕐 Time
                📅 Date
                🔦 Turn on/off flashlight
                🔋 Battery status
                🔊 Volume up/down
                📳 Vibrate
                ⚙️ Open settings
                📷 Open camera
                🌐 Open browser
                🎤 Voice Mode

                Nova only uses Android permissions and supported system APIs.
                """.trimIndent()

            else ->
                "I'm still learning that. Try 'help' to see what I can do."
        }

        addLine("Nova", response)
        speak(response)
    }

    private fun setTorch(enabled: Boolean) {
        try {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            manager.setTorchMode(cameraId, enabled)
            torchOn = enabled
        } catch (e: Exception) {
            addLine("Nova", "I couldn't control the flashlight: ${e.message}")
        }
    }

    private fun batteryStatus(): String {
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Battery: $level% 🔋"
    }

    private fun startVoiceMode() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            addLine("Nova", "Speech recognition isn't available on this phone.")
            speak("Speech recognition isn't available on this phone.")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
            return
        }

        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this)

        speech?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                addLine("Nova", "🎤 Listening...")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                addLine("Nova", "I didn't catch that. Try again somewhere quieter.")
            }
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!words.isNullOrBlank()) respondTo(words)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Nova")
        }

        speech?.startListening(intent)
    }

    override fun onDestroy() {
        speech?.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
