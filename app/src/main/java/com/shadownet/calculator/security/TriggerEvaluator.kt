package com.shadownet.calculator.security

/**
 * Extensibility hook interface for evaluating/intercepting calculation flows.
 *
 * Decouples normal math expression parsing from external handlers (e.g. security vaults,
 * secret passcodes, hidden gestures, or custom plugin commands).
 */
fun interface TriggerEvaluator {
    /**
     * Interception hook called on Equals ('=') button tap or long-press.
     *
     * @param expression The current input expression in the calculator display.
     * @return `true` if intercepted (halts normal math execution); `false` to proceed with calculation.
     */
    fun onTriggerEvaluator(expression: String): Boolean
}

/**
 * Default non-intercepting evaluator.
 */
object DefaultTriggerEvaluator : TriggerEvaluator {
    override fun onTriggerEvaluator(expression: String): Boolean = false
}

/**
 * Sample security trigger evaluator demonstrating a passcode vault activation gateway.
 *
 * Intercepts calculation if the expression matches a secret passcode (e.g. "7777").
 */
class SampleSecurityTriggerEvaluator(
    private val secretPasscode: String = "7777",
    private val onSecurityTriggered: () -> Unit = {}
) : TriggerEvaluator {

    override fun onTriggerEvaluator(expression: String): Boolean {
        if (expression.trim() == secretPasscode) {
            onSecurityTriggered()
            return true
        }
        return false
    }
}
