package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.TutorialLevels
import com.example.ui.PipeGameViewModel
import com.example.ui.ScreenStatus

@Composable
fun MainMenuScreen(
    viewModel: PipeGameViewModel,
    onShowHowToPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Hero Section (Card matching bg-[#D8E2FF] rounded-b-[32px])
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD8E2FF)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Logo",
                            tint = Color(0xFF0047AB),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FLUID FUSION",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF001A40),
                            letterSpacing = 1.2.sp
                        )
                    }

                    Text(
                        text = "Tubi, Valvole e Sintesi di Colori",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44474E),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Menu Buttons with customized High Density colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Resume session (Autosave detection)
                if (viewModel.hasSessionToResume) {
                    Button(
                        onClick = { viewModel.resumeAutosavedSession() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE040FB),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_resume_session"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Continua")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CONTINUA PARTITA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Normal play tutorials (Using themed Primary blue bg-[#0047AB])
                Button(
                    onClick = { viewModel.navigateTo(ScreenStatus.LEVEL_SELECT) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0047AB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_play_tutorials"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Tutorial")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MODALITÀ TUTORIAL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Speedrun Challenge
                Button(
                    onClick = { viewModel.startTimeChallengeMode() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color(0xFF0C141C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_time_challenge"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Sfida")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SFIDA A TEMPO INFINITA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                // Leaderboard (High contrast Outline matching themed border-[#ADC6FF])
                OutlinedButton(
                    onClick = { viewModel.navigateTo(ScreenStatus.LEADERBOARD) },
                    border = BorderStroke(1.5.dp, Color(0xFFADC6FF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0047AB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_leaderboard"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Classifiche", tint = Color(0xFF0047AB))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CLASSIFICHE GLOBALI",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0047AB),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Technical details footer with High Density text colors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShowHowToPlay,
                    modifier = Modifier.testTag("btn_show_help")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Guida di Gioco",
                        tint = Color(0xFF44474E),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "v1.2.0 • Prova procedurale",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LevelSelectScreen(
    viewModel: PipeGameViewModel
) {
    val scores by viewModel.allScores.collectAsState()

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenStatus.MAIN_MENU) },
                    modifier = Modifier.testTag("btn_back_levels")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Indietro",
                        tint = Color(0xFF001A40)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Seleziona Livello",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF001A40)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(TutorialLevels.LIST) { level ->
                    val savedScore = scores.firstOrNull { it.levelId == level.id }
                    val stars = savedScore?.stars ?: 0
                    val completed = savedScore != null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startTutorialLevel(level.id) }
                            .testTag("level_card_${level.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (completed) Color(0xFFD8E2FF) else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (completed) Color(0xFFADC6FF) else Color(0xFFE1E2E9)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Livello ${level.id}: ${level.name}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completed) Color(0xFF0047AB) else Color(0xFF1B1B1F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = level.description,
                                    fontSize = 13.sp,
                                    color = Color(0xFF44474E),
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Stars Render with High Density custom empty slots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Stella",
                                        tint = if (index < stars) Color(0xFFFFD54F) else Color(0xFFDEE3EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
