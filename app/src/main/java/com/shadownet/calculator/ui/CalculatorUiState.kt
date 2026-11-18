package com.shadownet.calculator.ui

import com.shadownet.calculator.engine.AngleUnit

/**
 * Historical record of an evaluated mathematical expression.
 */
data class CalculationHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val expression: String,
    val result: String
)

/**
 * Immutable State holding the current UI model for the scientific calculator screen.
 */
data class CalculatorUiState(
    val expression: String = "",
    val resultPreview: String = "",
    val isScientificMode: Boolean = true,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val error: String? = null,
    val history: List<CalculationHistoryItem> = emptyList(),
    val isIntercepted: Boolean = false,
    val interceptionMessage: String? = null,
    val isShiftActive: Boolean = false,
    val isAlphaActive: Boolean = false
)
