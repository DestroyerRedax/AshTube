package com.ashrin.ashtube.ui.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
fun HomeScreen(
    onSearch: () -> Unit,
    onOpenAdult: () -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    val sections = MockCatalogRepository.homeSections()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AshTube", style = MaterialTheme.typography.headlineMedium)
                Row {
                    TextButton(onClick = onSearch) {
                        Text("Search")
                    }
                    TextButton(onClick = onOpenAdult) {
                        Text("18+")
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    Text(
                        text = "Your feed",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(sections, key = HomeSection::title) { section ->
                    FeedRow(
                        section = section,
                        onItemClick = { item -> onOpenDetails(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedRow(
    section: HomeSection,
    onItemClick: (MediaItem) -> Unit,
) {
    Column {
        Text(
            text = section.title,
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items, key = MediaItem::id) { item ->
                MediaCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(156.dp)
            .height(128.dp)
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.type, style = MaterialTheme.typography.labelMedium)
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.year, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = MockCatalogRepository.search(query)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Search", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search titles") },
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (query.isBlank()) {
                Text("Type a title to search the local development catalogue.")
            } else if (results.isEmpty()) {
                Text("No local results found for \"$query\".")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results, key = MediaItem::id) { item ->
                        MediaCard(item = item, onClick = { onOpenDetails(item.id) })
                    }
                }
            }
        }
    }
}
