package io.github.nku100.webui.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import io.github.nku100.webui.generated.resources.Res
import io.github.nku100.webui.generated.resources.cancel
import io.github.nku100.webui.generated.resources.save

@Composable
fun EditableTextPreference(
    title: String,
    summary: String,
    value: String,
    dialogSummary: String,
    singleLine: Boolean,
    onSave: (String) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(value) { mutableStateOf(value) }

    fun dismissInput() {
        hideEditableInput()
        showDialog = false
    }

    ArrowPreference(
        title = title,
        summary = summary,
        onClick = {
            hideEditableInput()
            draft = value
            showDialog = true
        },
    )

    OverlayDialog(
        show = showDialog,
        title = title,
        summary = dialogSummary,
        onDismissRequest = { dismissInput() },
        content = {
            EditableInputField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = singleLine,
                enabled = showDialog,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(Res.string.cancel),
                    onClick = { dismissInput() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(Res.string.save),
                    onClick = {
                        val result = readEditableInputValue(draft)
                        hideEditableInput()
                        onSave(result)
                        showDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
internal expect fun EditableInputField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    enabled: Boolean,
    modifier: Modifier,
)

internal expect fun readEditableInputValue(fallback: String): String

internal expect fun hideEditableInput()
