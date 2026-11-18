package com.shadownet.calculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shadownet.calculator.engine.AngleUnit
import com.shadownet.calculator.engine.EvaluationResult
import com.shadownet.calculator.engine.ExpressionParser
import com.shadownet.calculator.security.DefaultTriggerEvaluator
import com.shadownet.calculator.security.TriggerEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel managing Calculator state, input buffer, math engine evaluation,
 * shift/alpha modifiers, persistent history storage, and security interception hooks.
 */
class CalculatorViewModel(
    application: Application
) : AndroidViewModel(application) {

    private var triggerEvaluator: TriggerEvaluator = DefaultTriggerEvaluator

    private val _uiState = MutableStateFlow(
        CalculatorUiState(
            history = HistoryPersistenceManager.loadHistory(application)
        )
    )
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    var onPowerOff: (() -> Unit)? = null

    fun setTriggerEvaluator(evaluator: TriggerEvaluator) {
        this.triggerEvaluator = evaluator
    }

    fun toggleShift() {
        _uiState.update { it.copy(isShiftActive = !it.isShiftActive, isAlphaActive = false) }
    }

    fun toggleAlpha() {
        _uiState.update { it.copy(isAlphaActive = !it.isAlphaActive, isShiftActive = false) }
    }

    fun onDigitClick(digit: String) {
        _uiState.update { state ->
            val newExpr = if (state.isIntercepted) digit else state.expression + digit
            state.copy(
                expression = newExpr,
                error = null,
                isIntercepted = false,
                interceptionMessage = null
            )
        }
        updatePreview()
    }

    fun onOperatorClick(op: String) {
        _uiState.update { state ->
            var current = if (state.isIntercepted) "" else state.expression
            if (current.isEmpty() && op == "−") {
                current = "−"
            } else if (current.isNotEmpty()) {
                val lastChar = current.last()
                if (isOperatorChar(lastChar) && op != "−") {
                    current = current.dropLast(1) + op
                } else {
                    current += op
                }
            }
            state.copy(
                expression = current,
                error = null,
                isIntercepted = false,
                interceptionMessage = null,
                isShiftActive = false,
                isAlphaActive = false
            )
        }
        updatePreview()
    }

    fun onFunctionClick(fn: String) {
        _uiState.update { state ->
            val prefix = if (state.isIntercepted) "" else state.expression
            val actualFn = if (state.isShiftActive) {
                when (fn) {
                    "sin" -> "asin"
                    "cos" -> "acos"
                    "tan" -> "atan"
                    "log" -> "10^"
                    "ln" -> "e^"
                    "√" -> "cbrt"
                    else -> fn
                }
            } else {
                fn
            }

            val formattedFn = when (actualFn) {
                "√" -> "√("
                "cbrt" -> "cbrt("
                else -> "$actualFn("
            }
            state.copy(
                expression = prefix + formattedFn,
                error = null,
                isIntercepted = false,
                interceptionMessage = null,
                isShiftActive = false,
                isAlphaActive = false
            )
        }
        updatePreview()
    }

    fun onParenthesisClick(paren: String) {
        _uiState.update { state ->
            val current = if (state.isIntercepted) "" else state.expression
            state.copy(
                expression = current + paren,
                error = null,
                isIntercepted = false,
                interceptionMessage = null,
                isShiftActive = false,
                isAlphaActive = false
            )
        }
        updatePreview()
    }

    fun onConstantClick(constant: String) {
        _uiState.update { state ->
            val current = if (state.isIntercepted) "" else state.expression
            state.copy(
                expression = current + constant,
                error = null,
                isIntercepted = false,
                interceptionMessage = null,
                isShiftActive = false,
                isAlphaActive = false
            )
        }
        updatePreview()
    }

    fun onClearClick() {
        if (_uiState.value.isShiftActive) {
            // SHIFT + AC -> Power Off Shortcut!
            _uiState.update {
                it.copy(
                    expression = "Off",
                    resultPreview = "",
                    error = null,
                    isShiftActive = false,
                    isAlphaActive = false
                )
            }
            onPowerOff?.invoke()
            return
        }

        _uiState.update {
            it.copy(
                expression = "",
                resultPreview = "",
                error = null,
                isIntercepted = false,
                interceptionMessage = null,
                isShiftActive = false,
                isAlphaActive = false
            )
        }
    }

    fun onDeleteClick() {
        _uiState.update { state ->
            if (state.isIntercepted) {
                return@update state.copy(expression = "", isIntercepted = false, interceptionMessage = null)
            }
            var current = state.expression
            if (current.isNotEmpty()) {
                val functionPrefixes = listOf("asin(", "acos(", "atan(", "cbrt(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(")
                val matchedPrefix = functionPrefixes.find { current.endsWith(it) }

                current = if (matchedPrefix != null) {
                    current.dropLast(matchedPrefix.length)
                } else {
                    current.dropLast(1)
                }
            }
            state.copy(expression = current, error = null)
        }
        updatePreview()
    }

    fun onEqualsClick(isLongPress: Boolean = false) {
        val currentExpr = _uiState.value.expression
        if (currentExpr.isBlank()) return

        if (triggerEvaluator.onTriggerEvaluator(currentExpr)) {
            _uiState.update {
                it.copy(
                    isIntercepted = true,
                    interceptionMessage = "SECURITY HOOK TRIGGERED",
                    resultPreview = "INTERCEPTED",
                    error = null
                )
            }
            return
        }

        val angleMode = _uiState.value.angleUnit
        when (val result = ExpressionParser.evaluate(currentExpr, angleMode)) {
            is EvaluationResult.Success -> {
                val newHistoryItem = CalculationHistoryItem(
                    expression = currentExpr,
                    result = result.formattedResult
                )
                val updatedHistory = listOf(newHistoryItem) + _uiState.value.history
                val limitedHistory = updatedHistory.take(20)

                // Save to persistent storage
                HistoryPersistenceManager.saveHistory(getApplication(), limitedHistory)

                _uiState.update {
                    it.copy(
                        expression = result.formattedResult,
                        resultPreview = "",
                        error = null,
                        history = limitedHistory,
                        isIntercepted = false,
                        interceptionMessage = null,
                        isShiftActive = false,
                        isAlphaActive = false
                    )
                }
            }
            is EvaluationResult.Error -> {
                _uiState.update { it.copy(error = result.message, resultPreview = "") }
            }
        }
    }

    fun toggleScientificMode() {
        _uiState.update { it.copy(isScientificMode = !it.isScientificMode) }
    }

    fun toggleAngleUnit() {
        _uiState.update {
            val newMode = if (it.angleUnit == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG
            it.copy(angleUnit = newMode)
        }
        updatePreview()
    }

    fun onHistoryItemSelect(item: CalculationHistoryItem) {
        _uiState.update {
            it.copy(
                expression = item.expression,
                error = null,
                isIntercepted = false,
                interceptionMessage = null
            )
        }
        updatePreview()
    }

    fun clearHistory() {
        HistoryPersistenceManager.saveHistory(getApplication(), emptyList())
        _uiState.update { it.copy(history = emptyList()) }
    }

    private fun updatePreview() {
        val current = _uiState.value.expression
        if (current.isBlank() || _uiState.value.isIntercepted) {
            _uiState.update { it.copy(resultPreview = "") }
            return
        }

        val angleMode = _uiState.value.angleUnit
        val evalResult = ExpressionParser.evaluate(current, angleMode)
        if (evalResult is EvaluationResult.Success && evalResult.formattedResult != current) {
            _uiState.update { it.copy(resultPreview = evalResult.formattedResult, error = null) }
        } else {
            _uiState.update { it.copy(resultPreview = "") }
        }
    }

    private fun isOperatorChar(c: Char): Boolean {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '^'
    }
}
