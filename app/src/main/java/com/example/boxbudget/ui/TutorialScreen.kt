package com.example.boxbudget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxbudget.data.Box
import com.example.boxbudget.data.BoxRecord
import com.example.boxbudget.data.Item
import com.example.boxbudget.data.MonthRecord
import com.example.boxbudget.data.budgetCents
import com.example.boxbudget.data.formatRand
import com.example.boxbudget.ui.theme.Card
import com.example.boxbudget.ui.theme.Hairline
import com.example.boxbudget.ui.theme.Ink
import com.example.boxbudget.ui.theme.Muted
import com.example.boxbudget.ui.theme.Page
import com.example.boxbudget.ui.theme.Teal
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val SAMPLE_SALARY = 28_000_00L
private val sampleDay = LocalDate.of(2026, 11, 8).toEpochDay()

private fun sampleItem(name: String, cents: Long) = Item(name, name, cents, sampleDay)

private class TutorialStep(
    val title: String,
    val body: String,
    val illustration: @Composable () -> Unit,
)

private val tutorialSteps = listOf(
    TutorialStep(
        title = "Welcome to BoxBudget",
        body = "Split your salary into boxes for things like groceries and fun, and always see what's left. " +
            "Start by tapping the pencil to enter your salary.",
        illustration = { SalaryIllustration() },
    ),
    TutorialStep(
        title = "Make a box",
        body = "Tap Add box, give it a name and choose its share of your salary. " +
            "BoxBudget works out the amount for you. Try the slider.",
        illustration = { SliderIllustration() },
    ),
    TutorialStep(
        title = "Add what you buy",
        body = "Open a box, type the item and its price, then tap Add. " +
            "Made a mistake? Tap the item to change or remove it.",
        illustration = { ItemsIllustration() },
    ),
    TutorialStep(
        title = "Red means overspent",
        body = "A box turns red when you spend more than its budget, so you can spot it at a glance.",
        illustration = { BoxesIllustration() },
    ),
    TutorialStep(
        title = "A fresh start each month",
        body = "On payday your boxes empty and the month is saved in History. " +
            "You can change the day there, reset by hand or export a spreadsheet.",
        illustration = { HistoryIllustration() },
    ),
)

/** The walkthrough shown once, on first launch. Finishing or skipping it marks it as seen. */
@Composable
fun TutorialScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { tutorialSteps.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == tutorialSteps.lastIndex

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Page)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isLast) {
                TextButton(onClick = onFinish) {
                    Text("Skip", color = Muted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            pageSpacing = 24.dp,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            StepPage(tutorialSteps[page])
        }
        StepDots(current = pager.currentPage, count = tutorialSteps.size)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = if (isLast) "Get started" else "Next",
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            },
        )
    }
}

@Composable
private fun StepPage(step: TutorialStep) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        step.illustration()
        Spacer(Modifier.height(30.dp))
        Text(
            step.title,
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            step.body,
            color = Muted,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepDots(current: Int, count: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (selected) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) Teal else Hairline),
            )
        }
    }
}

@Composable
private fun SalaryIllustration() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Teal)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Monthly salary", color = Card.copy(alpha = 0.9f), fontSize = 15.sp)
            Text(formatRand(SAMPLE_SALARY), color = Card, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp)
            Text("${formatRand(20_210_00)} left this month", color = Card, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Card.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Card, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SliderIllustration() {
    var percent by rememberSaveable { mutableIntStateOf(15) }
    ShareCard(
        percent = percent,
        maxPercent = 55,
        otherBoxesPercent = 45,
        salaryCents = SAMPLE_SALARY,
        onPercentChange = { percent = it },
    )
}

@Composable
private fun ItemsIllustration() {
    AppCard(Modifier.fillMaxWidth()) {
        Column {
            ItemRow(sampleItem("Weekly shop", 640_00), onClick = null)
            HorizontalDivider(color = Hairline)
            ItemRow(sampleItem("Bread and milk", 499_50), onClick = null)
            HorizontalDivider(color = Hairline)
            ItemRow(sampleItem("Meat and veg", 890_00), onClick = null)
        }
    }
}

@Composable
private fun BoxesIllustration() {
    val overspent = Box("over", "Groceries", "icon:basket", 10, listOf(sampleItem("Shopping", 3_150_00)))
    val onBudget = Box("fine", "Fun", "icon:game", 10, listOf(sampleItem("Cinema", 1_240_00)))
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(overspent, onBudget).forEach { box ->
            BoxCard(
                box = box,
                budget = budgetCents(SAMPLE_SALARY, box.percent),
                onClick = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun HistoryIllustration() {
    val budget = budgetCents(SAMPLE_SALARY, 10)
    MonthCard(
        MonthRecord(
            id = "sample",
            label = "October 2026",
            closedOnEpochDay = sampleDay,
            salaryCents = SAMPLE_SALARY,
            boxes = listOf(
                BoxRecord("Groceries", 10, budget, listOf(sampleItem("Shopping", 3_000_00))),
                BoxRecord("Fun", 10, budget, listOf(sampleItem("Cinema", 1_500_00))),
            ),
        ),
    )
}
