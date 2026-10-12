package com.eisenhower.matrix.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.*
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.eisenhower.matrix.R
import com.eisenhower.matrix.data.Quadrant
import com.eisenhower.matrix.data.Task
import com.eisenhower.matrix.data.TaskStore
import com.eisenhower.matrix.data.Prefs
import com.eisenhower.matrix.data.WidgetSettings
import com.eisenhower.matrix.ui.MainActivity
import com.eisenhower.matrix.ui.QuickAddActivity

/** Colori del widget, per tema scuro/chiaro. */
private class Palette(
    val text: Color,
    val secondary: Color,
    val tertiary: Color,
    val tileAlpha: Float,
)

private val DarkPalette = Palette(
    text = Color(0xFFFFFFFF),
    secondary = Color(0xB3FFFFFF),
    tertiary = Color(0x73FFFFFF),
    tileAlpha = 0.18f,
)

private val LightPalette = Palette(
    text = Color(0xFF1C1C1E),
    secondary = Color(0xB31C1C1E),
    tertiary = Color(0x731C1C1E),
    tileAlpha = 0.14f,
)

private fun glassRes(context: Context, dark: Boolean, step: Int): Int {
    val name = "glass_${if (dark) "dark" else "light"}_${step.coerceIn(1, 10) * 10}"
    val id = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (id != 0) id else R.drawable.glass_dark_60
}

class EisenhowerWidget : GlanceAppWidget() {

    // Il contenuto si adatta alle dimensioni reali scelte dall'utente (trascinando i bordi).
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        TaskStore.seedIfFirstRun(context)
        val tasks = TaskStore.all(context)
        val settings = Prefs.widget(context)
        val background = glassRes(context, settings.dark, settings.opacityStep)
        provideContent {
            WidgetContent(background, tasks, settings)
        }
    }
}

@Composable
private fun WidgetContent(backgroundRes: Int, tasks: List<Task>, settings: WidgetSettings) {
    val size = LocalSize.current
    val p = if (settings.dark) DarkPalette else LightPalette

    // Il testo cresce con il widget e con la preferenza dell'utente.
    val auto = minOf(size.width.value / 300f, size.height.value / 300f).coerceIn(0.85f, 1.3f)
    val scale = auto * (settings.textPercent / 100f)

    val pad = 8.dp
    val gap = 6.dp
    val showBar = size.height >= 190.dp
    val barH = if (showBar) (26f * scale).dp else 0.dp

    val tileW = (size.width - pad * 2 - gap) / 2
    val tileH = (size.height - pad * 2 - gap - barH) / 2

    val pending = tasks.count { !it.done }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(backgroundRes))
            .padding(pad)
    ) {
        if (showBar) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(barH),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Matrice",
                    modifier = GlanceModifier.padding(start = 4.dp),
                    style = TextStyle(
                        color = ColorProvider(p.text),
                        fontSize = (14f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = if (pending == 0) "Tutto fatto" else "$pending da fare",
                    style = TextStyle(
                        color = ColorProvider(p.secondary),
                        fontSize = (11f * scale).sp
                    )
                )
                Box(
                    modifier = GlanceModifier
                        .size(barH)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(TabKey to 1)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_settings),
                        contentDescription = "Impostazioni",
                        colorFilter = ColorFilter.tint(ColorProvider(p.secondary)),
                        modifier = GlanceModifier.size((16f * scale).dp)
                    )
                }
            }
        }

        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            QuadrantTile(Quadrant.DO, tasks, tileW, tileH, p, scale, settings.dark,
                GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(modifier = GlanceModifier.width(gap))
            QuadrantTile(Quadrant.PLAN, tasks, tileW, tileH, p, scale, settings.dark,
                GlanceModifier.defaultWeight().fillMaxHeight())
        }
        Spacer(modifier = GlanceModifier.height(gap))
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            QuadrantTile(Quadrant.DELEGATE, tasks, tileW, tileH, p, scale, settings.dark,
                GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(modifier = GlanceModifier.width(gap))
            QuadrantTile(Quadrant.DROP, tasks, tileW, tileH, p, scale, settings.dark,
                GlanceModifier.defaultWeight().fillMaxHeight())
        }
    }
}

@Composable
private fun QuadrantTile(
    quadrant: Quadrant,
    allTasks: List<Task>,
    tileW: Dp,
    tileH: Dp,
    p: Palette,
    scale: Float,
    dark: Boolean,
    modifier: GlanceModifier,
) {
    val accent = quadrant.color(dark)
    val mine = allTasks
        .filter { it.quadrant == quadrant.index }
        .sortedWith(compareBy<Task> { it.done }.thenBy { it.createdAt })
    val pendingCount = mine.count { !it.done }

    val headH = (24f * scale).dp
    val rowH = (22f * scale).dp
    val capacity = (((tileH - 12.dp - headH).value) / rowH.value).toInt().coerceAtLeast(0)
    // Max 8 righe per quadrante: i container Glance hanno un limite di elementi figli
    val visible = if (mine.size > capacity) {
        mine.take((capacity - 1).coerceIn(0, 8))
    } else {
        mine.take(8)
    }
    val overflow = mine.size - visible.size
    val showDelete = tileW >= 120.dp

    Column(
        modifier = modifier
            .background(accent.copy(alpha = p.tileAlpha))
            .cornerRadius(18.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Intestazione del quadrante
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(headH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .background(accent)
                    .cornerRadius(4.dp)
            ) {}
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = quadrant.title,
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(p.text),
                    fontSize = (12.5f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (pendingCount > 0) {
                Text(
                    text = pendingCount.toString(),
                    style = TextStyle(
                        color = ColorProvider(p.secondary),
                        fontSize = (11f * scale).sp
                    )
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(headH)
                    .clickable(
                        actionStartActivity<QuickAddActivity>(
                            actionParametersOf(QuadrantKey to quadrant.index)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "Aggiungi attività a ${quadrant.title}",
                    colorFilter = ColorFilter.tint(ColorProvider(accent)),
                    modifier = GlanceModifier.size((16f * scale).dp)
                )
            }
        }

        if (mine.isEmpty() && capacity > 0) {
            Text(
                text = "Nessuna attività",
                modifier = GlanceModifier.padding(start = 2.dp, top = 2.dp),
                style = TextStyle(
                    color = ColorProvider(p.tertiary),
                    fontSize = (11f * scale).sp
                )
            )
        }

        visible.forEach { task ->
            TaskRow(task, accent, p, scale, rowH, showDelete)
        }

        if (overflow > 0 && capacity > 0) {
            Text(
                text = "+$overflow altre",
                modifier = GlanceModifier
                    .height(rowH)
                    .padding(start = 4.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                style = TextStyle(
                    color = ColorProvider(p.secondary),
                    fontSize = (11f * scale).sp
                )
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    accent: Color,
    p: Palette,
    scale: Float,
    rowH: Dp,
    showDelete: Boolean,
) {
    val toggle = actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to task.id))
    val delete = actionRunCallback<DeleteTaskAction>(actionParametersOf(TaskIdKey to task.id))

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(rowH),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(rowH).clickable(toggle),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(
                    if (task.done) R.drawable.ic_check_circle else R.drawable.ic_circle
                ),
                contentDescription = if (task.done) "Segna da fare" else "Segna come fatta",
                colorFilter = ColorFilter.tint(
                    ColorProvider(if (task.done) accent else p.tertiary)
                ),
                modifier = GlanceModifier.size((16f * scale).dp)
            )
        }
        Text(
            text = task.title,
            modifier = GlanceModifier.defaultWeight().clickable(toggle),
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(if (task.done) p.tertiary else p.text),
                fontSize = (12f * scale).sp,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
        )
        if (showDelete) {
            Box(
                modifier = GlanceModifier.size(rowH).clickable(delete),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_close),
                    contentDescription = "Elimina",
                    colorFilter = ColorFilter.tint(ColorProvider(p.tertiary)),
                    modifier = GlanceModifier.size((12f * scale).dp)
                )
            }
        }
    }
}
