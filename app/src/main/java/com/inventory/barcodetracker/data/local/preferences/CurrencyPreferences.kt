package com.inventory.barcodetracker.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class CurrencyPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _currencySymbol = MutableStateFlow(
        prefs.getString(KEY_CURRENCY_SYMBOL, "£") ?: "£",
    )
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _budgetLimit = MutableStateFlow(
        prefs.getFloat(KEY_BUDGET_LIMIT, 0f).toDouble(),
    )
    val budgetLimit: StateFlow<Double> = _budgetLimit.asStateFlow()

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, "system") ?: "system",
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _capitalizationMode = MutableStateFlow(
        prefs.getString(KEY_CAPITALIZATION_MODE, "TITLE_CASE") ?: "TITLE_CASE",
    )
    val capitalizationMode: StateFlow<String> = _capitalizationMode.asStateFlow()

    fun setCurrencySymbol(symbol: String) {
        val trimmed = symbol.trim().ifEmpty { "£" }
        prefs.edit { putString(KEY_CURRENCY_SYMBOL, trimmed) }
        _currencySymbol.value = trimmed
    }

    fun setBudgetLimit(limit: Double) {
        val positive = maxOf(0.0, limit)
        prefs.edit { putFloat(KEY_BUDGET_LIMIT, positive.toFloat()) }
        _budgetLimit.value = positive
    }

    fun setThemeMode(mode: String) {
        val validMode = if (mode in listOf("system", "light", "dark")) mode else "system"
        prefs.edit { putString(KEY_THEME_MODE, validMode) }
        _themeMode.value = validMode
    }

    fun setCapitalizationMode(mode: String) {
        val validMode = if (mode in listOf("FIRST_LETTER", "TITLE_CASE", "NONE")) mode else "TITLE_CASE"
        prefs.edit { putString(KEY_CAPITALIZATION_MODE, validMode) }
        _capitalizationMode.value = validMode
    }

    fun formatText(text: String): String {
        return formatTextWithMode(text, _capitalizationMode.value)
    }

    companion object {
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_BUDGET_LIMIT = "budget_limit"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CAPITALIZATION_MODE = "capitalization_mode"

        val POPULAR_CURRENCIES = listOf(
            "RON" to "Romanian Leu (RON)",
            "€" to "Euro (€)",
            "£" to "British Pound (£)",
            "$" to "US Dollar ($)",
        )

        fun formatTextWithMode(text: String, mode: String): String {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return ""
            return when (mode) {
                "FIRST_LETTER" -> trimmed.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                "TITLE_CASE" -> trimmed.split(" ").joinToString(" ") { word ->
                    if (word.length <= 2 && word.all { c -> c.isUpperCase() }) word
                    else word.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                else -> trimmed
            }
        }
    }
}
