package com.exemple.blockingapps.ui.history // <-- KIỂM TRA LẠI PACKAGE NÀY

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Import đúng model của mày
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.home.RecommendationItem

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recommendations = uiState.recommendations

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gợi ý thông minh", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Dựa trên dữ liệu sử dụng thực tế của con, chúng tôi đề xuất các biện pháp sau:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )

            if (recommendations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Mọi thứ đều ổn!", fontWeight = FontWeight.Bold)
                        Text("Chưa có hành vi bất thường nào cần xử lý.")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommendations) { rec ->
                        RecommendationItemCard(
                            recommendation = rec,
                            onApply = { viewModel.applyRecommendation(rec) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationItemCard(
    recommendation: RecommendationItem,
    onApply: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recommendation.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (recommendation.suggestedLimitMinutes != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onApply, shape = RoundedCornerShape(8.dp)) {
                        Text("Áp dụng chặn ${recommendation.suggestedLimitMinutes} ph", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}