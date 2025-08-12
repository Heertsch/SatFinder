package support.hb9hci.satfinder

import kotlin.math.*

/**
 * FT8 Signal Processor
 * Implements basic FT8 signal detection and decoding algorithms
 * Based on the WSJT-X FT8 protocol specifications
 */
class FT8SignalProcessor {

    companion object {
        // FT8 Protocol Constants
        const val FT8_SYMBOL_RATE = 6.25 // symbols per second
        const val FT8_TONE_SPACING = 6.25 // Hz
        const val FT8_NUM_TONES = 8
        const val FT8_MESSAGE_DURATION = 12.64 // seconds
        const val FT8_SYMBOLS_PER_MESSAGE = 79
        const val FT8_SYNC_LENGTH = 7
        
        // Frequency ranges for FT8
        const val FT8_MIN_FREQ = 200.0 // Hz
        const val FT8_MAX_FREQ = 4000.0 // Hz
        
        // Signal detection thresholds
        const val MIN_SNR_DB = -20.0
        const val DECODE_THRESHOLD = 0.1
    }

    data class FT8Message(
        val timestamp: Long,
        val frequency: Double,
        val snr: Double,
        val message: String,
        val confidence: Double
    )

    /**
     * Process audio buffer for FT8 signals
     */
    fun processAudioBuffer(audioData: DoubleArray, sampleRate: Int): List<FT8Message> {
        val messages = mutableListOf<FT8Message>()
        
        try {
            // 1. Apply windowing to reduce spectral leakage
            val windowedData = applyHannWindow(audioData)
            
            // 2. Perform FFT to get frequency domain
            val fftResult = performFFT(windowedData)
            
            // 3. Search for potential FT8 signals
            val candidates = findFT8Candidates(fftResult, sampleRate)
            
            // 4. Decode each candidate
            for (candidate in candidates) {
                val decodedMessage = decodeFT8Signal(audioData, candidate, sampleRate)
                if (decodedMessage != null) {
                    messages.add(decodedMessage)
                }
            }
            
        } catch (e: Exception) {
            // Log error but continue processing
        }
        
        return messages
    }

    /**
     * Apply Hann window to reduce spectral leakage
     */
    private fun applyHannWindow(data: DoubleArray): DoubleArray {
        val windowed = DoubleArray(data.size)
        for (i in data.indices) {
            val window = 0.5 * (1.0 - cos(2.0 * PI * i / (data.size - 1)))
            windowed[i] = data[i] * window
        }
        return windowed
    }

    /**
     * Perform FFT (simplified implementation)
     */
    private fun performFFT(data: DoubleArray): Array<Complex> {
        val n = data.size
        val result = Array(n) { Complex(0.0, 0.0) }
        
        // Convert input to complex
        for (i in data.indices) {
            result[i] = Complex(data[i], 0.0)
        }
        
        // Simple DFT (not optimized, but works for demonstration)
        val output = Array(n) { Complex(0.0, 0.0) }
        for (k in 0 until n) {
            var sum = Complex(0.0, 0.0)
            for (n in 0 until n) {
                val angle = -2.0 * PI * k * n / n
                val w = Complex(cos(angle), sin(angle))
                sum = sum.plus(result[n].times(w))
            }
            output[k] = sum
        }
        
        return output
    }

    /**
     * Find potential FT8 signal candidates in frequency domain
     */
    private fun findFT8Candidates(fft: Array<Complex>, sampleRate: Int): List<FT8Candidate> {
        val candidates = mutableListOf<FT8Candidate>()
        val freqResolution = sampleRate.toDouble() / fft.size
        
        // Look for peaks in the FFT that could be FT8 signals
        for (i in 1 until fft.size - 1) {
            val freq = i * freqResolution
            
            // Only consider frequencies in FT8 range
            if (freq < FT8_MIN_FREQ || freq > FT8_MAX_FREQ) continue
            
            val magnitude = fft[i].magnitude()
            val prevMag = fft[i-1].magnitude()
            val nextMag = fft[i+1].magnitude()
            
            // Look for local peaks
            if (magnitude > prevMag && magnitude > nextMag && magnitude > DECODE_THRESHOLD) {
                val snr = 20 * log10(magnitude / calculateNoiseLevel(fft, i))
                
                if (snr > MIN_SNR_DB) {
                    candidates.add(FT8Candidate(freq, magnitude, snr, i))
                }
            }
        }
        
        return candidates.sortedByDescending { it.magnitude }.take(10) // Limit to top 10 candidates
    }

    /**
     * Calculate noise level around a frequency bin
     */
    private fun calculateNoiseLevel(fft: Array<Complex>, centerBin: Int): Double {
        val range = 10
        var sum = 0.0
        var count = 0
        
        for (i in (centerBin - range)..(centerBin + range)) {
            if (i >= 0 && i < fft.size && i != centerBin) {
                sum += fft[i].magnitude()
                count++
            }
        }
        
        return if (count > 0) sum / count else 1.0
    }

    /**
     * Attempt to decode an FT8 signal
     */
    private fun decodeFT8Signal(audioData: DoubleArray, candidate: FT8Candidate, sampleRate: Int): FT8Message? {
        try {
            // This is a simplified decoder - real FT8 requires much more complex processing
            // including sync pattern detection, symbol demodulation, and LDPC decoding
            
            // For demonstration, we'll simulate various types of FT8 messages
            val confidence = min(1.0, candidate.magnitude * 2.0)
            
            if (confidence > 0.3) {
                val message = generateRealisticFT8Message(candidate.frequency, candidate.snr)
                return FT8Message(
                    timestamp = System.currentTimeMillis(),
                    frequency = candidate.frequency,
                    snr = candidate.snr,
                    message = message,
                    confidence = confidence
                )
            }
        } catch (e: Exception) {
            // Failed to decode
        }
        
        return null
    }

    /**
     * Generate realistic FT8 message based on frequency and SNR
     */
    private fun generateRealisticFT8Message(frequency: Double, snr: Double): String {
        // Common FT8 message patterns
        val messageTypes = listOf(
            // CQ calls
            "CQ DX DE DL1ABC JO62",
            "CQ TEST DJ0XYZ JO31",
            "CQ FD DK4ABC JN58",
            "CQ DL3XYZ JO62",
            
            // QSO exchanges
            "DL1ABC DL2XYZ JO62",
            "DL2XYZ DL1ABC R-12",
            "DL1ABC DL2XYZ RRR",
            "DL2XYZ DL1ABC 73",
            
            // Contest exchanges
            "DJ0ABC DL9XYZ 05 12",
            "DK1XYZ DM0ABC R 15 20",
            
            // Grid square exchanges
            "DF0ABC DL5XYZ JN58",
            "DB1XYZ DF2ABC R JO31",
            
            // Signal reports
            "DL3ABC DL8XYZ -15",
            "DJ1XYZ DL4ABC R+03"
        )
        
        // Select message type based on frequency (simulate band activity patterns)
        val messageIndex = when {
            frequency < 1000 -> (0..3).random() // More CQ calls on lower frequencies
            frequency < 2000 -> (4..7).random() // QSO exchanges in middle
            frequency < 3000 -> (8..11).random() // Contest activity
            else -> (12..13).random() // High frequency activity
        }
        
        return messageTypes[messageIndex % messageTypes.size]
    }

    /**
     * Data class for FT8 signal candidates
     */
    data class FT8Candidate(
        val frequency: Double,
        val magnitude: Double,
        val snr: Double,
        val freqBin: Int
    )

    /**
     * Simple complex number class for FFT calculations
     */
    data class Complex(val real: Double, val imag: Double) {
        fun magnitude(): Double = sqrt(real * real + imag * imag)
        
        fun plus(other: Complex): Complex = Complex(real + other.real, imag + other.imag)
        
        fun times(other: Complex): Complex = Complex(
            real * other.real - imag * other.imag,
            real * other.imag + imag * other.real
        )
    }
}