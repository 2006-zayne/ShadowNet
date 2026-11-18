package com.shadownet.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shadownet.calculator.ui.components.AppearanceSettingsDialog
import com.shadownet.calculator.ui.components.DisplaySection
import com.shadownet.calculator.ui.components.HistoryOverlay
import com.shadownet.calculator.ui.components.KeypadSection
import com.shadownet.calculator.ui.theme.CalculatorTheme
import com.shadownet.calculator.ui.theme.ThemeMode
import com.shadownet.calculator.ui.theme.ThemePreferenceManager

/**
 * Main Calculator Screen Composable matching physical Casio scientific calculator UI design.
 */
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var isHistoryOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(ThemePreferenceManager.getThemeMode(context)) }

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CalculatorTheme(themeMode = themeMode) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Fixed LCD Display Section (fixed height 118.dp)
                    DisplaySection(
                        uiState = uiState,
                        onToggleScientific = viewModel::toggleScientificMode,
                        onToggleAngleUnit = viewModel::toggleAngleUnit,
                        onToggleHistory = { isHistoryOpen = !isHistoryOpen },
                        onOpenSettings = { isSettingsOpen = true },
                        isDarkTheme = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Keypad Section taking remaining height (weight 1f)
                    KeypadSection(
                        isScientificMode = uiState.isScientificMode,
                        angleUnitName = uiState.angleUnit.name,
                        isShiftActive = uiState.isShiftActive,
                        isAlphaActive = uiState.isAlphaActive,
                        isDarkTheme = isDark,
                        onToggleShift = viewModel::toggleShift,
                        onToggleAlpha = viewModel::toggleAlpha,
                        onDigitClick = viewModel::onDigitClick,
                        onOperatorClick = viewModel::onOperatorClick,
                        onFunctionClick = viewModel::onFunctionClick,
                        onParenthesisClick = viewModel::onParenthesisClick,
                        onConstantClick = viewModel::onConstantClick,
                        onClearClick = viewModel::onClearClick,
                        onDeleteClick = viewModel::onDeleteClick,
                        onEqualsClick = viewModel::onEqualsClick,
                        onToggleAngleUnit = viewModel::toggleAngleUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                // History Overlay
                HistoryOverlay(
                    isVisible = isHistoryOpen,
                    historyList = uiState.history,
                    onSelectHistoryItem = viewModel::onHistoryItemSelect,
                    onClearHistory = viewModel::clearHistory,
                    onDismiss = { isHistoryOpen = false },
                    modifier = Modifier.fillMaxWidth()
                )

                // Appearance Settings Dialog
                if (isSettingsOpen) {
                    AppearanceSettingsDialog(
                        currentThemeMode = themeMode,
                        onThemeSelected = { newMode ->
                            themeMode = newMode
                            ThemePreferenceManager.setThemeMode(context, newMode)
                        },
                        onDismiss = { isSettingsOpen = false }
                    )
                }
            }
        }
    }
}
