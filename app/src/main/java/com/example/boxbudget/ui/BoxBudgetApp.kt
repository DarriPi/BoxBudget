package com.example.boxbudget.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.boxbudget.data.boxedPercent
import com.example.boxbudget.data.budgetOf
import com.example.boxbudget.data.currentMonthLabel
import com.example.boxbudget.ui.theme.Card
import com.example.boxbudget.ui.theme.Hairline
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Page
import com.example.boxbudget.ui.theme.Teal

private sealed interface Route {
    data object Boxes : Route
    data object History : Route
    data object NewBox : Route
    data class BoxDetail(val id: String) : Route
    data class EditBox(val id: String) : Route
}

private val RouteSaver = Saver<Route, String>(
    save = { route ->
        when (route) {
            Route.Boxes -> "boxes"
            Route.History -> "history"
            Route.NewBox -> "new"
            is Route.BoxDetail -> "box:${route.id}"
            is Route.EditBox -> "edit:${route.id}"
        }
    },
    restore = { key ->
        when {
            key == "history" -> Route.History
            key == "new" -> Route.NewBox
            key.startsWith("box:") -> Route.BoxDetail(key.removePrefix("box:"))
            key.startsWith("edit:") -> Route.EditBox(key.removePrefix("edit:"))
            else -> Route.Boxes
        }
    },
)

@Composable
fun BoxBudgetApp(viewModel: BudgetViewModel) {
    val state by viewModel.state.collectAsState()
    val today by viewModel.today.collectAsState()
    var route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf<Route>(Route.Boxes) }
    val monthLabel = state.currentMonthLabel(today)

    // The Boxes header is teal and runs under the status bar, so it needs light status icons.
    val view = LocalView.current
    val darkStatusIcons = route != Route.Boxes || !state.tutorialSeen
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkStatusIcons
    }

    BackHandler(enabled = route != Route.Boxes) {
        route = when (val current = route) {
            is Route.EditBox -> Route.BoxDetail(current.id)
            else -> Route.Boxes
        }
    }

    if (!state.tutorialSeen) {
        TutorialScreen(onFinish = viewModel::finishTutorial)
        return
    }

    Column(Modifier.fillMaxSize().background(Page)) {
        val contentModifier = Modifier.weight(1f)
        when (val current = route) {
            Route.Boxes -> BoxesScreen(
                state = state,
                monthLabel = monthLabel,
                onSetSalary = viewModel::setSalary,
                onOpenBox = { route = Route.BoxDetail(it) },
                onAddBox = { route = Route.NewBox },
                modifier = contentModifier,
            )

            Route.History -> HistoryScreen(
                state = state,
                today = today,
                monthLabel = monthLabel,
                onAutoResetChange = viewModel::setAutoReset,
                onPaydayChange = viewModel::setPayday,
                onResetNow = viewModel::resetNow,
                onExport = viewModel::exportCsv,
                modifier = contentModifier,
            )

            Route.NewBox -> BoxFormScreen(
                existing = null,
                otherBoxesPercent = state.boxedPercent,
                salaryCents = state.salaryCents,
                onBack = { route = Route.Boxes },
                onSave = { name, icon, percent ->
                    viewModel.addBox(name, icon, percent)
                    route = Route.Boxes
                },
                onDelete = {},
            )

            is Route.BoxDetail -> {
                val box = state.boxes.firstOrNull { it.id == current.id }
                if (box == null) {
                    LaunchedEffect(Unit) { route = Route.Boxes }
                } else {
                    BoxDetailScreen(
                        box = box,
                        budget = state.budgetOf(box),
                        onBack = { route = Route.Boxes },
                        onEditBox = { route = Route.EditBox(box.id) },
                        onAddItem = { name, price -> viewModel.addItem(box.id, name, price) },
                        onUpdateItem = { itemId, name, price -> viewModel.updateItem(box.id, itemId, name, price) },
                        onDeleteItem = { itemId -> viewModel.deleteItem(box.id, itemId) },
                    )
                }
            }

            is Route.EditBox -> {
                val box = state.boxes.firstOrNull { it.id == current.id }
                if (box == null) {
                    LaunchedEffect(Unit) { route = Route.Boxes }
                } else {
                    BoxFormScreen(
                        existing = box,
                        otherBoxesPercent = state.boxedPercent - box.percent,
                        salaryCents = state.salaryCents,
                        onBack = { route = Route.BoxDetail(box.id) },
                        onSave = { name, icon, percent ->
                            viewModel.updateBox(box.id, name, icon, percent)
                            route = Route.BoxDetail(box.id)
                        },
                        onDelete = {
                            viewModel.deleteBox(box.id)
                            route = Route.Boxes
                        },
                    )
                }
            }
        }
        if (route == Route.Boxes || route == Route.History) {
            BottomBar(
                onBoxes = route == Route.Boxes,
                onSelectBoxes = { route = Route.Boxes },
                onSelectHistory = { route = Route.History },
            )
        }
    }
}

@Composable
private fun BottomBar(onBoxes: Boolean, onSelectBoxes: () -> Unit, onSelectHistory: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Card).navigationBarsPadding()) {
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth()) {
            BottomTab("Boxes", Icons.Outlined.GridView, onBoxes, onSelectBoxes, Modifier.weight(1f))
            BottomTab("History", Icons.Outlined.Schedule, !onBoxes, onSelectHistory, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) Teal else Muted
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
