package com.veyro.debloatengine.ui.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.veyro.debloatengine.data.model.DownloadState
import com.veyro.debloatengine.data.model.UpdateInfo
import com.veyro.debloatengine.ui.components.PressScale
import com.veyro.debloatengine.ui.components.SweepProgressBar
import com.veyro.debloatengine.ui.theme.VeyroPurple
import com.veyro.debloatengine.ui.theme.VeyroPurpleBright
import com.veyro.debloatengine.ui.theme.VeyroWhite

/**
 * One-click in-app updater dialog.
 * Shows markdown-style release notes, then a live DownloadManager progress
 * bar, and finally the install actions (system installer or silent Shizuku
 * install).
 */
@Composable
fun UpdateDialog(
    update: UpdateInfo?,
    downloadState: DownloadState,
    downloadPercent: Int,
    shizukuReady: Boolean,
    onUpdateNow: () -> Unit,
    onSilentInstall: () -> Unit,
    onOpenInstaller: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (update == null) return

    val animatedPercent by animateFloatAsState(
        targetValue = downloadPercent / 100f,
        label = "downloadPercent"
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (downloadState != DownloadState.DOWNLOADING) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text("Veyro Engine v${update.versionName}")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Release Notes",
                    style = MaterialTheme.typography.labelLarge,
                    color = VeyroPurpleBright
                )
                Spacer(Modifier.height(6.dp))
                // Markdown bullet release notes rendered as styled lines.
                update.releaseNotes.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                when (downloadState) {
                    DownloadState.DOWNLOADING -> {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            com.veyro.debloatengine.ui.components.LottieIcon(
                                resId = com.veyro.debloatengine.ui.components.VeyroAnim.download,
                                color = VeyroPurpleBright,
                                size = 22.dp
                            )
                            Spacer(Modifier.height(0.dp))
                            Text(
                                "  Downloading update… $downloadPercent%",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        SweepProgressBar(
                            progress = animatedPercent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    DownloadState.DOWNLOADED -> {
                        Text(
                            "Download complete!",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(8.dp))
                        if (shizukuReady) {
                            Text(
                                "Shizuku detected — 1-tap silent install available.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    DownloadState.FAILED -> {
                        Text(
                            "Download failed. Check your connection and try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    DownloadState.IDLE -> Unit
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                DownloadState.IDLE, DownloadState.FAILED -> {
                    PressScale(onClick = onUpdateNow) {
                        Button(
                            onClick = onUpdateNow,
                            colors = ButtonDefaults.buttonColors(containerColor = VeyroPurple)
                        ) {
                            Text("Update Now", color = VeyroWhite)
                        }
                    }
                }
                DownloadState.DOWNLOADING -> Unit
                DownloadState.DOWNLOADED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (shizukuReady) {
                            PressScale(onClick = onSilentInstall) {
                                Button(
                                    onClick = onSilentInstall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VeyroPurple,
                                        contentColor = VeyroWhite
                                    )
                                ) {
                                    Text("Silent Install")
                                }
                            }
                        }
                        PressScale(onClick = onOpenInstaller) {
                            Button(
                                onClick = onOpenInstaller,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = VeyroPurpleBright
                                )
                            ) {
                                Text("Open Installer")
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (downloadState != DownloadState.DOWNLOADING && update.mandatory.not()) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        },
        modifier = modifier.background(Brush.verticalGradient(listOf(VeyroPurple.copy(alpha = 0.05f), androidx.compose.ui.graphics.Color.Transparent)))
    )
}
