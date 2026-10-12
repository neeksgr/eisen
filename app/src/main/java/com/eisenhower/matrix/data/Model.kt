package com.eisenhower.matrix.data

import androidx.compose.ui.graphics.Color

enum class Quadrant(
    val index: Int,
    val title: String,
    val subtitle: String,
    val dot: String,
    private val darkColor: Long,
    private val lightColor: Long,
) {
    DO(0, "Fai subito", "Urgente · Importante", "🔴", 0xFFFF453A, 0xFFFF3B30),
    PLAN(1, "Pianifica", "Non urgente · Importante", "🔵", 0xFF0A84FF, 0xFF007AFF),
    DELEGATE(2, "Delega", "Urgente · Non importante", "🟠", 0xFFFF9F0A, 0xFFFF9500),
    DROP(3, "Elimina", "Non urgente · Non importante", "⚪", 0xFF8E8E93, 0xFF8E8E93);

    fun color(dark: Boolean): Color = Color(if (dark) darkColor else lightColor)

    companion object {
        fun from(index: Int): Quadrant = entries.firstOrNull { it.index == index } ?: DO
    }
}

data class Task(
    val id: Long,
    val title: String,
    val quadrant: Int,
    val done: Boolean,
    val createdAt: Long,
)

data class WidgetSettings(
    /** 2..10 → 20%..100% di opacità dello sfondo */
    val opacityStep: Int = 6,
    val dark: Boolean = true,
    /** Percentuale testo: 80..140 */
    val textPercent: Int = 100,
)

data class ReminderConfig(
    val enabled: Boolean = false,
    val mode: Int = MODE_DAILY,
    val hour: Int = 9,
    val minute: Int = 0,
    val everyHours: Int = 3,
    val onlyIfPending: Boolean = true,
) {
    companion object {
        const val MODE_DAILY = 0
        const val MODE_INTERVAL = 1

        /** Con "ogni N ore" gli avvisi arrivano solo in questa fascia oraria. */
        const val WINDOW_START = 8
        const val WINDOW_END = 22
    }
}
