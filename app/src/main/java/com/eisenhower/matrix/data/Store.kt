package com.eisenhower.matrix.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Archivio delle attività (SharedPreferences + JSON): semplice e senza dipendenze extra. */
object TaskStore {
    private const val PREFS = "eisenhower_tasks"
    private const val KEY_TASKS = "tasks"
    private const val KEY_SEEDED = "seeded"

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(c: Context): List<Task> {
        val raw = prefs(c).getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Task(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    quadrant = o.getInt("q"),
                    done = o.optBoolean("done", false),
                    createdAt = o.optLong("created", 0L),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun save(c: Context, list: List<Task>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("q", t.quadrant)
                    .put("done", t.done)
                    .put("created", t.createdAt)
            )
        }
        prefs(c).edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    @Synchronized
    fun add(c: Context, title: String, quadrant: Int): Boolean {
        val clean = title.trim()
        if (clean.isEmpty()) return false
        val now = System.currentTimeMillis()
        val list = all(c) + Task(now, clean, quadrant, false, now)
        save(c, list)
        return true
    }

    @Synchronized
    fun toggle(c: Context, id: Long) {
        save(c, all(c).map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    @Synchronized
    fun delete(c: Context, id: Long) {
        save(c, all(c).filter { it.id != id })
    }

    @Synchronized
    fun update(c: Context, id: Long, title: String, quadrant: Int) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        save(c, all(c).map { if (it.id == id) it.copy(title = clean, quadrant = quadrant) else it })
    }

    @Synchronized
    fun clearDone(c: Context) {
        save(c, all(c).filter { !it.done })
    }

    /** Al primo avvio inserisce qualche esempio, così il widget non è vuoto. */
    @Synchronized
    fun seedIfFirstRun(c: Context) {
        val p = prefs(c)
        if (p.getBoolean(KEY_SEEDED, false)) return
        p.edit().putBoolean(KEY_SEEDED, true).apply()
        if (all(c).isNotEmpty()) return
        val base = System.currentTimeMillis()
        val demo = listOf(
            Task(base + 1, "Consegnare la pratica in scadenza", Quadrant.DO.index, false, base + 1),
            Task(base + 2, "Preparare la riunione di domani", Quadrant.DO.index, false, base + 2),
            Task(base + 3, "Studiare 30 minuti", Quadrant.PLAN.index, false, base + 3),
            Task(base + 4, "Allenamento settimanale", Quadrant.PLAN.index, true, base + 4),
            Task(base + 5, "Rispondere alle email non critiche", Quadrant.DELEGATE.index, false, base + 5),
            Task(base + 6, "Riordinare le notifiche", Quadrant.DROP.index, false, base + 6),
        )
        save(c, demo)
    }
}

/** Impostazioni del widget e del promemoria. */
object Prefs {
    private const val PREFS = "eisenhower_settings"

    private fun p(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun widget(c: Context): WidgetSettings {
        val d = WidgetSettings()
        val sp = p(c)
        return WidgetSettings(
            opacityStep = sp.getInt("w_opacity", d.opacityStep).coerceIn(1, 10),
            dark = sp.getBoolean("w_dark", d.dark),
            textPercent = sp.getInt("w_text", d.textPercent).coerceIn(80, 140),
        )
    }

    fun saveWidget(c: Context, s: WidgetSettings) {
        p(c).edit()
            .putInt("w_opacity", s.opacityStep)
            .putBoolean("w_dark", s.dark)
            .putInt("w_text", s.textPercent)
            .apply()
    }

    fun reminder(c: Context): ReminderConfig {
        val d = ReminderConfig()
        val sp = p(c)
        return ReminderConfig(
            enabled = sp.getBoolean("r_enabled", d.enabled),
            mode = sp.getInt("r_mode", d.mode),
            hour = sp.getInt("r_hour", d.hour),
            minute = sp.getInt("r_minute", d.minute),
            everyHours = sp.getInt("r_every", d.everyHours),
            onlyIfPending = sp.getBoolean("r_only_pending", d.onlyIfPending),
        )
    }

    fun saveReminder(c: Context, r: ReminderConfig) {
        p(c).edit()
            .putBoolean("r_enabled", r.enabled)
            .putInt("r_mode", r.mode)
            .putInt("r_hour", r.hour)
            .putInt("r_minute", r.minute)
            .putInt("r_every", r.everyHours)
            .putBoolean("r_only_pending", r.onlyIfPending)
            .apply()
    }
}
