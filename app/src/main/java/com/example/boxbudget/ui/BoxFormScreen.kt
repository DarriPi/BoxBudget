package com.example.boxbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxbudget.data.Box
import com.example.boxbudget.data.budgetCents
import com.example.boxbudget.data.formatRand
import com.example.boxbudget.data.lastEmoji
import com.example.boxbudget.ui.theme.Card
import com.example.boxbudget.ui.theme.Hairline
import com.example.boxbudget.ui.theme.Ink
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Red
import com.example.boxbudget.ui.theme.Teal
import com.example.boxbudget.ui.theme.TealMuted
import com.example.boxbudget.ui.theme.TealTint
import com.example.boxbudget.ui.theme.Track
import kotlin.math.roundToInt

/** Creates a box, or edits [existing] when one is passed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxFormScreen(
    existing: Box?,
    otherBoxesPercent: Int,
    salaryCents: Long,
    onBack: () -> Unit,
    onSave: (name: String, icon: String, percent: Int) -> Unit,
    onDelete: () -> Unit,
) {
    val maxPercent = 100 - otherBoxesPercent
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: BoxIcons.first().first) }
    var percent by rememberSaveable { mutableIntStateOf((existing?.percent ?: 10).coerceIn(0, maxPercent)) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SquareIconButton(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Back", onBack)
                Spacer(Modifier.width(14.dp))
                Text(
                    if (existing == null) "New box" else "Edit box",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Box name")
                Field(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Transport",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            IconPicker(icon, onSelect = { icon = it })
            ShareCard(
                percent = percent,
                maxPercent = maxPercent,
                otherBoxesPercent = otherBoxesPercent,
                salaryCents = salaryCents,
                onPercentChange = { percent = it },
            )
            if (maxPercent == 0) {
                Text(
                    "All of your salary is already in boxes. Lower another box to make room.",
                    color = Red,
                    fontSize = 14.sp,
                )
            }
        }
        if (existing != null) {
            TextButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Delete box", color = Red, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        PrimaryButton(
            text = if (existing == null) "Create box" else "Save changes",
            onClick = { onSave(name, icon, percent) },
            enabled = name.isNotBlank() && percent >= 1,
        )
    }

    if (confirmingDelete && existing != null) {
        ConfirmDialog(
            title = "Delete ${existing.name}?",
            message = "This removes the box and this month's items in it. Saved months in History stay.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    val emojiFocus = remember { FocusRequester() }
    val shape = RoundedCornerShape(14.dp)
    val selectedEmoji = selected.takeUnless(::isPresetIcon)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Icon")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BoxIcons.forEach { (key, _) ->
                val isSelected = key == selected
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(shape)
                        .background(if (isSelected) TealTint else Card)
                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Teal else Hairline, shape)
                        .clickable { onSelect(key) },
                    contentAlignment = Alignment.Center,
                ) {
                    BoxGlyph(key, if (isSelected) Teal else Muted)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Or pick any emoji from your keyboard",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f).clickable { emojiFocus.requestFocus() },
            )
            Box(
                Modifier
                    .size(46.dp)
                    .clip(shape)
                    .background(if (selectedEmoji != null) TealTint else Card)
                    .border(if (selectedEmoji != null) 1.5.dp else 1.dp, if (selectedEmoji != null) Teal else Hairline, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedEmoji == null) {
                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Pick an emoji", tint = Muted, modifier = Modifier.size(22.dp))
                }
                BasicTextField(
                    value = selectedEmoji ?: "",
                    onValueChange = { typed -> lastEmoji(typed)?.let(onSelect) },
                    singleLine = true,
                    cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                    textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().focusRequester(emojiFocus),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareCard(
    percent: Int,
    maxPercent: Int,
    otherBoxesPercent: Int,
    salaryCents: Long,
    onPercentChange: (Int) -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Share of salary", color = Muted, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$percent%",
                    color = Teal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 44.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(formatRand(budgetCents(salaryCents, percent)), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(" / month", color = Muted, fontSize = 15.sp)
            }
            Slider(
                value = percent.toFloat(),
                onValueChange = { onPercentChange(it.roundToInt().coerceAtLeast(if (maxPercent > 0) 1 else 0)) },
                valueRange = 0f..maxPercent.coerceAtLeast(1).toFloat(),
                enabled = maxPercent > 0,
                thumb = { Box(Modifier.size(22.dp).background(Teal, CircleShape)) },
                track = { sliderState ->
                    val range = sliderState.valueRange
                    val fraction = (sliderState.value - range.start) / (range.endInclusive - range.start)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Track)) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Teal),
                        )
                    }
                },
            )
            Spacer(Modifier.height(4.dp))
            SalaryShareBar(otherBoxesPercent, percent)
            Spacer(Modifier.height(10.dp))
            val boxed = otherBoxesPercent + percent
            Text("$boxed% of salary boxed · ${100 - boxed}% still free", color = Muted, fontSize = 14.sp)
        }
    }
}

/** Light segment is the other boxes, dark segment is this box, the rest is free. */
@Composable
private fun SalaryShareBar(otherPercent: Int, thisPercent: Int) {
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Track)) {
        if (otherPercent > 0) Box(Modifier.weight(otherPercent.toFloat()).height(10.dp).background(TealMuted))
        if (thisPercent > 0) Box(Modifier.weight(thisPercent.toFloat()).height(10.dp).background(Teal))
        val free = 100 - otherPercent - thisPercent
        if (free > 0) Spacer(Modifier.weight(free.toFloat()))
    }
}
