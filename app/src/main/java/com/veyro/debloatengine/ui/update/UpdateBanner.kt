package com.veyro.debloatengine.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.veyro.debloatengine.data.model.UpdateInfo
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.theme.VeyroPurple
import com.veyro.debloatengine.ui.theme.VeyroPurpleBright

/**
 * Top Material 3 alert banner shown when a new GitHub release is detected.
 * Animates in with fade + vertical expansion.
 */
@Composable
fun UpdateBanner(
    visible: Boolean,
    update: UpdateInfo?,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && update != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(VeyroPurple.copy(alpha = 0.35f), VeyroPurpleBright.copy(alpha = 0.15f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Veyro Engine Update Available (v${update?.versionName ?: ""})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (update?.mandatory == true)
                        "Mandatory update — install to continue."
                    else
                        "Tap to see release notes and update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PressScale(onClick = onUpdateClick) {
                TextButton(onClick = onUpdateClick) {
                    Text("Update", color = VeyroPurpleBright)
                }
            }
            PressScale(onClick = onDismiss) {
                TextButton(onClick = onDismiss) {
                    Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
