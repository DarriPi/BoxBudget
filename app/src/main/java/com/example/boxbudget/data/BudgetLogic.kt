package com.example.boxbudget.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

private const val NBSP = ' '
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

// ---- Money ----------------------------------------------------------------

/** "R28 000", "R1 120.50". Cents are shown only when needed unless [alwaysCents] is set. */
fun formatRand(cents: Long, alwaysCents: Boolean = false): String {
    val magnitude = abs(cents)
    val grouped = (magnitude / 100).toString().reversed().chunked(3).joinToString("$NBSP").reversed()
    val fraction = magnitude % 100
    val decimals = if (alwaysCents || fraction != 0L) ".${fraction.toString().padStart(2, '0')}" else ""
    return "${if (cents < 0) "-" else ""}R$grouped$decimals"
}

/** "1120.50" with no grouping or currency: for pre-filling inputs and for CSV. */
fun plainAmount(cents: Long): String =
    BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString()

/** Parses what a person typed ("12.50" or "12,50") into cents, or null if it isn't an amount. */
fun parseCents(text: String): Long? {
    val cleaned = text.trim().replace(NBSP.toString(), "").replace(" ", "").replace(',', '.')
    if (cleaned.isEmpty() || cleaned == ".") return null
    return try {
        val cents = BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        cents.takeIf { it >= 0 }
    } catch (_: NumberFormatException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

/** Keeps a money field to digits, one decimal separator and two decimals. */
fun sanitizeMoneyInput(text: String): String {
    val normalized = text.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = normalized.indexOf('.')
    if (dot < 0) return normalized.take(MAX_WHOLE_DIGITS)
    val whole = normalized.substring(0, dot).take(MAX_WHOLE_DIGITS)
    val decimals = normalized.substring(dot + 1).replace(".", "").take(2)
    return "$whole.$decimals"
}

private const val MAX_WHOLE_DIGITS = 10

// ---- Budgets --------------------------------------------------------------

fun budgetCents(salaryCents: Long, percent: Int): Long = (salaryCents * percent + 50) / 100

fun BudgetState.budgetOf(box: Box): Long = budgetCents(salaryCents, box.percent)
val BudgetState.boxedPercent: Int get() = boxes.sumOf { it.percent }
val BudgetState.totalBudgetCents: Long get() = boxes.sumOf { budgetOf(it) }
val BudgetState.totalSpentCents: Long get() = boxes.sumOf { it.spentCents }
val BudgetState.unboxedCents: Long get() = salaryCents - totalBudgetCents

/** What is left of the salary after everything bought so far. */
val BudgetState.amountLeftCents: Long get() = salaryCents - totalSpentCents

// ---- Dates ----------------------------------------------------------------

fun monthLabel(date: LocalDate): String = date.format(MONTH_FORMAT)

fun shortDate(date: LocalDate): String = date.format(SHORT_DATE_FORMAT)

fun ordinal(day: Int): String {
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

private fun YearMonth.payday(day: Int): LocalDate = atDay(min(day, lengthOfMonth()))

/** The first payday strictly after [date]. Short months use their last day. */
fun nextPayday(date: LocalDate, payday: Int): LocalDate {
    val month = YearMonth.from(date)
    val candidate = month.payday(payday)
    return if (candidate.isAfter(date)) candidate else month.plusMonths(1).payday(payday)
}

// ---- Month rollover -------------------------------------------------------

private fun BudgetState.closeMonth(label: String, closedOn: LocalDate): BudgetState {
    val record = MonthRecord(
        id = UUID.randomUUID().toString(),
        label = label,
        closedOnEpochDay = closedOn.toEpochDay(),
        salaryCents = salaryCents,
        boxes = boxes.map { BoxRecord(it.name, it.percent, budgetOf(it), it.items) },
    )
    return copy(boxes = boxes.map { it.copy(items = emptyList()) }, history = listOf(record) + history)
}

val BudgetState.hasSpending: Boolean get() = boxes.any { it.items.isNotEmpty() }

/** Saves this month to history and empties every box. Does nothing if nothing was bought. */
fun BudgetState.resetNow(today: LocalDate): BudgetState =
    if (hasSpending) closeMonth(monthLabel(today), today) else this

/**
 * Runs the payday reset if one is due. The app has no background job, so this runs whenever the
 * app opens or changes. Several missed paydays produce one record, not a run of empty months.
 */
fun BudgetState.applyAutoReset(today: LocalDate): BudgetState {
    if (!autoReset) return this
    val upcoming = nextPayday(today, payday).toEpochDay()
    val due = nextResetEpochDay?.let(LocalDate::ofEpochDay) ?: return copy(nextResetEpochDay = upcoming)
    if (today.isBefore(due)) return this
    val closed = if (hasSpending) closeMonth(monthLabel(due), due) else this
    return closed.copy(nextResetEpochDay = upcoming)
}

fun BudgetState.withResetSchedule(enabled: Boolean, day: Int, today: LocalDate): BudgetState =
    copy(
        autoReset = enabled,
        payday = day,
        nextResetEpochDay = if (enabled) nextPayday(today, day).toEpochDay() else null,
    )

// ---- CSV ------------------------------------------------------------------

private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

/** One row per item, so it pivots easily. A box with no items still gets one row. */
fun historyToCsv(history: List<MonthRecord>): String {
    val header = listOf(
        "month", "closed_on", "salary", "box", "box_percent", "box_budget", "box_spent",
        "box_over_budget", "item", "item_date", "item_price",
    )
    val rows = history.flatMap { month ->
        month.boxes.flatMap { box ->
            val shared = listOf(
                month.label,
                LocalDate.ofEpochDay(month.closedOnEpochDay).toString(),
                plainAmount(month.salaryCents),
                box.name,
                box.percent.toString(),
                plainAmount(box.budgetCents),
                plainAmount(box.spentCents),
                if (box.isOver) "yes" else "no",
            )
            if (box.items.isEmpty()) {
                listOf(shared + listOf("", "", ""))
            } else {
                box.items.map { item ->
                    shared + listOf(
                        item.name,
                        LocalDate.ofEpochDay(item.epochDay).toString(),
                        plainAmount(item.priceCents),
                    )
                }
            }
        }
    }
    return (listOf(header) + rows).joinToString("\r\n", postfix = "\r\n") { row ->
        row.joinToString(",", transform = ::csvCell)
    }
}

// ---- Emoji ----------------------------------------------------------------

private fun isEmojiLike(text: String): Boolean {
    val first = text.codePointAt(0)
    return first > 0x2000 && !Character.isLetterOrDigit(first)
}

/** The last emoji in [text], or null if it doesn't end in one. Used by the emoji icon field. */
fun lastEmoji(text: String): String? {
    if (text.isEmpty()) return null
    val breaks = java.text.BreakIterator.getCharacterInstance(Locale.ROOT)
    breaks.setText(text)
    val end = breaks.last()
    val start = breaks.previous()
    if (start == java.text.BreakIterator.DONE) return null
    return text.substring(start, end).takeIf(::isEmojiLike)
}
