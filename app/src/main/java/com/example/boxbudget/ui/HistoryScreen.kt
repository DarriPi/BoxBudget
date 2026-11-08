package com.example.boxbudget.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxbudget.data.BudgetState
import com.example.boxbudget.data.MonthRecord
import com.example.boxbudget.data.formatRand
import com.example.boxbudget.data.hasSpending
import com.example.boxbudget.data.ordinal
import com.example.boxbudget.data.shortDate
import com.example.boxbudget.ui.theme.Card
import com.example.boxbudget.ui.theme.Hairline
import com.example.boxbudget.ui.theme.Ink
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Red
import com.example.boxbudget.ui.theme.RedTint
import com.example.boxbudget.ui.theme.Teal
import com.example.boxbudget.ui.theme.TealTint
import java.time.LocalDate

@Composable
fun HistoryScreen(
    state: BudgetState,
    today: LocalDate,
    monthLabel: String,
    onAutoResetChange: (Boolean) -> Unit,
    onPaydayChange: (Int) -> Unit,
    onResetNow: () -> Unit,
    onExport: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var pickingPayday by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(onExport) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "History",
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("boxbudget-history-$today.csv") },
                enabled = state.history.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Hairline),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Card, contentColor = Teal),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export CSV", fontWeight = FontWeight.SemiBold)
            }
        }
        ResetCard(
            state = state,
            onAutoResetChange = onAutoResetChange,
            onPickPayday = { pickingPayday = true },
            onResetNow = { confirmingReset = true },
        )
        Text(
            "Past months",
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (state.history.isEmpty()) {
            Text("Months appear here after each reset.", color = Muted, fontSize = 15.sp)
        }
        state.history.forEach { MonthCard(it) }
    }

    if (confirmingReset) {
        ConfirmDialog(
            title = "Reset now?",
            message = "This saves $monthLabel to History and empties every box. Your boxes and salary stay.",
            confirmLabel = "Reset",
            destructive = false,
            onConfirm = {
                onResetNow()
                confirmingReset = false
            },
            onDismiss = { confirmingReset = false },
        )
    }
    if (pickingPayday) {
        PaydayDialog(
            selected = state.payday,
            onSelect = {
                onPaydayChange(it)
                pickingPayday = false
            },
            onDismiss = { pickingPayday = false },
        )
    }
}

@Composable
private fun ResetCard(
    state: BudgetState,
    onAutoResetChange: (Boolean) -> Unit,
    onPickPayday: () -> Unit,
    onResetNow: () -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = state.autoReset, onClick = onPickPayday),
                ) {
                    Text("Reset every payday", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val next = state.nextResetEpochDay?.let { shortDate(LocalDate.ofEpochDay(it)) }
                    Text(
                        if (state.autoReset && next != null) {
                            "On the ${ordinal(state.payday)} · next reset $next"
                        } else {
                            "Off · reset by hand below"
                        },
                        color = Muted,
                        fontSize = 14.sp,
                    )
                }
                Checkbox(
                    checked = state.autoReset,
                    onCheckedChange = onAutoResetChange,
                    colors = CheckboxDefaults.colors(checkedColor = Teal),
                )
            }
            OutlinedButton(
                onClick = onResetNow,
                enabled = state.hasSpending,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, if (state.hasSpending) Teal else Hairline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal),
            ) {
                Text("Reset now and save this month", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
internal fun MonthCard(month: MonthRecord) {
    val overBoxes = month.boxes.filter { it.isOver }.map { it.name }
    AppCard(Modifier.fillMaxWidth(), borderColor = if (month.isOver) Red else Hairline) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    month.label,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatRand(month.spentCents),
                    color = if (month.isOver) Red else Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(" / ${formatRand(month.budgetCents)}", color = Muted, fontSize = 15.sp)
            }
            BudgetBar(spentFraction(month.spentCents, month.budgetCents), if (month.isOver) Red else Teal)
            if (overBoxes.isEmpty()) {
                StatusChip("All boxes on budget", TealTint, Teal)
            } else {
                StatusChip("Over: ${overBoxes.joinToString(", ")}", RedTint, Red)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, background: Color, color: Color) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

@Composable
private fun PaydayDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    BudgetDialog("Payday", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..31).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { day ->
                        val isSelected = day == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Teal else TealTint.copy(alpha = 0.5f))
                                .clickable { onSelect(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day",
                                color = if (isSelected) Card else Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text("In shorter months the reset happens on the last day.", color = Muted, fontSize = 13.sp)
    }
}
