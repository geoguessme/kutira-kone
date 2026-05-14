package kutira.kone.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kutira.kone.app.components.EmptyState
import kutira.kone.app.components.FilterChip
import kutira.kone.app.components.ScrapCard
import kutira.kone.app.components.materialOptions
import kutira.kone.app.model.AppState
import kutira.kone.app.model.Scrap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppState,
    searchQuery: String,
    onRefresh: () -> Unit,
    onPullRefresh: () -> Unit,
    onOpenDetails: (Scrap) -> Unit
) {
    var filter by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    val filtered = state.scraps.filter { scrap ->
        val queryOk = searchQuery.isBlank() ||
            scrap.materialType.contains(searchQuery, ignoreCase = true) ||
            scrap.size.contains(searchQuery, ignoreCase = true) ||
            scrap.contactInfo.contains(searchQuery, ignoreCase = true)
        val materialOk = filter.isBlank() || scrap.materialType.equals(filter, ignoreCase = true)
        queryOk && materialOk
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onPullRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                materialOptions.forEach { option ->
                    FilterChip(
                        text = option,
                        selected = filter == option,
                        onClick = { filter = if (filter == option) "" else option }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            if (filtered.isEmpty() && !state.loading) {
                EmptyState("No scraps found")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { scrap ->
                        ScrapCard(scrap) { onOpenDetails(scrap) }
                    }
                }
            }
        }
    }
}
