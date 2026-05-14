package kutira.kone.app.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kutira.kone.app.components.EmptyState
import kutira.kone.app.components.LineGray
import kutira.kone.app.components.PrimaryButton
import kutira.kone.app.components.RoundedInput
import kutira.kone.app.model.AppState

@Composable
fun IdeasScreen(
    state: AppState,
    context: Context,
    onLoadIdeas: () -> Unit,
    onAddIdea: (Context, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onLoadIdeas()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .padding(24.dp)
    ) {
        item {
            Text("Ideas", fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text(
                "Share simple ways to reuse textile scraps.",
                color = Color(0xFF3F3838),
                fontSize = 19.sp,
                lineHeight = 28.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LineGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Post an Idea", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    RoundedInput(title, { title = it }, "Title (optional)")
                    Spacer(Modifier.height(12.dp))
                    RoundedInput(description, { description = it }, "Description", singleLine = false)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Post Idea", enabled = description.isNotBlank() && !state.loading) {
                        onAddIdea(context, title, description)
                        title = ""
                        description = ""
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (state.ideas.isEmpty() && !state.loading) {
            item { EmptyState("No ideas yet") }
        } else {
            items(state.ideas) { idea ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LineGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (idea.title.isNotBlank()) {
                            Text(idea.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(idea.text, fontSize = 18.sp, lineHeight = 26.sp)
                        Text(
                            "by ${idea.userId.take(8)}",
                            color = Color(0xFF777777),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
