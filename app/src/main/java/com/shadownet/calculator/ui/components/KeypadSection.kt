package com.shadownet.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadownet.calculator.ui.theme.AcKeyBg
import com.shadownet.calculator.ui.theme.AlphaKeyBg
import com.shadownet.calculator.ui.theme.DarkFaceplateBg
import com.shadownet.calculator.ui.theme.DarkKeyText
import com.shadownet.calculator.ui.theme.DarkNumberKeyBg
import com.shadownet.calculator.ui.theme.DarkOperatorKeyBg
import com.shadownet.calculator.ui.theme.DarkScientificKeyBg
import com.shadownet.calculator.ui.theme.DelKeyBg
import com.shadownet.calculator.ui.theme.FuncKeyBg
import com.shadownet.calculator.ui.theme.KeyTextLight
import com.shadownet.calculator.ui.theme.LightFaceplateBg
import com.shadownet.calculator.ui.theme.LightNumberKeyBg
import com.shadownet.calculator.ui.theme.LightNumberKeyContent
import com.shadownet.calculator.ui.theme.LightOperatorKeyBg
import com.shadownet.calculator.ui.theme.LightOperatorKeyContent
import com.shadownet.calculator.ui.theme.LightScientificKeyBg
import com.shadownet.calculator.ui.theme.ShiftKeyBg
import com.shadownet.calculator.ui.theme.getAlphaLabelColor
import com.shadownet.calculator.ui.theme.getSecondaryLabelColor

/**
 * Casio Scientific Calculator Keypad with faceplate-printed secondary labels (SHIFT/ALPHA)
 * positioned OUTSIDE on the calculator plastic body above each button keycap.
 */
@Composable
fun KeypadSection(
    isScientificMode: Boolean,
    angleUnitName: String,
    isShiftActive: Boolean,
    isAlphaActive: Boolean,
    isDarkTheme: Boolean,
    onToggleShift: () -> Unit,
    onToggleAlpha: () -> Unit,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onFunctionClick: (String) -> Unit,
    onParenthesisClick: (String) -> Unit,
    onConstantClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEqualsClick: (isLongPress: Boolean) -> Unit,
    onToggleAngleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faceplateBg = if (isDarkTheme) DarkFaceplateBg else LightFaceplateBg
    val keyBorder = if (isDarkTheme) Color(0xFF323B38) else Color(0xFF94A3B8)

    val sciBg = if (isDarkTheme) DarkScientificKeyBg else LightScientificKeyBg
    val sciContent = if (isDarkTheme) DarkKeyText else Color.White

    val numBg = if (isDarkTheme) DarkNumberKeyBg else LightNumberKeyBg
    val numContent = if (isDarkTheme) DarkKeyText else LightNumberKeyContent

    val opBg = if (isDarkTheme) DarkOperatorKeyBg else LightOperatorKeyBg
    val opContent = if (isDarkTheme) DarkKeyText else LightOperatorKeyContent

    val secondaryLabelColor = getSecondaryLabelColor(isDarkTheme)
    val alphaLabelColor = getAlphaLabelColor(isDarkTheme)

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(faceplateBg)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Modifier Row: SHIFT, ALPHA, MODE, DEG/RAD ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.85f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioModifierKey("SHIFT", ShiftKeyBg, Color.Black, isShiftActive, keyBorder, Modifier.weight(1f), onToggleShift)
            CasioModifierKey("ALPHA", AlphaKeyBg, KeyTextLight, isAlphaActive, keyBorder, Modifier.weight(1f), onToggleAlpha)
            CasioModifierKey("MODE", FuncKeyBg, KeyTextLight, false, keyBorder, Modifier.weight(1f), onToggleAngleUnit)
            CasioModifierKey("DEG/RAD", FuncKeyBg, KeyTextLight, false, keyBorder, Modifier.weight(1f), onToggleAngleUnit)
        }

        // --- Upper Scientific Function Rows ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("CALC", "SOLVE", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("abs") }
            CasioKeyCell("d/dx", "∫dx", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("abs") }
            CasioKeyCell("x⁻¹", "x!", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("^") }
            CasioKeyCell("logᵧ", "10ˣ", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("log") }
            CasioKeyCell("ln", "eˣ", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("ln") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("⚲", "a/b", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("/") }
            CasioKeyCell(if (isShiftActive) "∛" else "√", if (isShiftActive) "³√" else "√", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) {
                if (isShiftActive) onFunctionClick("cbrt") else onFunctionClick("√")
            }
            CasioKeyCell("x²", "x³", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("^") }
            CasioKeyCell("xʸ", "ʸ√x", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("^") }
            CasioKeyCell("log", "10ˣ", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("log") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("(-)", "STO", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("-") }
            CasioKeyCell("hyp", "abs", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("abs") }
            CasioKeyCell(if (isShiftActive) "sin⁻¹" else "sin", if (isShiftActive) "sin" else "sin⁻¹", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("sin") }
            CasioKeyCell(if (isShiftActive) "cos⁻¹" else "cos", if (isShiftActive) "cos" else "cos⁻¹", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("cos") }
            CasioKeyCell(if (isShiftActive) "tan⁻¹" else "tan", if (isShiftActive) "tan" else "tan⁻¹", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onFunctionClick("tan") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("RCL", "CLR", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onConstantClick("e") }
            CasioKeyCell("ENG", "i", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onConstantClick("π") }
            CasioKeyCell("(", "%", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onParenthesisClick("(") }
            CasioKeyCell(")", "", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onParenthesisClick(")") }
            CasioKeyCell("S⇕D", "M+", "", sciBg, sciContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onToggleAngleUnit() }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // --- Lower Numeric & Operator Keypad Grid (4x5) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("7", "CONST", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("7") }
            CasioKeyCell("8", "CONV", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("8") }
            CasioKeyCell("9", "CLR", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("9") }
            CasioKeyCell("DEL", "INS", "", DelKeyBg, KeyTextLight, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDeleteClick() }
            CasioKeyCell("AC", "OFF", "", AcKeyBg, KeyTextLight, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onClearClick() }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("4", "MATRIX", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("4") }
            CasioKeyCell("5", "VECTOR", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("5") }
            CasioKeyCell("6", "STAT", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("6") }
            CasioKeyCell("×", "nPr", "", opBg, opContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("×") }
            CasioKeyCell("÷", "nCr", "", opBg, opContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("÷") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("1", "STAT", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("1") }
            CasioKeyCell("2", "CMPLX", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("2") }
            CasioKeyCell("3", "DISTR", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("3") }
            CasioKeyCell("+", "Pol", "", opBg, opContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("+") }
            CasioKeyCell("−", "Rec", "", opBg, opContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onOperatorClick("−") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CasioKeyCell("0", "Rnd", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick("0") }
            CasioKeyCell(".", "Ran#", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onDigitClick(".") }
            CasioKeyCell("×10ˣ", "π", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onConstantClick("π") }
            CasioKeyCell("Ans", "PreAns", "", numBg, numContent, keyBorder, secondaryLabelColor, alphaLabelColor, Modifier.weight(1f)) { onConstantClick("e") }

            CasioEqualsCell(
                containerColor = opBg,
                contentColor = opContent,
                borderColor = keyBorder,
                secondaryLabelColor = secondaryLabelColor,
                modifier = Modifier.weight(1f),
                onEqualsClick = onEqualsClick
            )
        }
    }
}

@Composable
private fun CasioModifierKey(
    text: String,
    bg: Color,
    textColor: Color,
    isActive: Boolean,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val keyShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    val activeBorder = if (isActive) Color.White else borderColor

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .shadow(elevation = 2.5.dp, shape = keyShape)
            .border(if (isActive) 2.dp else 1.dp, activeBorder, keyShape),
        shape = keyShape,
        color = bg,
        contentColor = textColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Key cell rendering secondary SHIFT/ALPHA labels on the calculator faceplate OUTSIDE the button keycap,
 * with 100% button surface coverage for the primary key function name.
 */
@Composable
private fun CasioKeyCell(
    primaryLabel: String,
    shiftLabel: String,
    alphaLabel: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    secondaryLabelColor: Color,
    alphaLabelColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val keyShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Secondary labels printed on the faceplate plastic OUTSIDE the button keycap
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shiftLabel,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryLabelColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            if (alphaLabel.isNotEmpty()) {
                Text(
                    text = alphaLabel,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = alphaLabelColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // 2. Physical raised keycap carrying ONLY the primary function name
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(elevation = 2.5.dp, shape = keyShape)
                .border(1.dp, borderColor, keyShape),
            shape = keyShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(1.dp)
            ) {
                Text(
                    text = primaryLabel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CasioEqualsCell(
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    secondaryLabelColor: Color,
    modifier: Modifier = Modifier,
    onEqualsClick: (isLongPress: Boolean) -> Unit
) {
    val keyShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "EXE",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryLabelColor,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(elevation = 2.5.dp, shape = keyShape)
                .border(1.dp, borderColor, keyShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onEqualsClick(false) },
                        onLongPress = { onEqualsClick(true) }
                    )
                },
            shape = keyShape,
            color = containerColor,
            contentColor = contentColor
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(1.dp)
            ) {
                Text(
                    text = "=",
                    color = contentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
