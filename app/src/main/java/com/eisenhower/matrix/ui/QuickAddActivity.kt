package com.eisenhower.matrix.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eisenhower.matrix.data.Prefs
import com.eisenhower.matrix.data.Quadrant
import com.eisenhower.matrix.data.TaskStore
import com.eisenhower.matrix.widget.refreshWidgets
import kotlinx.coroutines.delay

/**
 * Mini finestra flottante che si apre sopra la Home quando tocchi "+" nel widget:
 * non apre l'app, resta sullo sfondo della schermata Home.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initial = intent.getIntExtra("quadrant", 0)
        val dark = Prefs.widget(this).dark

        setContent {
            AppTheme(dark = dark) {
                QuickAddScreen(
                    initialQuadrant = initial,
                    dark = dark,
                    onAdd = { title, q ->
                        val ok = TaskStore.add(this, title, q)
                        if (ok) refreshWidgets(this)
                        ok
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
private fun QuickAddScreen(
    initialQuadrant: Int,
    dark: Boolean,
    onAdd: (String, Int) -> Boolean,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var quadrant by remember { mutableIntStateOf(initialQuadrant) }
    var addedCount by remember { mutableIntStateOf(0) }
    var lastAddedTo by remember { mutableStateOf<Quadrant?>(null) }
    val focus = remember { FocusRequester() }
    val q = Quadrant.from(quadrant)
    val accent = q.color(dark)

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(addedCount) {
        if (addedCount > 0) {
            delay(1600)
            lastAddedTo = null
        }
    }

    fun submit() {
        if (onAdd(text, quadrant)) {
            lastAddedTo = Quadrant.from(quadrant)
            addedCount++
            text = ""
        }
    }

    // Sfondo: tocca fuori dalla scheda per chiudere
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                // Evita che i tocchi sulla scheda chiudano la finestra
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Text(
                text = "Nuova attività",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                placeholder = { Text("Cosa devi fare?") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            QuadrantPicker(selected = quadrant, dark = dark, onSelect = { quadrant = it })

            Spacer(modifier = Modifier.height(8.dp))
            val added = lastAddedTo
            Text(
                text = if (added != null) "✓ Aggiunta a «${added.title}»" else " ",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) { Text("Fine") }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = { submit() },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Aggiungi", color = Color.White)
                }
            }
        }
    }
}
