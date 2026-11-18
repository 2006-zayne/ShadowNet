package com.shadownet.calculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadownet.calculator.ui.CalculatorUiState
import com.shadownet.calculator.ui.theme.CasioLcdDarkBg
import com.shadownet.calculator.ui.theme.CasioLcdDarkText
import com.shadownet.calculator.ui.theme.CasioLcdBg
import com.shadownet.calculator.ui.theme.CasioLcdText
import com.shadownet.calculator.ui.theme.ErrorText
import com.shadownet.calculator.ui.theme.InterceptedBannerBg
import com.shadownet.calculator.ui.theme.InterceptedBannerContent

/**
 * Spacious Casio-style Natural Textbook LCD Display Panel with double height (210dp)
 * and top-aligned status indicators and toolbar toggles.
 */
@Composable
fun DisplaySection(
    uiState: CalculatorUiState,
    onToggleScientific: () -> Unit,
    onToggleAngleUnit: () -> Unit,
    onToggleHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.expression) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val lcdBg = if (isDarkTheme) CasioLcdDarkBg else CasioLcdBg
    val lcdText = if (isDarkTheme) CasioLcdDarkText else CasioLcdText
    val lcdBorder = if (isDarkTheme) Color(0xFF26332E) else Color(0xFFBAC8C0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp) // Double fixed height for spacious natural textbook display
            .clip(RoundedCornerShape(12.dp))
            .background(lcdBg)
            .border(1.5.dp, lcdBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Very Top Header: Status Indicators (MATH, DEG/RAD, NORM) + Toolbars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isShiftActive) {
                        Text(
                            text = "S",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEAB308)
                        )
                    }
                    if (uiState.isAlphaActive) {
                        Text(
                            text = "A",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEC4899)
                        )
                    }
                    Text(
                        text = "MATH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = lcdText.copy(alpha = 0.85f)
                    )
                    Text(text = "•", fontSize = 11.sp, color = lcdText.copy(alpha = 0.5f))
                    Text(
                        text = uiState.angleUnit.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = lcdText.copy(alpha = 0.85f)
                    )
                    Text(text = "•", fontSize = 11.sp, color = lcdText.copy(alpha = 0.5f))
                    Text(
                        text = if (uiState.isScientificMode) "NORM-11" else "STD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = lcdText.copy(alpha = 0.85f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme",
                            tint = lcdText.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onToggleHistory,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = lcdText.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Security Interception Banner
            AnimatedVisibility(
                visible = uiState.isIntercepted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(InterceptedBannerBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Intercepted",
                        tint = InterceptedBannerContent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.interceptionMessage ?: "Security Hook Triggered",
                        color = InterceptedBannerContent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Spacious Expression and Result Display Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Expression Input Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.expression.ifEmpty { "0" },
                        color = if (uiState.expression.isEmpty()) lcdText.copy(alpha = 0.4f) else lcdText,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Result Output
                val resultText = when {
                    uiState.error != null -> uiState.error
                    uiState.isIntercepted -> "INTERCEPTED"
                    uiState.resultPreview.isNotEmpty() -> uiState.resultPreview
                    else -> ""
                }

                val resultColor = when {
                    uiState.error != null -> ErrorText
                    uiState.isIntercepted -> InterceptedBannerBg
                    else -> lcdText.copy(alpha = 0.9f)
                }

                if (resultText.isNotEmpty()) {
                    Text(
                        text = resultText,
                        color = resultColor,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
