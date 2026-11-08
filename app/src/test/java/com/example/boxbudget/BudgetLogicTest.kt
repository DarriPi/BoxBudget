package com.example.boxbudget

import com.example.boxbudget.data.Box
import com.example.boxbudget.data.BudgetState
import com.example.boxbudget.data.Item
import com.example.boxbudget.data.amountLeftCents
import com.example.boxbudget.data.applyAutoReset
import com.example.boxbudget.data.budgetCents
import com.example.boxbudget.data.decodeState
import com.example.boxbudget.data.encodeState
import com.example.boxbudget.data.formatRand
import com.example.boxbudget.data.historyToCsv
import com.example.boxbudget.data.lastEmoji
import com.example.boxbudget.data.nextPayday
import com.example.boxbudget.data.ordinal
import com.example.boxbudget.data.parseCents
import com.example.boxbudget.data.resetNow
import com.example.boxbudget.data.sanitizeMoneyInput
import com.example.boxbudget.data.totalBudgetCents
import com.example.boxbudget.data.totalSpentCents
import com.example.boxbudget.data.unboxedCents
import com.example.boxbudget.data.withResetSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetLogicTest {

    private val nov8: LocalDate = LocalDate.of(2026, 11, 8)
    private val day = nov8.toEpochDay()

    private fun item(rand: Long, name: String = "x") = Item(name + rand, name, rand * 100, day)

    /** The exact figures from the design PDF. */
    private val designState = BudgetState(
        salaryCents = 28_000_00,
        boxes = listOf(
            Box("g", "Groceries", "icon:basket", 10, listOf(item(3150))),
            Box("f", "Fun", "icon:game", 10, listOf(item(1240))),
            Box("c", "Charity", "icon:heart", 20, listOf(item(2000))),
            Box("i", "Investment", "icon:trend", 5, listOf(item(1400))),
        ),
    ).withResetSchedule(enabled = true, day = 25, today = nov8)

    @Test
    fun totalsMatchTheDesign() {
        assertEquals(12_600_00L, designState.totalBudgetCents)
        assertEquals(7_790_00L, designState.totalSpentCents)
        assertEquals(15_400_00L, designState.unboxedCents)
        assertEquals(20_210_00L, designState.amountLeftCents)
    }

    @Test
    fun formatsRand() {
        assertEquals("R28 000", formatRand(28_000_00))
        assertEquals("R1 120.50", formatRand(1_120_50))
        assertEquals("R890.00", formatRand(890_00, alwaysCents = true))
        assertEquals("R0", formatRand(0))
        assertEquals("-R5.05", formatRand(-5_05))
    }

    @Test
    fun parsesTypedAmounts() {
        assertEquals(1250L, parseCents("12.50"))
        assertEquals(1250L, parseCents("12,50"))
        assertEquals(1200L, parseCents("12"))
        assertEquals(1200L, parseCents("12."))
        assertEquals(50L, parseCents(".5"))
        assertNull(parseCents(""))
        assertNull(parseCents("."))
        assertNull(parseCents("abc"))
    }

    @Test
    fun sanitizesMoneyInput() {
        assertEquals("12.34", sanitizeMoneyInput("12,345"))
        assertEquals("1.23", sanitizeMoneyInput("1.2.3"))
        assertEquals("100", sanitizeMoneyInput("R1 00"))
    }

    @Test
    fun budgetRoundsToNearestCent() {
        assertEquals(33L, budgetCents(333, 10))
        assertEquals(34L, budgetCents(335, 10))
    }

    @Test
    fun nextPaydayIsStrictlyAfterToday() {
        assertEquals(LocalDate.of(2026, 11, 25), nextPayday(nov8, 25))
        assertEquals(LocalDate.of(2026, 12, 25), nextPayday(LocalDate.of(2026, 11, 25), 25))
    }

    @Test
    fun nextPaydayClampsToShortMonths() {
        assertEquals(LocalDate.of(2027, 2, 28), nextPayday(LocalDate.of(2027, 2, 1), 31))
        assertEquals(LocalDate.of(2027, 3, 31), nextPayday(LocalDate.of(2027, 2, 28), 31))
    }

    @Test
    fun manualResetAfterPaydayKeepsTheCurrentMonthName() {
        val afterPayday = LocalDate.of(2026, 11, 26)
        val state = designState.withResetSchedule(enabled = true, day = 25, today = afterPayday)
        assertEquals("November 2026", state.resetNow(afterPayday).history.first().label)
    }

    @Test
    fun autoResetWaitsForPayday() {
        assertEquals(designState, designState.applyAutoReset(LocalDate.of(2026, 11, 24)))
    }

    @Test
    fun autoResetSavesMonthAndEmptiesBoxes() {
        val after = designState.applyAutoReset(LocalDate.of(2026, 11, 25))
        assertEquals(1, after.history.size)
        val month = after.history.single()
        assertEquals("November 2026", month.label)
        assertEquals(7_790_00L, month.spentCents)
        assertEquals(12_600_00L, month.budgetCents)
        assertEquals(listOf("Groceries"), month.boxes.filter { it.isOver }.map { it.name })
        assertTrue(after.boxes.all { it.items.isEmpty() })
        assertEquals(4, after.boxes.size)
        assertEquals(LocalDate.of(2026, 12, 25).toEpochDay(), after.nextResetEpochDay)
    }

    @Test
    fun missedPaydaysMakeOneRecord() {
        val after = designState.applyAutoReset(LocalDate.of(2027, 3, 2))
        assertEquals(1, after.history.size)
        assertEquals(LocalDate.of(2027, 3, 25).toEpochDay(), after.nextResetEpochDay)
    }

    @Test
    fun autoResetIsOffWhenDisabled() {
        val off = designState.withResetSchedule(enabled = false, day = 25, today = nov8)
        assertEquals(off, off.applyAutoReset(LocalDate.of(2027, 3, 2)))
    }

    @Test
    fun manualResetKeepsTheSchedule() {
        val after = designState.resetNow(nov8)
        assertEquals(1, after.history.size)
        assertEquals(designState.nextResetEpochDay, after.nextResetEpochDay)
    }

    @Test
    fun resetWithNoSpendingSavesNothing() {
        val empty = designState.resetNow(nov8)
        assertEquals(empty, empty.resetNow(nov8))
    }

    @Test
    fun csvHasOneRowPerItemAndEscapesNames() {
        val state = designState.copy(
            boxes = designState.boxes.take(2).mapIndexed { index, box ->
                if (index == 0) {
                    box.copy(items = listOf(item(10, "Milk, \"fresh\""), item(5, "Eggs")))
                } else {
                    box.copy(items = emptyList())
                }
            },
        ).resetNow(nov8)
        val lines = historyToCsv(state.history).trimEnd().split("\r\n")
        assertEquals(4, lines.size)
        assertEquals("month,closed_on,salary,box,box_percent,box_budget,box_spent,box_over_budget,item,item_date,item_price", lines[0])
        assertEquals("November 2026,2026-11-08,28000.00,Groceries,10,2800.00,15.00,no,\"Milk, \"\"fresh\"\"\",2026-11-08,10.00", lines[1])
        assertEquals("November 2026,2026-11-08,28000.00,Fun,10,2800.00,0.00,no,,,", lines[3])
    }

    @Test
    fun jsonRoundTripKeepsEverything() {
        val state = designState.resetNow(nov8).copy(boxes = designState.boxes)
        assertEquals(state, decodeState(encodeState(state)))
        val off = state.withResetSchedule(enabled = false, day = 3, today = nov8)
        assertEquals(off, decodeState(encodeState(off)))
    }

    @Test
    fun newInstallStillNeedsTheTutorial() {
        assertEquals(false, BudgetState().tutorialSeen)
        assertEquals(false, decodeState(encodeState(BudgetState())).tutorialSeen)
        assertEquals(true, decodeState(encodeState(BudgetState(tutorialSeen = true))).tutorialSeen)
    }

    @Test
    fun budgetsSavedBeforeTheTutorialExistedSkipIt() {
        val oldFile = org.json.JSONObject(encodeState(BudgetState())).apply { remove("tutorialSeen") }.toString()
        assertEquals(true, decodeState(oldFile).tutorialSeen)
    }

    @Test
    fun ordinals() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "23rd", "25th", "31st"),
            listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 23, 25, 31).map(::ordinal))
    }

    @Test
    fun emojiFieldIgnoresLetters() {
        assertEquals("🍎", lastEmoji("🍎"))
        assertEquals("🚗", lastEmoji("🍎🚗"))
        assertNull(lastEmoji("a"))
        assertNull(lastEmoji(""))
    }
}
