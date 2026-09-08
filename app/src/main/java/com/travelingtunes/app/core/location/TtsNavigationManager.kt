package com.travelingtunes.app.core.location

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsNavigationManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isInitialized = true
        }
    }

    fun say(text: String) {
        if (!isInitialized) return
        val expandedText = expandText(text)
        tts?.speak(expandedText, TextToSpeech.QUEUE_FLUSH, null, "TravelingTunesNav")
    }

    fun feetOrMiles(distanceMeters: Float): String {
        val feet = distanceMeters * 3.28084f
        return if (feet >= 1000f) {
            val miles = feet / 5280f
            String.format(Locale.US, "%.1f miles", miles)
        } else {
            "${feet.toInt()} feet"
        }
    }

    private fun expandText(words: String): String {
        return words
            .replace(" N ", " North ")
            .replace(" S ", " South ")
            .replace(" E ", " East ")
            .replace(" W ", " West ")
            .replace(" St ", " Street ")
            .replace(" Ave ", " Avenue ")
            .replace(" Rd ", " Road ")
            .replace(" Blvd ", " Boulevard ")
            .replace(" Hwy ", " Highway ")
            .replace(" Fwy ", " Freeway ")
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
