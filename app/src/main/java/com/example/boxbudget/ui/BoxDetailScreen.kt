package com.example.boxbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxbudget.data.Box
import com.example.boxbudget.data.Item
import com.example.boxbudget.data.formatRand
import com.example.boxbudget.data.parseCents
import com.example.boxbudget.data.plainAmount
import com.example.boxbudget.data.sanitizeMoneyInput
import com.example.boxbudget.data.shortDate
import com.example.boxbudget.ui.theme.Hairline
import com.example.boxbudget.ui.theme.Ink
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Red
import com.example.boxbudget.ui.theme.RedTint
import com.example.boxbudget.ui.theme.Teal
import com.example.boxbudget.ui.theme.TealTint
import java.time.LocalDate

@Composable
fun BoxDetailScreen(
    box: Box,
    budget: Long,
    onBack: () -> Unit,
    onEditBox: () -> Unit,
    onAddItem: (String, Long) -> Unit,
    onUpdateItem: (String, String, Long) -> Unit,
    onDeleteItem: (String) -> Unit,
) {
    val spent = box.spentCents
    val over = spent > budget
    var editingItemId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row {
            SquareIconButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Back", onBack)
            Spacer(Modifier.weight(1f))
            SquareIconButton(Icons.Outlined.Edit, "Edit box", onEditBox)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(box.icon, background = if (over) RedTint else TealTint, tint = if (over) Red else Teal, size = 54.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    box.name,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${box.percent}% of salary · ${formatRand(budget)} a month", color = Muted, fontSize = 15.sp)
            }
        }
        SpentCard(box.name, spent, budget)
        AddItemCard(box.name, onAddItem)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "This month",
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
            )
            if (box.items.isNotEmpty()) {
                Text("Tap an item to edit or remove", color = Muted, fontSize = 13.sp)
            }
        }
        if (box.items.isEmpty()) {
            Text("Nothing bought from this box yet.", color = Muted, fontSize = 15.sp)
        } else {
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    box.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = Hairline)
                        ItemRow(item, onClick = { editingItemId = item.id })
                    }
                }
            }
        }
    }

    box.items.firstOrNull { it.id == editingItemId }?.let { item ->
        EditItemDialog(
            item = item,
            onSave = { name, price ->
                onUpdateItem(item.id, name, price)
                editingItemId = null
            },
            onDelete = {
                onDeleteItem(item.id)
                editingItemId = null
            },
            onDismiss = { editingItemId = null },
        )
    }
}

@Composable
private fun SpentCard(boxName: String, spent: Long, budget: Long) {
    val over = spent > budget
    AppCard(Modifier.fillMaxWidth(), borderColor = if (over) Red else Hairline) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Spent this month", color = Muted, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatRand(spent),
                    color = if (over) Red else Teal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text("of ${formatRand(budget)}", color = Muted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            BudgetBar(spentFraction(spent, budget), if (over) Red else Teal)
            if (over) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RedTint)
                        .padding(12.dp),
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "You spent ${formatRand(spent - budget)} more than your $boxName budget.",
                        color = Red,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddItemCard(boxName: String, onAdd: (String, Long) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val cents = parseCents(price)?.takeIf { it > 0 }
    val valid = name.isNotBlank() && cents != null
    val submit = {
        if (valid) {
            onAdd(name, cents!!)
            name = ""
            price = ""
            focus.clearFocus()
        }
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add item", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1.7f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Item")
                    Field(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "e.g. Weekly shop",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Price (R)")
                    Field(
                        value = price,
                        onValueChange = { price = sanitizeMoneyInput(it) },
                        placeholder = "0.00",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                }
            }
            PrimaryButton("Add to $boxName", onClick = { submit() }, enabled = valid)
        }
    }
}

@Composable
internal fun ItemRow(item: Item, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(shortDate(LocalDate.ofEpochDay(item.epochDay)), color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(formatRand(item.priceCents, alwaysCents = true), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun EditItemDialog(
    item: Item,
    onSave: (String, Long) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var price by remember(item.id) { mutableStateOf(plainAmount(item.priceCents)) }
    val cents = parseCents(price)?.takeIf { it > 0 }
    val valid = name.isNotBlank() && cents != null

    BudgetDialog("Edit item", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldLabel("Item")
            Field(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldLabel("Price (R)")
            Field(
                value = price,
                onValueChange = { price = sanitizeMoneyInput(it) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = MoneyKeyboard,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDelete) { Text("Remove", color = Red, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
            TextButton(onClick = { if (valid) onSave(name, cents!!) }, enabled = valid) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}
