package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.LeaderboardEntryEntity
import com.example.ui.PipeGameViewModel
import com.example.ui.ScreenStatus

@Composable
fun LeaderboardScreen(
    viewModel: PipeGameViewModel
) {
    val entries by viewModel.leaderboard.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenStatus.MAIN_MENU) },
                    modifier = Modifier.testTag("btn_back_leaderboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Indietro",
                        tint = Color(0xFF001A40)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Global",
                    tint = Color(0xFF0047AB),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CLASSIFICA ONLINE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF001A40)
                )
            }

            // Subtitle
            Text(
                text = "Competi con idraulici e scienziati da tutto il mondo nella sfida a tempo infiniti!",
                fontSize = 13.sp,
                color = Color(0xFF44474E),
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                fontWeight = FontWeight.Medium
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "RANGO & GIOCATORE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF44474E))
                Text(text = "PUNTEGGIO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF44474E))
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0047AB))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(entries) { index, entry ->
                        LeaderboardItemRow(index + 1, entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItemRow(
    rank: Int,
    entry: LeaderboardEntryEntity
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD54F) // Gold
        2 -> Color(0xFFB0BEC5) // Silver
        3 -> Color(0xFFFFAB91) // Bronze
        else -> Color.Transparent
    }

    val isTopThree = rank <= 3
    val itemBg = if (entry.isLocalPlayer) {
        Color(0xFFD8E2FF) // Pastel blue highlights for the local player matching high density D8E2FF
    } else {
        Color.White // Crisp white background for regular rows
    }

    val itemBorder = if (entry.isLocalPlayer) {
        BorderStroke(1.5.dp, Color(0xFF0047AB)) // Royal blue border
    } else {
        BorderStroke(1.dp, Color(0xFFE1E2E9)) // Light gray border
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_item_$rank"),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        shape = RoundedCornerShape(12.dp),
        border = itemBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank slot
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isTopThree) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trofeo",
                            tint = rankColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "#$rank",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF44474E)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Name
                Text(
                    text = entry.playerName,
                    fontSize = 15.sp,
                    fontWeight = if (entry.isLocalPlayer) FontWeight.Black else FontWeight.Bold,
                    color = if (entry.isLocalPlayer) Color(0xFF0047AB) else Color(0xFF1B1B1F)
                )
            }

            // Score Badge matching beautiful container stylings
            Box(
                modifier = Modifier
                    .background(
                        color = if (entry.isLocalPlayer) Color(0xFF0047AB).copy(alpha = 0.15f) else Color(0xFFF2F3F7),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${entry.score} pts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0047AB)
                )
            }
        }
    }
}
