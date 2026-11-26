package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.new_playlist
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_saved_both_hint
import simpmusic.composeapp.generated.resources.playlist_saved_local_hint
import simpmusic.composeapp.generated.resources.playlist_visibility
import simpmusic.composeapp.generated.resources.privacy_private
import simpmusic.composeapp.generated.resources.privacy_public
import simpmusic.composeapp.generated.resources.privacy_unlisted

internal enum class PlaylistVisibilityOption(
    val apiValue: String,
    val labelRes: StringResource,
) {
    PUBLIC("PUBLIC", Res.string.privacy_public),
    UNLISTED("UNLISTED", Res.string.privacy_unlisted),
    PRIVATE("PRIVATE", Res.string.privacy_private),
}

/**
 * Direct dialog for creating a playlist without any intermediate playlist-type picker.
 * Styled to match the Muzix theme while collecting Title and Visibility (when logged in to
 * YouTube Music) and saving the playlist in both Muzix and YouTube Music.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDualPlaylistDialog(
    isYouTubeLoggedIn: Boolean,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, privacyStatus: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(PlaylistVisibilityOption.PUBLIC) }
    var visibilityExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = {
            if (!isCreating) onDismiss()
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.new_playlist),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    enabled = !isCreating,
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(Res.string.playlist_name),
                            style = typo().bodyMedium,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isYouTubeLoggedIn) {
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = stringResource(visibility.labelRes),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            singleLine = true,
                            label = {
                                Text(
                                    text = stringResource(Res.string.playlist_visibility),
                                    style = typo().bodyMedium,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = SimpIcons.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = !isCreating) { visibilityExpanded = true },
                        )
                        DropdownMenu(
                            expanded = visibilityExpanded,
                            onDismissRequest = { visibilityExpanded = false },
                        ) {
                            PlaylistVisibilityOption.entries.forEach { option ->
                                val selected = option == visibility
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(option.labelRes),
                                            style = typo().bodyMedium,
                                            color =
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                        )
                                    },
                                    trailingIcon =
                                        if (selected) {
                                            {
                                                Icon(
                                                    imageVector = SimpIcons.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    onClick = {
                                        visibility = option
                                        visibilityExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text =
                        stringResource(
                            if (isYouTubeLoggedIn) {
                                Res.string.playlist_saved_both_hint
                            } else {
                                Res.string.playlist_saved_local_hint
                            },
                        ),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isCreating,
                    ) {
                        Text(
                            text = stringResource(Res.string.cancel),
                            style = typo().labelLarge,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val trimmed = title.trim()
                            if (trimmed.isNotEmpty() && !isCreating) {
                                onCreate(trimmed, visibility.apiValue)
                            }
                        },
                        enabled = title.isNotBlank() && !isCreating,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(Res.string.create),
                            style = typo().labelLarge,
                        )
                    }
                }
            }
        }
    }
}
