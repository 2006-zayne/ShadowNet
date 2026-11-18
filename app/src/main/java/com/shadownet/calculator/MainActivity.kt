package com.shadownet.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.shadownet.calculator.security.SampleSecurityTriggerEvaluator
import com.shadownet.calculator.ui.CalculatorScreen
import com.shadownet.calculator.ui.CalculatorViewModel

/**
 * Main Activity hosting the Scientific Calculator Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val calculatorViewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Extensibility Security Hook Layer.
        calculatorViewModel.setTriggerEvaluator(
            SampleSecurityTriggerEvaluator(secretPasscode = "7777")
        )

        // Wire up SHIFT + AC power-off shortcut to exit app
        calculatorViewModel.onPowerOff = {
            finishAndRemoveTask()
        }

        setContent {
            CalculatorScreen(viewModel = calculatorViewModel)
        }
    }
}
