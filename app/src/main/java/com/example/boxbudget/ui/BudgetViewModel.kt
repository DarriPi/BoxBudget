package com.example.boxbudget.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.boxbudget.data.Box
import com.example.boxbudget.data.BudgetState
import com.example.boxbudget.data.BudgetStore
import com.example.boxbudget.data.Item
import com.example.boxbudget.data.applyAutoReset
import com.example.boxbudget.data.historyToCsv
import com.example.boxbudget.data.resetNow
import com.example.boxbudget.data.withResetSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val store = BudgetStore(File(application.filesDir, "budget.json"))
    private val _state = MutableStateFlow(store.load().applyAutoReset(LocalDate.now()))
    private val _today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<BudgetState> = _state.asStateFlow()
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    init {
        // StateFlow conflates, so a slow disk skips stale states and always ends on the latest.
        viewModelScope.launch(Dispatchers.IO) {
            _state.collect { store.save(it) }
        }
    }

    /** Called on resume, since a payday may have passed while the app was in the background. */
    fun refresh() {
        _today.value = LocalDate.now()
        mutate { it }
    }

    private fun mutate(change: (BudgetState) -> BudgetState) {
        _state.update { change(it.applyAutoReset(LocalDate.now())) }
    }

    private fun mutateBox(boxId: String, change: (Box) -> Box) = mutate { state ->
        state.copy(boxes = state.boxes.map { if (it.id == boxId) change(it) else it })
    }

    fun finishTutorial() = mutate { it.copy(tutorialSeen = true) }

    fun setSalary(cents: Long) = mutate { it.copy(salaryCents = cents) }

    fun addBox(name: String, icon: String, percent: Int) = mutate {
        it.copy(boxes = it.boxes + Box(UUID.randomUUID().toString(), name.trim(), icon, percent))
    }

    fun updateBox(boxId: String, name: String, icon: String, percent: Int) = mutateBox(boxId) {
        it.copy(name = name.trim(), icon = icon, percent = percent)
    }

    fun deleteBox(boxId: String) = mutate { state ->
        state.copy(boxes = state.boxes.filterNot { it.id == boxId })
    }

    fun addItem(boxId: String, name: String, priceCents: Long) = mutateBox(boxId) { box ->
        val item = Item(UUID.randomUUID().toString(), name.trim(), priceCents, LocalDate.now().toEpochDay())
        box.copy(items = box.items + item)
    }

    fun updateItem(boxId: String, itemId: String, name: String, priceCents: Long) = mutateBox(boxId) { box ->
        box.copy(items = box.items.map {
            if (it.id == itemId) it.copy(name = name.trim(), priceCents = priceCents) else it
        })
    }

    fun deleteItem(boxId: String, itemId: String) = mutateBox(boxId) { box ->
        box.copy(items = box.items.filterNot { it.id == itemId })
    }

    fun resetNow() = mutate { it.resetNow(LocalDate.now()) }

    fun setAutoReset(enabled: Boolean) = mutate {
        it.withResetSchedule(enabled, it.payday, LocalDate.now())
    }

    fun setPayday(day: Int) = mutate {
        it.withResetSchedule(it.autoReset, day, LocalDate.now())
    }

    fun exportCsv(uri: Uri) {
        val app = getApplication<Application>()
        val csv = "﻿" + historyToCsv(_state.value.history) // BOM so Excel reads UTF-8.
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    val stream = app.contentResolver.openOutputStream(uri) ?: return@withContext false
                    stream.use { it.write(csv.toByteArray()) }
                    true
                } catch (_: IOException) {
                    false
                }
            }
            Toast.makeText(app, if (saved) "CSV saved" else "Couldn't save the CSV", Toast.LENGTH_SHORT).show()
        }
    }
}
