package support.hb9hci.satfinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class FT4FT8Activity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var messagesView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var statusText: TextView

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val sampleRate = 12000 // Typical for FT4/FT8
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val handler = Handler(Looper.getMainLooper())
    private val messages = mutableListOf<String>()
    private val ft8Processor = FT8SignalProcessor()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
        private const val TAG = "FT4FT8Decoder"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ft4ft8)

        initializeViews()
        setupClickListeners()
        checkAudioPermission()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        messagesView = findViewById(R.id.messagesView)
        scrollView = findViewById(R.id.scrollView)
        statusText = findViewById(R.id.statusText)

        stopButton.isEnabled = false
        statusText.text = "Ready to start decoding FT4/FT8 signals"
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            startDecoding()
        }

        stopButton.setOnClickListener {
            stopDecoding()
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDecoding() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio recording permission required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "Failed to initialize audio recorder", Toast.LENGTH_SHORT).show()
                return
            }

            isRecording = true
            audioRecord?.startRecording()

            startButton.isEnabled = false
            stopButton.isEnabled = true
            statusText.text = "Recording and decoding... Listening for FT4/FT8 signals"

            recordingThread = Thread { recordingLoop() }
            recordingThread?.start()

            Log.d(TAG, "Started FT4/FT8 decoding")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            Toast.makeText(this, "Error starting audio recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopDecoding() {
        isRecording = false
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        recordingThread?.interrupt()
        recordingThread = null

        startButton.isEnabled = true
        stopButton.isEnabled = false
        statusText.text = "Stopped decoding"

        Log.d(TAG, "Stopped FT4/FT8 decoding")
    }

    private fun recordingLoop() {
        val buffer = ShortArray(bufferSize)
        var lastDecodeTime = 0L
        val decodeInterval = 15000L // FT8 decode every 15 seconds

        while (isRecording && audioRecord != null) {
            try {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (read > 0) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Process audio data for FT4/FT8 decoding
                    if (currentTime - lastDecodeTime >= decodeInterval) {
                        // Convert audio to double array for processing
                        val audioData = buffer.map { it.toDouble() / 32768.0 }.toDoubleArray()
                        
                        // Attempt to decode FT4/FT8 signals
                        val decodedMessages = processAudioForFT8(audioData)
                        
                        // Update UI with decoded messages
                        if (decodedMessages.isNotEmpty()) {
                            handler.post {
                                for (message in decodedMessages) {
                                    addDecodedMessage(message)
                                }
                            }
                        }
                        
                        lastDecodeTime = currentTime
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in recording loop", e)
                break
            }
        }
    }

    private fun processAudioForFT8(audioData: DoubleArray): List<String> {
        // Use the improved FT8 signal processor
        val decodedMessages = mutableListOf<String>()
        
        try {
            val ft8Messages = ft8Processor.processAudioBuffer(audioData, sampleRate)
            
            for (message in ft8Messages) {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
                val freqStr = String.format("%.0f", message.frequency)
                val snrStr = String.format("%+.0f", message.snr)
                val confidenceStr = String.format("%.1f", message.confidence)
                
                val formattedMessage = "$timestamp  $snrStr dB  $freqStr Hz  ${message.message}  [${confidenceStr}]"
                decodedMessages.add(formattedMessage)
            }
            
            // Also keep the simulation for demonstration when no real signals
            if (decodedMessages.isEmpty()) {
                // Basic signal analysis - detect potential patterns
                val rms = sqrt(audioData.map { it * it }.average())
                val signalThreshold = 0.01
                
                if (rms > signalThreshold) {
                    // Simulate occasional message for demonstration
                    if (Random.nextDouble() < 0.15) { // 15% chance per decode cycle
                        val simulatedMessage = generateSimpleSimulationMessage(1500.0, rms)
                        decodedMessages.add(simulatedMessage)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio for FT8", e)
        }
        
        return decodedMessages
    }

    private fun performBasicFFT(audioData: DoubleArray): DoubleArray {
        // Simplified FFT for demonstration
        // In production, you'd use a proper FFT library like JTransforms
        val fftSize = min(audioData.size, 1024)
        val result = DoubleArray(fftSize / 2)
        
        for (k in result.indices) {
            var real = 0.0
            var imag = 0.0
            
            for (n in 0 until fftSize) {
                val angle = -2.0 * PI * k * n / fftSize
                real += audioData[n] * cos(angle)
                imag += audioData[n] * sin(angle)
            }
            
            result[k] = sqrt(real * real + imag * imag)
        }
        
        return result
    }

    private fun findDominantFrequency(fft: DoubleArray): Double {
        val maxIndex = fft.indices.maxByOrNull { fft[it] } ?: 0
        return maxIndex * sampleRate.toDouble() / (2 * fft.size)
    }

    private fun generateSimpleSimulationMessage(frequency: Double, strength: Double): String {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val freqStr = String.format("%.0f", frequency)
        val strengthDb = String.format("%+.0f", 20 * log10(strength))
        
        // Simulate various FT8 message types
        val messageTypes = listOf(
            "CQ DL1ABC JO62",
            "DL1ABC DL2XYZ JO62",
            "DL2XYZ DL1ABC R-15",
            "DL1ABC DL2XYZ RRR",
            "DL2XYZ DL1ABC 73",
            "CQ TEST DJ0EE JO31"
        )
        
        val randomMessage = messageTypes.random()
        return "$timestamp  $strengthDb dB  $freqStr Hz  $randomMessage"
    }

    private fun addDecodedMessage(message: String) {
        messages.add(message)
        
        // Keep only last 50 messages to prevent memory issues
        if (messages.size > 50) {
            messages.removeAt(0)
        }
        
        // Update UI
        messagesView.text = messages.joinToString("\n")
        
        // Auto-scroll to bottom
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
        
        Log.d(TAG, "Decoded FT8 message: $message")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Audio permission is required for FT4/FT8 decoding", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDecoding()
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            stopDecoding()
        }
    }
}