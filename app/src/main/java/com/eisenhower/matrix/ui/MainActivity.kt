package com.eisenhower.matrix.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eisenhower.matrix.R
import com.eisenhower.matrix.data.Prefs
import com.eisenhower.matrix.data.Quadrant
import com.eisenhower.matrix.data.ReminderConfig
import com.eisenhower.matrix.data.Task
import com.eisenhower.matrix.data.TaskStore
import com.eisenhower.matrix.data.WidgetSettings
import com.eisenhower.matrix.reminder.ReminderNotifier
import com.eisenhower.matrix.reminder.ReminderScheduler
import com.eisenhower.matrix.widget.refreshWidgets
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)
    private var tabRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TaskStore.seedIfFirstRun(this)
        tabRequest = intent.getIntExtra("tab", 0)
        setContent {
            AppTheme {
                MainScreen(tabRequest = tabRequest, resumeTick = resumeTick)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tabRequest = intent.getIntExtra("tab", 0)
    }

    override fun onResume() {
        super.onResume()
        resumeTick++ // ricarica i dati: potrebbero essere cambiati dal widget
    }
}

@Composable
private fun MainScreen(tabRequest: Int, resumeTick: Int) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    var tab by remember { mutableIntStateOf(tabRequest) }
    LaunchedEffect(tabRequest) { tab = tabRequest }
    var tasks by remember { mutableStateOf(TaskStore.all(ctx)) }
    var editing by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(resumeTick) { tasks = TaskStore.all(ctx) }

    fun reload() {
        tasks = TaskStore.all(ctx)
        refreshWidgets(ctx)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(painterResource(R.drawable.ic_grid), contentDescription = null) },
                    label = { Text("Attività") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                    label = { Text("Impostazioni") }
                )
            }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner)) {
            if (tab == 0) {
                TasksTab(
                    tasks = tasks,
                    dark = dark,
                    onToggle = { TaskStore.toggle(ctx, it.id); reload() },
                    onEdit = { editing = it },
                    onDelete = { TaskStore.delete(ctx, it.id); reload() },
                    onAdd = { title, q -> TaskStore.add(ctx, title, q); reload() }
                )
            } else {
                SettingsTab(
                    dark = dark,
                    resumeTick = resumeTick,
                    onTasksChanged = { reload() }
                )
            }
        }
    }

    editing?.let { t ->
        EditDialog(
            task = t,
            dark = dark,
            onDismiss = { editing = null },
            onSave = { title, q ->
                TaskStore.update(ctx, t.id, title, q)
                editing = null
                reload()
            },
            onDelete = {
                TaskStore.delete(ctx, t.id)
                editing = null
                reload()
            }
        )
    }
}

// ───────────────────────── Tab "Attività" ─────────────────────────

@Composable
private fun TasksTab(
    tasks: List<Task>,
    dark: Boolean,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onAdd: (String, Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    "Matrice di Eisenhower",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Tocca un'attività per modificarla o spostarla di quadrante.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(Quadrant.entries.toList(), key = { it.index }) { q ->
            QuadrantCard(
                quadrant = q,
                tasks = tasks
                    .filter { it.quadrant == q.index }
                    .sortedWith(compareBy<Task> { it.done }.thenBy { it.createdAt }),
                dark = dark,
                onToggle = onToggle,
                onEdit = onEdit,
                onDelete = onDelete,
                onAdd = { title -> onAdd(title, q.index) }
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun QuadrantCard(
    quadrant: Quadrant,
    tasks: List<Task>,
    dark: Boolean,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onAdd: (String) -> Unit,
) {
    val accent = quadrant.color(dark)
    val surface = MaterialTheme.colorScheme.surface
    var draft by remember { mutableStateOf("") }

    fun submit() {
        if (draft.isNotBlank()) {
            onAdd(draft)
            draft = ""
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.10f).compositeOver(surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quadrant.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        quadrant.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${tasks.count { !it.done }}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEdit(task) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            if (task.done) R.drawable.ic_check_circle else R.drawable.ic_circle
                        ),
                        contentDescription = "Spunta",
                        tint = if (task.done) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable { onToggle(task) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        task.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onDelete(task) }
                            .padding(3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Aggiungi attività…") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Aggiungi",
                        tint = accent,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { submit() }
                            .padding(8.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent
                )
            )
        }
    }
}

@Composable
private fun EditDialog(
    task: Task,
    dark: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(task.title) }
    var q by remember { mutableIntStateOf(task.quadrant) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica attività") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuadrantPicker(selected = q, dark = dark, onSelect = { q = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, q) }, enabled = title.isNotBlank()) {
                Text("Salva")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Elimina", color = Color(0xFFFF453A)) }
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        }
    )
}

// ───────────────────────── Tab "Impostazioni" ─────────────────────────

@Composable
private fun SettingsTab(dark: Boolean, resumeTick: Int, onTasksChanged: () -> Unit) {
    val ctx = LocalContext.current
    var ws by remember { mutableStateOf(Prefs.widget(ctx)) }
    var rc by remember { mutableStateOf(Prefs.reminder(ctx)) }
    var opacity by remember { mutableFloatStateOf(ws.opacityStep.toFloat()) }
    var textSize by remember { mutableFloatStateOf(ws.textPercent.toFloat()) }
    var notifOk by remember { mutableStateOf(ReminderNotifier.hasPermission(ctx)) }
    var showTimeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resumeTick) { notifOk = ReminderNotifier.hasPermission(ctx) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifOk = granted }

    fun saveWidget(n: WidgetSettings) {
        ws = n
        Prefs.saveWidget(ctx, n)
        refreshWidgets(ctx)
    }

    fun saveReminder(n: ReminderConfig) {
        rc = n
        Prefs.saveReminder(ctx, n)
        ReminderScheduler.schedule(ctx)
    }

    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !notifOk) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Impostazioni",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Widget
        SettingsCard("Widget") {
            Text("Trasparenza sfondo: ${opacity.roundToInt() * 10}% di opacità", fontSize = 14.sp)
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = { saveWidget(ws.copy(opacityStep = opacity.roundToInt())) },
                valueRange = 2f..10f,
                steps = 7
            )
            Text("Tema", fontSize = 14.sp)
            Segmented(
                options = listOf("Scuro", "Chiaro"),
                selected = if (ws.dark) 0 else 1,
                onSelect = { saveWidget(ws.copy(dark = it == 0)) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Dimensione del testo: ${textSize.roundToInt()}%", fontSize = 14.sp)
            Slider(
                value = textSize,
                onValueChange = { textSize = it },
                onValueChangeFinished = { saveWidget(ws.copy(textPercent = textSize.roundToInt())) },
                valueRange = 80f..140f,
                steps = 5
            )
            Text(
                "Per ridimensionare il widget tienilo premuto e trascina i bordi: quadranti e testo si adattano.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Promemoria
        SettingsCard("Avviso promemoria") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ricordami di svolgere i compiti", modifier = Modifier.weight(1f), fontSize = 15.sp)
                Switch(
                    checked = rc.enabled,
                    onCheckedChange = { on ->
                        saveReminder(rc.copy(enabled = on))
                        if (on) askNotificationPermission()
                    }
                )
            }
            if (rc.enabled && !notifOk) {
                Text(
                    "Le notifiche sono disattivate: abilitale dalle impostazioni di sistema per ricevere l'avviso.",
                    fontSize = 12.sp,
                    color = Color(0xFFFF453A)
                )
            }
            if (rc.enabled) {
                Segmented(
                    options = listOf("Ogni giorno", "Ogni N ore"),
                    selected = rc.mode,
                    onSelect = { saveReminder(rc.copy(mode = it)) }
                )
                if (rc.mode == ReminderConfig.MODE_DAILY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTimeDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Orario dell'avviso", modifier = Modifier.weight(1f), fontSize = 15.sp)
                        Text(
                            "%02d:%02d".format(rc.hour, rc.minute),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 6, 8, 12).forEach { h ->
                            val sel = rc.everyHours == h
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (sel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { saveReminder(rc.copy(everyHours = h)) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Ogni $h h",
                                    fontSize = 14.sp,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Text(
                        "Gli avvisi arrivano solo dalle ${ReminderConfig.WINDOW_START}:00 alle ${ReminderConfig.WINDOW_END}:00.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Avvisa solo se ci sono attività da fare",
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp
                    )
                    Switch(
                        checked = rc.onlyIfPending,
                        onCheckedChange = { saveReminder(rc.copy(onlyIfPending = it)) }
                    )
                }
                val next = remember(rc) {
                    SimpleDateFormat("EEEE d MMMM 'alle' HH:mm", Locale.ITALIAN)
                        .format(ReminderScheduler.nextTrigger(rc, System.currentTimeMillis()))
                }
                Text(
                    "Prossimo avviso: $next",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    if (notifOk) ReminderNotifier.show(ctx, force = true) else askNotificationPermission()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Invia avviso di prova") }
        }

        // ── Dati
        SettingsCard("Dati") {
            Button(
                onClick = {
                    TaskStore.clearDone(ctx)
                    onTasksChanged()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Rimuovi le attività completate") }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showTimeDialog) {
        TimeDialog(
            hour = rc.hour,
            minute = rc.minute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m ->
                showTimeDialog = false
                saveReminder(rc.copy(hour = h, minute = m))
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        text = { TimePicker(state = state) }
    )
}
