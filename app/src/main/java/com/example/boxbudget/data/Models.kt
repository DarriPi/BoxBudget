package com.example.boxbudget.data

/** A single purchase. Money is always stored as whole cents to avoid rounding drift. */
data class Item(
    val id: String,
    val name: String,
    val priceCents: Long,
    val epochDay: Long,
)

/** A budget box. [icon] is either "icon:<key>" for a built-in icon, or an emoji. */
data class Box(
    val id: String,
    val name: String,
    val icon: String,
    val percent: Int,
    val items: List<Item> = emptyList(),
) {
    val spentCents: Long get() = items.sumOf { it.priceCents }
}

/** A box as it stood when its month was closed. */
data class BoxRecord(
    val name: String,
    val percent: Int,
    val budgetCents: Long,
    val items: List<Item>,
) {
    val spentCents: Long get() = items.sumOf { it.priceCents }
    val isOver: Boolean get() = spentCents > budgetCents
}

/** A closed month, kept for the History screen and CSV export. */
data class MonthRecord(
    val id: String,
    val label: String,
    val closedOnEpochDay: Long,
    val salaryCents: Long,
    val boxes: List<BoxRecord>,
) {
    val spentCents: Long get() = boxes.sumOf { it.spentCents }
    val budgetCents: Long get() = boxes.sumOf { it.budgetCents }
    val isOver: Boolean get() = spentCents > budgetCents
}

data class BudgetState(
    val salaryCents: Long = 0,
    val boxes: List<Box> = emptyList(),
    /** Newest first. */
    val history: List<MonthRecord> = emptyList(),
    val autoReset: Boolean = true,
    val payday: Int = 25,
    val nextResetEpochDay: Long? = null,
)
