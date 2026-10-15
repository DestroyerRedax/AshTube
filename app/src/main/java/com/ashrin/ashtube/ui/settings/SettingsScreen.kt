package com.ashrin.ashtube.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ashrin.ashtube.data.AshTubeRepository
import com.ashrin.ashtube.data.MockCatalogRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { AshTubeRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var defaultQuality by rememberSaveable { mutableStateOf(MockCatalogRepository.getDefaultQuality()) }
    var adultAccessEnabled by rememberSaveable { mutableStateOf(MockCatalogRepository.isAdultAccessEnabled()) }
    var showNoticesDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (statusMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Text(
                        text = statusMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section: Playback
            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Default Quality", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Current: $defaultQuality",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Auto", "1080p", "720p", "480p").forEach { quality ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch { repository.setDefaultQuality(quality) }
                                    defaultQuality = quality
                                    statusMessage = "Default quality updated to $quality."
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(quality)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Content & Policy
            Text(
                text = "Content & Age Gate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Adults (18+) Section", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (adultAccessEnabled) "Currently Enabled (Active)" else "Disabled (Locked)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (adultAccessEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (adultAccessEnabled) {
                            TextButton(
                                onClick = {
                                    scope.launch { repository.setAdultAccess(false) }
                                    adultAccessEnabled = false
                                    statusMessage = "Adults mode has been locked and disabled."
                                },
                            ) {
                                Text("Lock & Disable")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Storage & Data
            Text(
                text = "Storage & Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Watch History", style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = {
                                scope.launch { repository.clearHistory() }
                                statusMessage = "Watch history cleared."
                            },
                        ) {
                            Text("Clear")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Favourites", style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = {
                                scope.launch { repository.clearFavorites() }
                                statusMessage = "Favourites cleared."
                            },
                        ) {
                            Text("Clear")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showPrivacyDialog = !showPrivacyDialog },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (showPrivacyDialog) "Hide Privacy Policy" else "View Privacy Policy")
                    }
                    if (showPrivacyDialog) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• Zero Accounts & Zero Cloud Sync: No profile or personal data is collected or transmitted.\n" +
                                "• On-Device Storage: All watch history, favourites, and settings are saved exclusively in local SQLite (Room) and DataStore.\n" +
                                "• Storage Access Framework (SAF): Only video files and folders you explicitly pick are indexed. AshTube never scans unrelated storage.\n" +
                                "• Network Security: HTTPS-only traffic is strictly enforced; cleartext HTTP is disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AshTube", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Version 0.2.0-dev (Phase 5 Storage, Hardening & Local Media)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Independent Android video client. AshTube does not connect to or use the " +
                            "private Moviebox APK backend, accounts, tokens, code, artwork, or branding.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showNoticesDialog = !showNoticesDialog },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (showNoticesDialog) "Hide Open Source Notices" else "View Open Source Notices")
                    }
                    if (showNoticesDialog) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Reference core: MovieBox-TUI (MIT OR Apache-2.0)\n" +
                                "Pinned commit: 431ab051cfdf34953b11b602b46618380d2a653d\n" +
                                "All rights to third-party provider content belong to their respective copyright holders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
