package com.example.dindoripranityadnyiki.core.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationManager {

    private fun getOptions(source: String, target: String): TranslatorOptions {
        return TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
    }

    /**
     * Translated text from English to Marathi automatically.
     */
    fun translateToMarathi(
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val options = getOptions(TranslateLanguage.ENGLISH, TranslateLanguage.MARATHI)
        val translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onSuccess(translatedText)
                        translator.close()
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                        translator.close()
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
