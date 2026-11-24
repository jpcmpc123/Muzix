package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.DownloadForOfflineOutlined
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AutoEqStatus
import com.maxrave.simpmusic.viewModel.AutoEqViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.equalizer_autoeq_choose
import simpmusic.composeapp.generated.resources.equalizer_autoeq_custom_badge
import simpmusic.composeapp.generated.resources.equalizer_autoeq_downloading
import simpmusic.composeapp.generated.resources.equalizer_autoeq_empty
import simpmusic.composeapp.generated.resources.equalizer_autoeq_failed
import simpmusic.composeapp.generated.resources.equalizer_autoeq_import
import simpmusic.composeapp.generated.resources.equalizer_autoeq_import_hint
import simpmusic.composeapp.generated.resources.equalizer_autoeq_import_invalid
import simpmusic.composeapp.generated.resources.equalizer_autoeq_import_name
import simpmusic.composeapp.generated.resources.equalizer_autoeq_import_title
import simpmusic.composeapp.generated.resources.equalizer_autoeq_search
import simpmusic.composeapp.generated.resources.equalizer_autoeq_unavailable
import simpmusic.composeapp.generated.resources.equalizer_save_profile

private val MENU_LIST_MAX_HEIGHT = 280.dp

/**
 * Pick the headphone whose AutoEq correction runs on the independent AutoEq layer, or import
 * custom ParametricEQ / FixedBandEQ / Wavelet `.txt` profiles (e.g., from Squiglink).
 */
@Composable
fun AutoEqPicker(
    label: String?,
    modifier: Modifier = Modifier,
    viewModel: AutoEqViewModel = koinViewModel(),
) {
    var open by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val cachedPaths by viewModel.cachedPaths.collectAsStateWithLifecycle()
    val customProfiles by viewModel.customProfiles.collectAsStateWithLifecycle()

    LaunchedEffect(open) {
        if (open) viewModel.onOpen()
    }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { open = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label ?: stringResource(Res.string.equalizer_autoeq_choose),
                style = typo().bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (label != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = SimpIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(modifier = Modifier.width(320.dp).padding(horizontal = 12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        failed = false
                        viewModel.onQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = typo().bodyMedium,
                    label = { Text(text = stringResource(Res.string.equalizer_autoeq_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                open = false
                                showImportDialog = true
                            }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = SimpIcons.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.equalizer_autoeq_import),
                        style = typo().labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (status == AutoEqStatus.DOWNLOADING || status == AutoEqStatus.APPLYING) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                if (failed) {
                    Message(
                        text = stringResource(Res.string.equalizer_autoeq_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (customProfiles.isNotEmpty() && query.isBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    customProfiles.forEach { custom ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applyCustomProfile(custom)
                                        open = false
                                    }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = custom.label, style = typo().bodyMedium)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.equalizer_autoeq_custom_badge),
                                        style = typo().bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteCustomProfile(custom.path) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                when {
                    status == AutoEqStatus.DOWNLOADING ->
                        Message(stringResource(Res.string.equalizer_autoeq_downloading))

                    status == AutoEqStatus.UNAVAILABLE ->
                        Message(stringResource(Res.string.equalizer_autoeq_unavailable))

                    results.isEmpty() -> Message(stringResource(Res.string.equalizer_autoeq_empty))

                    else ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = MENU_LIST_MAX_HEIGHT)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            results.forEach { entry ->
                                val downloaded = entry.path in cachedPaths
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember(entry.path) { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                failed = false
                                                viewModel.apply(entry) { ok ->
                                                    if (ok) open = false else failed = true
                                                }
                                            }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = entry.name, style = typo().bodyMedium)
                                        Text(
                                            text = listOfNotNull(entry.source, entry.rig).joinToString(" · "),
                                            style = typo().bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        imageVector =
                                            if (downloaded) {
                                                SimpIcons.DownloadForOffline
                                            } else {
                                                SimpIcons.DownloadForOfflineOutlined
                                            },
                                        contentDescription = null,
                                        tint =
                                            if (downloaded) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            },
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    if (showImportDialog) {
        var profileName by remember { mutableStateOf("") }
        var rawText by remember { mutableStateOf("") }
        var parseError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = stringResource(Res.string.equalizer_autoeq_import_title), style = typo().titleSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        singleLine = true,
                        label = { Text(stringResource(Res.string.equalizer_autoeq_import_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it
                            parseError = false
                        },
                        minLines = 4,
                        maxLines = 8,
                        placeholder = { Text(stringResource(Res.string.equalizer_autoeq_import_hint), style = typo().bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (parseError) {
                        Text(
                            text = stringResource(Res.string.equalizer_autoeq_import_invalid),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = rawText.isNotBlank(),
                    onClick = {
                        val effectiveName = profileName.trim().ifEmpty { "Galaxy Buds / Custom" }
                        val ok = viewModel.importCustomText(effectiveName, rawText)
                        if (ok) {
                            showImportDialog = false
                        } else {
                            parseError = true
                        }
                    },
                ) {
                    Text(stringResource(Res.string.equalizer_save_profile))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun Message(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = typo().bodySmall,
        color = color,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}
