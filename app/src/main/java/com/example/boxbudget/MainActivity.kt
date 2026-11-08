package com.example.boxbudget

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.boxbudget.ui.BoxBudgetApp
import com.example.boxbudget.ui.BudgetViewModel
import com.example.boxbudget.ui.theme.BoxBudgetTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            BoxBudgetTheme {
                BoxBudgetApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
