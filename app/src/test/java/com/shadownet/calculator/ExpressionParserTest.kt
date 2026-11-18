package com.shadownet.calculator

import com.shadownet.calculator.engine.AngleUnit
import com.shadownet.calculator.engine.EvaluationResult
import com.shadownet.calculator.engine.ExpressionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionParserTest {

    @Test
    fun testBasicArithmetic() {
        val result = ExpressionParser.evaluate("5 + 3 * 2")
        assertTrue(result is EvaluationResult.Success)
        assertEquals("11", (result as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testParenthesesAndPrecedence() {
        val result = ExpressionParser.evaluate("(5 + 3) * 2")
        assertTrue(result is EvaluationResult.Success)
        assertEquals("16", (result as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testPowerAndAssociativity() {
        val result = ExpressionParser.evaluate("2 ^ 3 ^ 2")
        assertTrue(result is EvaluationResult.Success)
        assertEquals("512", (result as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testTrigonometricDegree() {
        val sinResult = ExpressionParser.evaluate("sin(90)", AngleUnit.DEG)
        assertTrue(sinResult is EvaluationResult.Success)
        assertEquals("1", (sinResult as EvaluationResult.Success).formattedResult)

        val cosResult = ExpressionParser.evaluate("cos(0)", AngleUnit.DEG)
        assertTrue(cosResult is EvaluationResult.Success)
        assertEquals("1", (cosResult as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testTrigonometricRadian() {
        val sinResult = ExpressionParser.evaluate("sin(pi / 2)", AngleUnit.RAD)
        assertTrue(sinResult is EvaluationResult.Success)
        assertEquals("1", (sinResult as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testLogarithmicFunctions() {
        val logResult = ExpressionParser.evaluate("log(100)")
        assertTrue(logResult is EvaluationResult.Success)
        assertEquals("2", (logResult as EvaluationResult.Success).formattedResult)

        val lnResult = ExpressionParser.evaluate("ln(e)")
        assertTrue(lnResult is EvaluationResult.Success)
        assertEquals("1", (lnResult as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testSquareRootAndConstants() {
        val sqrtResult = ExpressionParser.evaluate("sqrt(16) + pi")
        assertTrue(sqrtResult is EvaluationResult.Success)
        val success = sqrtResult as EvaluationResult.Success
        assertEquals(4.0 + Math.PI, success.rawValue, 1e-6)
    }

    @Test
    fun testImplicitMultiplication() {
        val result = ExpressionParser.evaluate("2pi")
        assertTrue(result is EvaluationResult.Success)
        assertEquals(2 * Math.PI, (result as EvaluationResult.Success).rawValue, 1e-6)
    }

    @Test
    fun testPercentageAndFactorial() {
        val pctResult = ExpressionParser.evaluate("50%")
        assertTrue(pctResult is EvaluationResult.Success)
        assertEquals("0.5", (pctResult as EvaluationResult.Success).formattedResult)

        val factResult = ExpressionParser.evaluate("5!")
        assertTrue(factResult is EvaluationResult.Success)
        assertEquals("120", (factResult as EvaluationResult.Success).formattedResult)
    }

    @Test
    fun testDivisionByZero() {
        val result = ExpressionParser.evaluate("10 / 0")
        assertTrue(result is EvaluationResult.Error)
        assertEquals("Cannot divide by zero", (result as EvaluationResult.Error).message)
    }

    @Test
    fun testDomainErrorLogNegative() {
        val result = ExpressionParser.evaluate("log(-10)")
        assertTrue(result is EvaluationResult.Error)
        assertTrue((result as EvaluationResult.Error).message.contains("Domain Error"))
    }
}
