package com.eisenhower.matrix.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.eisenhower.matrix.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val TaskIdKey = ActionParameters.Key<Long>("task_id")
val QuadrantKey = ActionParameters.Key<Int>("quadrant")
val TabKey = ActionParameters.Key<Int>("tab")

class EisenhowerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = EisenhowerWidget()
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        TaskStore.toggle(context, id)
        EisenhowerWidget().updateAll(context)
    }
}

class DeleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        TaskStore.delete(context, id)
        EisenhowerWidget().updateAll(context)
    }
}

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Da chiamare da Activity/Receiver dopo aver modificato dati o impostazioni. */
fun refreshWidgets(context: Context) {
    val app = context.applicationContext
    widgetScope.launch { EisenhowerWidget().updateAll(app) }
}
