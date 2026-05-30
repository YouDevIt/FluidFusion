package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.*
import com.example.ui.PipeGameViewModel
import com.example.ui.ScreenStatus
import com.example.ui.components.PipeCellRenderer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GamePlayScreen(
    viewModel: PipeGameViewModel,
    onBack: () -> Unit
) {
    val level = viewModel.activeLevel
    val cells = viewModel.gridCells
    val moves = viewModel.movesCount
    val elapsed = viewModel.elapsedTimeSec
    val isSolved = viewModel.isSolved
    val stars = viewModel.earnedStars
    val isChallenge = viewModel.isTimeChallengeActive

    // Temp state for scoreboard name input
    var playerNameInput by remember { mutableStateOf("") }

    // Delayed dialog state so the user can admire their connected flows first
    var showDialogWithDelay by remember { mutableStateOf(false) }

    LaunchedEffect(isSolved) {
        if (isSolved) {
            kotlinx.coroutines.delay(1800)
            showDialogWithDelay = true
        } else {
            showDialogWithDelay = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Combined "High Density" Header & Stats Card (bg-[#D8E2FF], rounded bottom for that floating Material 3 feel)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD8E2FF)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Top Row: navigation and level metadata
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.SpaceBetween
                     ) {
                         IconButton(
                             onClick = onBack,
                             modifier = Modifier.testTag("btn_gameplay_back")
                         ) {
                             Icon(
                                 imageVector = Icons.Default.ArrowBack,
                                 contentDescription = "Indietro",
                                 tint = Color(0xFF001A40)
                             )
                         }

                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.weight(1f)
                         ) {
                             Text(
                                 text = if (isChallenge) "SFIDA A TEMPO" else level?.name ?: "Partita Salvata",
                                 fontSize = 18.sp,
                                 fontWeight = FontWeight.ExtraBold,
                                 color = Color(0xFF001A40),
                                 textAlign = TextAlign.Center
                             )
                             Text(
                                 text = if (isChallenge) "Puzzle #${viewModel.challengeSolvedPuzzlesCount + 1}" else "Completa la connessione",
                                 fontSize = 11.sp,
                                 color = Color(0xFF44474E),
                                 fontWeight = FontWeight.Bold,
                                 textAlign = TextAlign.Center
                             )
                         }

                         IconButton(
                             onClick = {
                                 if (isChallenge) {
                                     viewModel.startTimeChallengeMode()
                                 } else {
                                     level?.id?.let {
                                         if (it < 0) viewModel.startProceduralLevel(-it)
                                         else viewModel.startTutorialLevel(it)
                                     }
                                 }
                             },
                             modifier = Modifier.testTag("btn_gameplay_reset")
                         ) {
                             Icon(
                                 imageVector = Icons.Default.Refresh,
                                 contentDescription = "Riavvia",
                                 tint = Color(0xFF001A40)
                             )
                         }
                     }

                     Spacer(modifier = Modifier.height(10.dp))

                     // Inner metrics blocks (p-3 rounded-2xl bg-white/50 from high density layout patterns)
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.spacedBy(8.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         // Moves block
                         Card(
                             modifier = Modifier.weight(1f),
                             colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                             shape = RoundedCornerShape(12.dp),
                             border = BorderStroke(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f))
                         ) {
                             Column(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 8.dp),
                                 horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                 Text(text = "MOSSE", fontSize = 10.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                 Text(text = "$moves", fontSize = 16.sp, color = Color(0xFF001A40), fontWeight = FontWeight.Black)
                             }
                         }

                         // Timer block
                         Card(
                             modifier = Modifier.weight(1f),
                             colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                             shape = RoundedCornerShape(12.dp),
                             border = BorderStroke(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f))
                         ) {
                             Column(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 8.dp),
                                 horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                 if (isChallenge) {
                                     val remTime = viewModel.challengeRemainingTimeSec
                                     val colorAlert = if (remTime < 15) Color(0xFFFF1744) else Color(0xFF0047AB)
                                     Text(text = "TEMPO RIMASTO", fontSize = 10.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                     Text(
                                         text = "${remTime} s",
                                         fontSize = 16.sp,
                                         color = colorAlert,
                                         fontWeight = FontWeight.Black,
                                         modifier = Modifier.animateContentSize()
                                     )
                                 } else {
                                     Text(text = "TEMPO", fontSize = 10.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                     val sec = elapsed % 60
                                     val min = elapsed / 60
                                     Text(
                                         text = String.format("%02d:%02d", min, sec),
                                         fontSize = 16.sp,
                                         color = Color(0xFF001A40),
                                         fontWeight = FontWeight.Black
                                     )
                                 }
                             }
                         }

                         // Goal / Score block
                         Card(
                             modifier = Modifier.weight(1f),
                             colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                             shape = RoundedCornerShape(12.dp),
                             border = BorderStroke(1.dp, Color(0xFFADC6FF).copy(alpha = 0.5f))
                         ) {
                             Column(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 8.dp),
                                 horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                 if (isChallenge) {
                                     Text(text = "PUNTI SFIDA", fontSize = 10.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                     Text(text = "${viewModel.challengeScoreTotal}", fontSize = 16.sp, color = Color(0xFF0047AB), fontWeight = FontWeight.Black)
                                 } else {
                                     Text(text = "🎯 TRAGUARDO", fontSize = 10.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                     level?.let {
                                         Text(text = "≤ ${it.maxMovesForThreeStars} mosse", fontSize = 12.sp, color = Color(0xFF0047AB), fontWeight = FontWeight.ExtraBold)
                                     } ?: Text(text = "-", fontSize = 12.sp, color = Color(0xFF0047AB), fontWeight = FontWeight.ExtraBold)
                                 }
                             }
                         }
                     }
                 }
             }

            // Game Grid container (Highly striking immersive dark board with ring-[#44474E]/10 glow)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1A1C1E), shape = RoundedCornerShape(24.dp))
                    .border(8.dp, Color(0xFF44474E).copy(alpha = 0.1f), shape = RoundedCornerShape(24.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val gridW = level?.width ?: 5
                val gridH = level?.height ?: 5

                val cellSize = minOf(maxWidth / gridW, maxHeight / gridH)

                Column(
                    modifier = Modifier.wrapContentSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (y in 0 until gridH) {
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (x in 0 until gridW) {
                                val cellIndex = y * gridW + x
                                if (cellIndex in cells.indices) {
                                    val cell = cells[cellIndex]
                                    val isDockSelected = viewModel.selectedDockPipeIndex != null && cell.pipe == null

                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (cell.pipe != null) {
                                                        viewModel.rotatePipeAt(x, y)
                                                    } else {
                                                        viewModel.placePipeAt(x, y)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (cell.pipe != null && cell.pipe.isMovable) {
                                                        viewModel.removePipeAt(x, y)
                                                    }
                                                }
                                            )
                                            .testTag("cell_${x}_${y}")
                                    ) {
                                        PipeCellRenderer(
                                            cell = cell,
                                            isSelected = isDockSelected
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PIPES WORKBENCH (Dock) Section (High Density bottom footer concept)
            Text(
                text = "BANCO DI LAVORO (Tocca per posare/ruotare, tieni premuto per togliere)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF44474E),
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE1E2E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (viewModel.dockPipes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun tubo rimovibile in magazzino.\nTutti i raccordi sono saldati sulla griglia.",
                            fontSize = 12.sp,
                            color = Color(0xFF44474E),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(viewModel.dockPipes) { index, pipe ->
                            val isSelected = viewModel.selectedDockPipeIndex == index
                            val pipeCell = GridCell(
                                x = 0, y = 0,
                                cellType = GridCellType.Normal,
                                pipe = pipe
                            )

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFD8E2FF) else Color(0xFFF2F3F7))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) Color(0xFF0047AB) else Color(0xFFADC6FF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectDockPipe(index) }
                                    .testTag("dock_pipe_$index")
                            ) {
                                PipeCellRenderer(
                                    cell = pipeCell,
                                    isSelected = isSelected
                                )
                            }
                        }
                    }
                }
            }

            // High density aesthetic bottom glide handle bar (w-12, h-1.5, bg-[#E1E2E9])
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .background(Color(0xFFE1E2E9), shape = RoundedCornerShape(100.dp))
                    .align(Alignment.CenterHorizontally)
            )
        }

        // --- DIALOGS AND OVERLAYS ---

        // Level Solved Dialog (Standard Level)
        AnimatedVisibility(
            visible = showDialogWithDelay && !isChallenge,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(24.dp)
                        .testTag("dialog_level_solved"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151E27)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, Color(0xFF00E676))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Successo",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "CONNESSO!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = "Liquidi colorati miscelati correttamente senza perdite.",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Stars feedback
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            repeat(3) { idx ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star",
                                    tint = if (idx < stars) Color(0xFFFFD54F) else Color(0xFF263238),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Efficiency Data Table
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E141B), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Mosse Totali:", fontSize = 13.sp, color = Color(0xFF78909C))
                                Text(text = "$moves", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Tempo Impiegato:", fontSize = 13.sp, color = Color(0xFF78909C))
                                Text(text = "$elapsed s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Efficienza Punteggio:", fontSize = 13.sp, color = Color(0xFF78909C))
                                Text(text = "${viewModel.activeScore} pt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val currentId = level?.id ?: 1
                                if (currentId < 5 && currentId > 0) {
                                    // Load next tutorial level
                                    viewModel.startTutorialLevel(currentId + 1)
                                } else {
                                    // Otherwise generate a new procedural index
                                    val nextProcIndex = (100..1000).random()
                                    viewModel.startProceduralLevel(nextProcIndex)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0C141C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("btn_next_level"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (level != null && level.id in 1..4) "PROSSIMO LIVELLO" else "GIOCA PROCEDURALE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_solved_dialog_menu")
                        ) {
                            Text(text = "Torna al Menu", color = Color(0xFF90A4AE))
                        }
                    }
                }
            }
        }

        // Time Challenge Game Over Dialog (Leaderboard Upload)
        AnimatedVisibility(
            visible = isChallenge && viewModel.challengeRemainingTimeSec == 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(340.dp)
                        .padding(20.dp)
                        .testTag("dialog_challenge_gameover"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1424)), // Dark purple container for time-up
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, Color(0xFFE040FB))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Tempo Scaduto",
                            tint = Color(0xFFFF1744),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "TEMPO SCADUTO!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = "Hai risolto i puzzle con rapidità ed efficienza!",
                            fontSize = 12.sp,
                            color = Color(0xFFB0BEC5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Score metrics
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF100B14)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PUNTEGGIO FINALE",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${viewModel.challengeScoreTotal}",
                                    fontSize = 32.sp,
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Puzzle risolti: ${viewModel.challengeSolvedPuzzlesCount}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Score Submit Input Section
                        OutlinedTextField(
                            value = playerNameInput,
                            onValueChange = { if (it.length <= 12) playerNameInput = it },
                            label = { Text("Nome Giocatore (max 12 car)") },
                            placeholder = { Text("es: PlumberMax") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                focusedLabelColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("challenge_name_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.submitLeaderboardScore(playerNameInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("btn_submit_score"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "SALVA IN CLASSICA", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_gameover_back_menu")
                        ) {
                            Text(text = "Ritorna al Menu", color = Color(0xFFB0BEC5))
                        }
                    }
                }
            }
        }
    }
}
