package com.ashrin.ashtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashrin.ashtube.ui.details.DetailScreen
import com.ashrin.ashtube.ui.home.HomeScreen
import com.ashrin.ashtube.ui.home.SearchScreen
import com.ashrin.ashtube.ui.theme.AshTubeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AshTubeTheme {
                AshTubeApp()
            }
        }
    }
}

@Composable
private fun AshTubeApp() {
    var noticeAccepted by rememberSaveable { mutableStateOf(false) }
    var showingSearch by rememberSaveable { mutableStateOf(false) }
    var selectedMediaId by rememberSaveable { mutableStateOf<String?>(null) }

    when {
        !noticeAccepted -> LegalNoticeScreen(onContinue = { noticeAccepted = true })
        selectedMediaId != null -> DetailScreen(
            mediaId = selectedMediaId!!,
            onBack = { selectedMediaId = null },
        )
        showingSearch -> SearchScreen(
            onBack = { showingSearch = false },
            onOpenDetails = { id -> selectedMediaId = id },
        )
        else -> HomeScreen(
            onSearch = { showingSearch = true },
            onOpenDetails = { id -> selectedMediaId = id },
        )
    }
}

@Composable
private fun LegalNoticeScreen(onContinue: () -> Unit) {
    var showOpenSourceNotices by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "Welcome to AshTube", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AshTube is an independent Android project. It does not use the private " +
                    "Moviebox APK backend, accounts, tokens, code, artwork, branding, or user interface.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Only content sources and streams that are authorized for AshTube may be " +
                    "enabled. Availability and legality can vary by source and location.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "The Adults (18+) section is disabled by default and will remain separate " +
                    "from general and Kids content. It is unavailable unless its content and " +
                    "distribution requirements are satisfied.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showOpenSourceNotices = !showOpenSourceNotices },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showOpenSourceNotices) "Hide open-source notices" else "View open-source notices")
            }
            if (showOpenSourceNotices) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Planned reusable core reference: MovieBox-TUI, licensed under MIT OR " +
                        "Apache-2.0. Attribution and license texts will be included before any " +
                        "upstream code is distributed with AshTube.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("I understand")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LegalNoticePreview() {
    AshTubeTheme {
        LegalNoticeScreen(onContinue = {})
    }
}
