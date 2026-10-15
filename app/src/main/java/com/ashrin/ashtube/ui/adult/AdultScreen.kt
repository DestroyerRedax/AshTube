package com.ashrin.ashtube.ui.adult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.MockCatalogRepository

@Composable
fun AdultScreen(
    onBack: () -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    var isEnabled by rememberSaveable {
        mutableStateOf(MockCatalogRepository.isAdultAccessEnabled())
    }

    if (!isEnabled) {
        AdultGateScreen(
            onConfirmed = {
                MockCatalogRepository.setAdultAccessEnabled(true)
                isEnabled = true
            },
            onCancel = onBack,
        )
    } else {
        AdultFeedScreen(
            onLockAndExit = {
                MockCatalogRepository.setAdultAccessEnabled(false)
                isEnabled = false
                onBack()
            },
            onBack = onBack,
            onOpenDetails = onOpenDetails,
        )
    }
}

@Composable
private fun AdultGateScreen(
    onConfirmed: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Adults (18+) Notice",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "The Adults section contains mature and adult-oriented media. Access is " +
                    "disabled by default and strictly isolated from the general Home feed, " +
                    "recommendations, search, and Kids sections.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "By continuing, you explicitly confirm that you are at least 18 years of " +
                    "age (or the legal age of majority in your jurisdiction) and that you consent " +
                    "to browsing mature titles authorized for your location.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onConfirmed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("I am 18 or older — Enter")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel & Return")
            }
        }
    }
}

@Composable
private fun AdultFeedScreen(
    onLockAndExit: () -> Unit,
    onBack: () -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchResults = if (searchQuery.isNotBlank()) MockCatalogRepository.searchAdult(searchQuery) else emptyList()
    val sections = MockCatalogRepository.adultSections()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Adults (18+)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Isolated Section",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                    TextButton(
                        onClick = onLockAndExit,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Lock & Exit")
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search adult catalog") },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (searchQuery.isNotBlank()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "Results for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (searchResults.isEmpty()) {
                        item {
                            Text("No adult titles found.")
                        }
                    } else {
                        items(searchResults, key = MediaItem::id) { item ->
                            AdultMediaCard(item = item, onClick = { onOpenDetails(item.id) })
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(sections, key = HomeSection::title) { section ->
                        AdultFeedRow(
                            section = section,
                            onItemClick = { item -> onOpenDetails(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultFeedRow(
    section: HomeSection,
    onItemClick: (MediaItem) -> Unit,
) {
    Column {
        Text(
            text = section.title,
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items, key = MediaItem::id) { item ->
                AdultMediaCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun AdultMediaCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(132.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                item.type,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.year, style = MaterialTheme.typography.bodySmall)
        }
    }
}
