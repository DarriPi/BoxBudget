package com.example.boxbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxbudget.data.Box
import com.example.boxbudget.data.BudgetState
import com.example.boxbudget.data.amountLeftCents
import com.example.boxbudget.data.boxedPercent
import com.example.boxbudget.data.budgetOf
import com.example.boxbudget.data.formatRand
import com.example.boxbudget.data.parseCents
import com.example.boxbudget.data.plainAmount
import com.example.boxbudget.data.sanitizeMoneyInput
import com.example.boxbudget.data.totalBudgetCents
import com.example.boxbudget.data.totalSpentCents
import com.example.boxbudget.data.unboxedCents
import com.example.boxbudget.ui.theme.Card
import com.example.boxbudget.ui.theme.Ink
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Red
import com.example.boxbudget.ui.theme.RedTint
import com.example.boxbudget.ui.theme.Teal
import com.example.boxbudget.ui.theme.TealDeep
import com.example.boxbudget.ui.theme.TealMuted
import com.example.boxbudget.ui.theme.TealTint

@Composable
fun BoxesScreen(
    state: BudgetState,
    monthLabel: String,
    onSetSalary: (Long) -> Unit,
    onOpenBox: (String) -> Unit,
    onAddBox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingSalary by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SalaryHeader(state, monthLabel, onEditSalary = { editingSalary = true })
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your boxes",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    modifier = Modifier.weight(1f),
                )
                val count = state.boxes.size
                Text(
                    "$count ${if (count == 1) "box" else "boxes"} · ${state.boxedPercent}% of salary",
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
            if (state.boxes.isEmpty()) {
                Text(
                    "Set your salary with the pencil above, then add your first box.",
                    color = Muted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            state.boxes.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { box ->
                        BoxCard(
                            box = box,
                            budget = state.budgetOf(box),
                            onClick = { onOpenBox(box.id) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            AddBoxButton(onAddBox)
        }
    }

    if (editingSalary) {
        SalaryDialog(
            current = state.salaryCents,
            onSave = {
                onSetSalary(it)
                editingSalary = false
            },
            onDismiss = { editingSalary = false },
        )
    }
}

@Composable
private fun SalaryHeader(state: BudgetState, monthLabel: String, onEditSalary: () -> Unit) {
    val translucent = Card.copy(alpha = 0.16f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.verticalGradient(listOf(TealDeep, Teal)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ViewInAr, contentDescription = null, tint = Card, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "BoxBudget",
                color = Card,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                monthLabel,
                color = Card,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(translucent)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("Monthly salary", color = Card.copy(alpha = 0.9f), fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatRand(state.salaryCents),
                    color = Card,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    maxLines = 1,
                )
                val left = state.amountLeftCents
                Text(
                    if (left >= 0) "${formatRand(left)} left this month" else "${formatRand(-left)} more than your salary",
                    color = Card,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(translucent)
                    .clickable(onClick = onEditSalary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit salary", tint = Card, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderTile(
                label = "Spent from boxes",
                value = formatRand(state.totalSpentCents),
                suffix = " of ${formatRand(state.totalBudgetCents)}",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            HeaderTile(
                label = "Not in a box yet",
                value = formatRand(state.unboxedCents),
                suffix = " · ${100 - state.boxedPercent}%",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun HeaderTile(label: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Card.copy(alpha = 0.16f))
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Text(label, color = Card.copy(alpha = 0.9f), fontSize = 13.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Card, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(
                suffix,
                color = Card.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BoxCard(box: Box, budget: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spent = box.spentCents
    val over = spent > budget
    AppCard(
        modifier = modifier,
        borderColor = if (over) Red else com.example.boxbudget.ui.theme.Hairline,
        background = if (over) RedTint else Card,
        onClick = onClick,
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(box.icon, background = if (over) Card else TealTint, tint = if (over) Red else Teal)
                Spacer(Modifier.weight(1f))
                Text(
                    "${box.percent}%",
                    color = if (over) Red else Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                box.name,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (over) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    when {
                        over -> "${formatRand(spent - budget)} over"
                        spent == budget && budget > 0 -> "Fully used"
                        else -> "${formatRand(budget - spent)} left"
                    },
                    color = if (over) Red else Teal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(9.dp))
            BudgetBar(spentFraction(spent, budget), if (over) Red else Teal)
            Spacer(Modifier.height(9.dp))
            Text(
                "${formatRand(spent)} of ${formatRand(budget)}",
                color = Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AddBoxButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = TealMuted,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())),
                    ),
                )
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Teal)
        Spacer(Modifier.width(8.dp))
        Text("Add box", color = Teal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SalaryDialog(current: Long, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(if (current > 0) plainAmount(current).removeSuffix(".00") else "") }
    val cents = parseCents(text)
    BudgetDialog("Monthly salary", onDismiss) {
        Field(
            value = text,
            onValueChange = { text = sanitizeMoneyInput(it) },
            placeholder = "0.00",
            keyboardOptions = MoneyKeyboard,
            keyboardActions = KeyboardActions(onDone = { cents?.let(onSave) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
            TextButton(onClick = { cents?.let(onSave) }, enabled = cents != null) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}
