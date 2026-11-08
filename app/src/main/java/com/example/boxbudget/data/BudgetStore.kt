package com.example.boxbudget.data

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** Saves the whole budget as one JSON file, written atomically so a crash can't leave half a file. */
class BudgetStore(private val file: File) {

    fun load(): BudgetState {
        val text = try {
            AtomicFile(file).readFully().decodeToString()
        } catch (_: FileNotFoundException) {
            return BudgetState()
        }
        return try {
            decodeState(text)
        } catch (_: JSONException) {
            // Keep the unreadable file rather than letting the next save overwrite it.
            file.renameTo(File(file.parentFile, "${file.name}.corrupt"))
            BudgetState()
        }
    }

    fun save(state: BudgetState) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(encodeState(state).toByteArray())
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun <T> List<T>.toJsonArray(encode: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(encode(it)) } }

private fun encodeItem(item: Item) = JSONObject()
    .put("id", item.id)
    .put("name", item.name)
    .put("priceCents", item.priceCents)
    .put("epochDay", item.epochDay)

private fun decodeItem(json: JSONObject) = Item(
    id = json.getString("id"),
    name = json.getString("name"),
    priceCents = json.getLong("priceCents"),
    epochDay = json.getLong("epochDay"),
)

fun encodeState(state: BudgetState): String = JSONObject()
    .put("version", 1)
    .put("salaryCents", state.salaryCents)
    .put("autoReset", state.autoReset)
    .put("payday", state.payday)
    .put("nextResetEpochDay", state.nextResetEpochDay ?: JSONObject.NULL)
    .put("tutorialSeen", state.tutorialSeen)
    .put(
        "boxes",
        state.boxes.toJsonArray { box ->
            JSONObject()
                .put("id", box.id)
                .put("name", box.name)
                .put("icon", box.icon)
                .put("percent", box.percent)
                .put("items", box.items.toJsonArray(::encodeItem))
        },
    )
    .put(
        "history",
        state.history.toJsonArray { month ->
            JSONObject()
                .put("id", month.id)
                .put("label", month.label)
                .put("closedOnEpochDay", month.closedOnEpochDay)
                .put("salaryCents", month.salaryCents)
                .put(
                    "boxes",
                    month.boxes.toJsonArray { box ->
                        JSONObject()
                            .put("name", box.name)
                            .put("percent", box.percent)
                            .put("budgetCents", box.budgetCents)
                            .put("items", box.items.toJsonArray(::encodeItem))
                    },
                )
        },
    )
    .toString()

fun decodeState(text: String): BudgetState {
    val json = JSONObject(text)
    return BudgetState(
        salaryCents = json.getLong("salaryCents"),
        autoReset = json.getBoolean("autoReset"),
        payday = json.getInt("payday"),
        nextResetEpochDay = if (json.isNull("nextResetEpochDay")) null else json.getLong("nextResetEpochDay"),
        // Files saved before the tutorial existed belong to people already using the app.
        tutorialSeen = json.optBoolean("tutorialSeen", true),
        boxes = json.getJSONArray("boxes").mapObjects { box ->
            Box(
                id = box.getString("id"),
                name = box.getString("name"),
                icon = box.getString("icon"),
                percent = box.getInt("percent"),
                items = box.getJSONArray("items").mapObjects(::decodeItem),
            )
        },
        history = json.getJSONArray("history").mapObjects { month ->
            MonthRecord(
                id = month.getString("id"),
                label = month.getString("label"),
                closedOnEpochDay = month.getLong("closedOnEpochDay"),
                salaryCents = month.getLong("salaryCents"),
                boxes = month.getJSONArray("boxes").mapObjects { box ->
                    BoxRecord(
                        name = box.getString("name"),
                        percent = box.getInt("percent"),
                        budgetCents = box.getLong("budgetCents"),
                        items = box.getJSONArray("items").mapObjects(::decodeItem),
                    )
                },
            )
        },
    )
}
