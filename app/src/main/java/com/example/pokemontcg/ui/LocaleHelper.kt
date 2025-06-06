package com.example.pokemontcg.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val PREF_LANG = "app_language"       // clave en SharedPreferences
    private const val DEFAULT_LANG = "es"               // idioma por defecto (español)

    /** Devuelve el idioma actual guardado. */
    fun getSavedLanguage(prefs: SharedPreferences): String {
        return prefs.getString(PREF_LANG, DEFAULT_LANG) ?: DEFAULT_LANG
    }

    /**
     * Cambia el idioma guardado en prefs y devuelve un Context actualizado.
     * @param newLang “es” o “en”
     */
    fun setLocale(context: Context, prefs: SharedPreferences, newLang: String): Context {
        // 1) Guardar la elección en prefs
        prefs.edit().putString(PREF_LANG, newLang).apply()
        // 2) Aplicar el nuevo locale al context y devolverlo
        return updateContextLocale(context, newLang)
    }

    /**
     * Toma un Context y devuelve un nuevo Context configurado para `language`.
     */
    private fun updateContextLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            // para versiones antiguas
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    fun updateAppLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        // Para Android 7.0+:
        return context.createConfigurationContext(config)
    }
}