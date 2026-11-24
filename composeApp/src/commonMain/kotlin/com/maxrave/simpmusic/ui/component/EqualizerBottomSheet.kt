package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.home.EqualizerSection
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer

/**
 * Full-screen Player BottomSheet hosting [EqualizerSection] so listeners can tune curves,
 * Bass/Air/Width, Saved Profiles, and A/B comparisons live without leaving playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = rememberSurfaceDarkColors().container),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.equalizer),
                        style = typo().titleMedium,
                        color = rememberSurfaceDarkColors().content,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = SimpIcons.Close,
                            contentDescription = null,
                            tint = rememberSurfaceDarkColors().content,
                        )
                    }
                }
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                ) {
                    EqualizerSection(embeddedInSheet = true)
                    Spacer(modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}
