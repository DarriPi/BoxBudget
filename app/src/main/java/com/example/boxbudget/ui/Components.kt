package com.example.boxbudget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.boxbudget.ui.theme.Card
import com.example.boxbudget.ui.theme.Hairline
import com.example.boxbudget.ui.theme.Ink
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Red
import com.example.boxbudget.ui.theme.Teal
import com.example.boxbudget.ui.theme.TealTint
import com.example.boxbudget.ui.theme.Track

val BoxIcons: List<Pair<String, ImageVector>> = listOf(
    "icon:car" to Icons.Outlined.DirectionsCar,
    "icon:home" to Icons.Outlined.Home,
    "icon:book" to Icons.AutoMirrored.Outlined.MenuBook,
    "icon:phone" to Icons.Outlined.Smartphone,
    "icon:gift" to Icons.Outlined.CardGiftcard,
    "icon:basket" to Icons.Outlined.ShoppingBasket,
    "icon:game" to Icons.Outlined.SportsEsports,
    "icon:heart" to Icons.Outlined.FavoriteBorder,
    "icon:trend" to Icons.AutoMirrored.Outlined.TrendingUp,
)

fun isPresetIcon(icon: String): Boolean = icon.startsWith("icon:")

/** Draws a box's built-in icon or emoji. */
@Composable
fun BoxGlyph(icon: String, tint: Color, size: Dp = 24.dp) {
    val vector = BoxIcons.firstOrNull { it.first == icon }?.second
    if (vector != null) {
        Icon(vector, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    } else {
        Text(icon, fontSize = (size.value - 2).sp)
    }
}

@Composable
fun IconTile(icon: String, background: Color, tint: Color, size: Dp = 48.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        BoxGlyph(icon, tint)
    }
}

/** A white rounded card. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Hairline,
    background: Color = Card,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val width = if (borderColor == Hairline) 1.dp else 1.5.dp
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = background,
        border = BorderStroke(width, borderColor),
        content = content,
    )
}

@Composable
fun BudgetBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = TealTint) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}

fun spentFraction(spent: Long, budget: Long): Float = when {
    budget > 0 -> (spent.toFloat() / budget).coerceIn(0f, 1f)
    spent > 0 -> 1f
    else -> 0f
}

@Composable
fun SquareIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = Muted, maxLines = 1) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Hairline,
            focusedBorderColor = Teal,
            unfocusedContainerColor = Card,
            focusedContainerColor = Card,
        ),
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}

val MoneyKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

/** The rounded dialog used for salary, item editing and confirmations. */
@Composable
fun BudgetDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Card) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BudgetDialog(title, onDismiss) {
        Text(message, color = Muted, fontSize = 15.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) Red else Teal, fontWeight = FontWeight.Bold)
            }
        }
    }
}
