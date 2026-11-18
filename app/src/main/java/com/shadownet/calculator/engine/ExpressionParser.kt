package com.shadownet.calculator.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Angle measurement units supported for trigonometric computations.
 */
enum class AngleUnit {
    DEG, RAD
}

/**
 * Result wrapper for expression evaluation.
 */
sealed class EvaluationResult {
    data class Success(val rawValue: Double, val formattedResult: String) : EvaluationResult()
    data class Error(val message: String) : EvaluationResult()
}

/**
 * Internal token representation for the Shunting-Yard parser.
 */
private sealed class Token {
    data class Number(val value: Double) : Token()
    data class Constant(val name: String, val value: Double) : Token()
    data class Operator(
        val symbol: String,
        val precedence: Int,
        val isRightAssociative: Boolean,
        val isUnary: Boolean = false,
        val isPostfix: Boolean = false
    ) : Token()
    data class Function(val name: String) : Token()
    object LeftParen : Token()
    object RightParen : Token()
}

/**
 * Production-grade Mathematical Expression Parser & Evaluator engine.
 *
 * Implements the Shunting-Yard algorithm (Dijkstra) to parse infix expressions
 * into Reverse Polish Notation (RPN) tokens and evaluate them safely with stack evaluation.
 */
object ExpressionParser {

    private const val EPSILON = 1e-12

    fun evaluate(expression: String, angleUnit: AngleUnit = AngleUnit.DEG): EvaluationResult {
        val sanitized = sanitizeExpression(expression)
        if (sanitized.isBlank()) {
            return EvaluationResult.Success(0.0, "0")
        }

        return try {
            val tokens = tokenize(sanitized)
            val rpn = shuntingYard(tokens)
            val rawResult = evaluateRPN(rpn, angleUnit)

            if (rawResult.isNaN()) {
                return EvaluationResult.Error("Undefined Result")
            }
            if (rawResult.isInfinite()) {
                return EvaluationResult.Error("Cannot divide by zero")
            }

            val formatted = formatNumber(rawResult)
            EvaluationResult.Success(rawResult, formatted)
        } catch (e: ArithmeticException) {
            EvaluationResult.Error(e.message ?: "Calculation Error")
        } catch (e: IllegalArgumentException) {
            EvaluationResult.Error(e.message ?: "Invalid Expression")
        } catch (e: Exception) {
            EvaluationResult.Error("Syntax Error")
        }
    }

    private fun sanitizeExpression(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("√", "sqrt")
            .replace("∛", "cbrt")
            .replace("π", "pi")
            .replace("−", "-")
            .trim()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        var expectUnary = true

        while (i < expr.length) {
            val c = expr[i]

            when {
                c.isWhitespace() -> {
                    i++
                    continue
                }

                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    var hasDecimal = false
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') {
                            if (hasDecimal) throw IllegalArgumentException("Malformed number")
                            hasDecimal = true
                        }
                        sb.append(expr[i])
                        i++
                    }
                    val numVal = sb.toString().toDoubleOrNull()
                        ?: throw IllegalArgumentException("Invalid number format")

                    checkImplicitMultiplication(tokens)
                    tokens.add(Token.Number(numVal))
                    expectUnary = false
                    continue
                }

                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expr.length && expr[i].isLetter()) {
                        sb.append(expr[i])
                        i++
                    }
                    val word = sb.toString()
                    when (word) {
                        "pi" -> {
                            checkImplicitMultiplication(tokens)
                            tokens.add(Token.Constant("π", Math.PI))
                            expectUnary = false
                        }
                        "e" -> {
                            checkImplicitMultiplication(tokens)
                            tokens.add(Token.Constant("e", Math.E))
                            expectUnary = false
                        }
                        "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "cbrt", "abs" -> {
                            checkImplicitMultiplication(tokens)
                            tokens.add(Token.Function(word))
                            expectUnary = true
                        }
                        else -> throw IllegalArgumentException("Unknown function: $word")
                    }
                    continue
                }

                c == '(' -> {
                    checkImplicitMultiplication(tokens)
                    tokens.add(Token.LeftParen)
                    expectUnary = true
                    i++
                }

                c == ')' -> {
                    tokens.add(Token.RightParen)
                    expectUnary = false
                    i++
                }

                c == '+' -> {
                    tokens.add(Token.Operator("+", 1, false))
                    expectUnary = true
                    i++
                }

                c == '-' -> {
                    if (expectUnary) {
                        tokens.add(Token.Operator("u-", 5, true, isUnary = true))
                    } else {
                        tokens.add(Token.Operator("-", 1, false))
                    }
                    expectUnary = true
                    i++
                }

                c == '*' || c == '/' -> {
                    val op = if (c == '*') "*" else "/"
                    tokens.add(Token.Operator(op, 2, false))
                    expectUnary = true
                    i++
                }

                c == '^' -> {
                    tokens.add(Token.Operator("^", 4, true))
                    expectUnary = true
                    i++
                }

                c == '%' -> {
                    tokens.add(Token.Operator("%", 3, false, isPostfix = true))
                    expectUnary = false
                    i++
                }

                c == '!' -> {
                    tokens.add(Token.Operator("!", 4, false, isPostfix = true))
                    expectUnary = false
                    i++
                }

                else -> throw IllegalArgumentException("Unexpected character: $c")
            }
        }

        return tokens
    }

    private fun checkImplicitMultiplication(tokens: MutableList<Token>) {
        if (tokens.isEmpty()) return
        val last = tokens.last()
        if (last is Token.Number || last is Token.Constant || last is Token.RightParen || (last is Token.Operator && last.isPostfix)) {
            tokens.add(Token.Operator("*", 2, false))
        }
    }

    private fun shuntingYard(tokens: List<Token>): List<Token> {
        val outputQueue = mutableListOf<Token>()
        val operatorStack = Stack<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number, is Token.Constant -> outputQueue.add(token)
                is Token.Function -> operatorStack.push(token)
                is Token.Operator -> {
                    while (operatorStack.isNotEmpty()) {
                        val top = operatorStack.peek()
                        if (top is Token.LeftParen) break
                        val shouldPop = when (top) {
                            is Token.Function -> true
                            is Token.Operator -> {
                                if (token.isRightAssociative) top.precedence > token.precedence
                                else top.precedence >= token.precedence
                            }
                            else -> false
                        }
                        if (shouldPop) outputQueue.add(operatorStack.pop()) else break
                    }
                    operatorStack.push(token)
                }
                is Token.LeftParen -> operatorStack.push(token)
                is Token.RightParen -> {
                    var foundLeft = false
                    while (operatorStack.isNotEmpty()) {
                        val top = operatorStack.peek()
                        if (top is Token.LeftParen) {
                            operatorStack.pop()
                            foundLeft = true
                            break
                        } else {
                            outputQueue.add(operatorStack.pop())
                        }
                    }
                    if (!foundLeft) throw IllegalArgumentException("Mismatched parentheses")
                    if (operatorStack.isNotEmpty() && operatorStack.peek() is Token.Function) {
                        outputQueue.add(operatorStack.pop())
                    }
                }
            }
        }

        while (operatorStack.isNotEmpty()) {
            val top = operatorStack.pop()
            if (top is Token.LeftParen || top is Token.RightParen) throw IllegalArgumentException("Mismatched parentheses")
            outputQueue.add(top)
        }

        return outputQueue
    }

    private fun evaluateRPN(rpn: List<Token>, angleUnit: AngleUnit): Double {
        val stack = Stack<Double>()

        for (token in rpn) {
            when (token) {
                is Token.Number -> stack.push(token.value)
                is Token.Constant -> stack.push(token.value)
                is Token.Operator -> {
                    if (token.isUnary) {
                        if (stack.isEmpty()) throw IllegalArgumentException("Invalid expression")
                        val a = stack.pop()
                        if (token.symbol == "u-") stack.push(-a)
                    } else if (token.isPostfix) {
                        if (stack.isEmpty()) throw IllegalArgumentException("Invalid expression")
                        val a = stack.pop()
                        when (token.symbol) {
                            "%" -> stack.push(a / 100.0)
                            "!" -> stack.push(calculateFactorial(a))
                            else -> throw IllegalArgumentException("Unknown operator")
                        }
                    } else {
                        if (stack.size < 2) throw IllegalArgumentException("Invalid expression")
                        val b = stack.pop()
                        val a = stack.pop()
                        val res = when (token.symbol) {
                            "+" -> a + b
                            "-" -> a - b
                            "*" -> a * b
                            "/" -> {
                                if (abs(b) < 1e-15) throw ArithmeticException("Cannot divide by zero")
                                a / b
                            }
                            "^" -> a.pow(b)
                            else -> throw IllegalArgumentException("Unknown operator")
                        }
                        stack.push(res)
                    }
                }
                is Token.Function -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Missing argument")
                    val arg = stack.pop()
                    val res = evaluateFunction(token.name, arg, angleUnit)
                    stack.push(res)
                }
                else -> throw IllegalArgumentException("Invalid RPN token")
            }
        }

        if (stack.size != 1) throw IllegalArgumentException("Malformed expression")
        return cleanFloatingPointNoise(stack.pop())
    }

    private fun evaluateFunction(fnName: String, arg: Double, angleUnit: AngleUnit): Double {
        return when (fnName) {
            "sin" -> {
                val rad = if (angleUnit == AngleUnit.DEG) Math.toRadians(arg) else arg
                val v = sin(rad)
                if (angleUnit == AngleUnit.DEG && abs(arg % 180.0) < EPSILON) 0.0 else v
            }
            "cos" -> {
                val rad = if (angleUnit == AngleUnit.DEG) Math.toRadians(arg) else arg
                val v = cos(rad)
                if (angleUnit == AngleUnit.DEG && abs((arg - 90.0) % 180.0) < EPSILON) 0.0 else v
            }
            "tan" -> {
                if (angleUnit == AngleUnit.DEG && abs((arg - 90.0) % 180.0) < EPSILON) {
                    throw ArithmeticException("Domain Error: tan undefined")
                }
                val rad = if (angleUnit == AngleUnit.DEG) Math.toRadians(arg) else arg
                tan(rad)
            }
            "asin" -> {
                if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain Error: asin")
                val res = asin(arg)
                if (angleUnit == AngleUnit.DEG) Math.toDegrees(res) else res
            }
            "acos" -> {
                if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain Error: acos")
                val res = acos(arg)
                if (angleUnit == AngleUnit.DEG) Math.toDegrees(res) else res
            }
            "atan" -> {
                val res = atan(arg)
                if (angleUnit == AngleUnit.DEG) Math.toDegrees(res) else res
            }
            "log" -> {
                if (arg <= 0) throw ArithmeticException("Domain Error: log")
                log10(arg)
            }
            "ln" -> {
                if (arg <= 0) throw ArithmeticException("Domain Error: ln")
                ln(arg)
            }
            "sqrt" -> {
                if (arg < 0) throw ArithmeticException("Domain Error: sqrt")
                sqrt(arg)
            }
            "cbrt" -> cbrt(arg)
            "abs" -> abs(arg)
            else -> throw IllegalArgumentException("Unsupported function: $fnName")
        }
    }

    private fun calculateFactorial(valIn: Double): Double {
        if (valIn < 0 || valIn != valIn.toLong().toDouble()) {
            throw ArithmeticException("Domain Error: Factorial integer required")
        }
        val n = valIn.toLong()
        if (n > 170) throw ArithmeticException("Overflow")
        var result = 1.0
        for (i in 2..n) result *= i.toDouble()
        return result
    }

    private fun cleanFloatingPointNoise(valIn: Double): Double {
        if (valIn.isNaN() || valIn.isInfinite()) return valIn
        val rounded = Math.round(valIn * 1e12) / 1e12
        return if (abs(valIn - rounded) < EPSILON) rounded else valIn
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return "Infinity"
        if (value == value.toLong().toDouble() && abs(value) < 1e15) {
            return value.toLong().toString()
        }
        return if (abs(value) in 1e-6..1e12 || value == 0.0) {
            val bd = BigDecimal(value.toString()).stripTrailingZeros()
            val scale = bd.scale().coerceAtMost(10)
            bd.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        } else {
            val symbols = DecimalFormatSymbols(Locale.US)
            val formatter = DecimalFormat("0.######E0", symbols)
            formatter.format(value)
        }
    }
}
