package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                // Initialize the game state controller
                val gameViewModel: PipeGameViewModel = viewModel()
                
                // Overlay visibility of standard Italian manual guide
                var showHelpDialog by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF7F9FF)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Switch between app panels based on state machine
                        Crossfade(
                            targetState = gameViewModel.currentScreen,
                            animationSpec = tween(400),
                            label = "screenTransition"
                        ) { screen ->
                            when (screen) {
                                ScreenStatus.MAIN_MENU -> {
                                    MainMenuScreen(
                                        viewModel = gameViewModel,
                                        onShowHowToPlay = { showHelpDialog = true }
                                    )
                                }
                                ScreenStatus.LEVEL_SELECT -> {
                                    LevelSelectScreen(viewModel = gameViewModel)
                                }
                                ScreenStatus.GAMEPLAY -> {
                                    GamePlayScreen(
                                        viewModel = gameViewModel,
                                        onBack = { gameViewModel.navigateTo(ScreenStatus.MAIN_MENU) }
                                    )
                                }
                                ScreenStatus.LEADERBOARD -> {
                                    LeaderboardScreen(viewModel = gameViewModel)
                                }
                                else -> {
                                    MainMenuScreen(
                                        viewModel = gameViewModel,
                                        onShowHowToPlay = { showHelpDialog = true }
                                    )
                                }
                            }
                        }

                        // Floating / Modal instruction handbook "Come Giocare"
                        if (showHelpDialog) {
                            HowToPlayModal(onDismiss = { showHelpDialog = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HowToPlayModal(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .clickable(enabled = false) { /* Prevent click through */ }
                .testTag("modal_how_to_play"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151C24)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Guida",
                            tint = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guida Idraulica",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_help")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = Color.White
                        )
                    }
                }

                Divider(color = Color(0xFF2C3E50), modifier = Modifier.padding(vertical = 12.dp))

                // Chapter 1: Rotation
                ChapterGuideBox(
                    num = "1",
                    title = "Rotazione del Flusso",
                    desc = "Tocca i tubi saldati sulla griglia per ruotarli di 90 gradi. Allinea le entrate e le uscite per consentire il transito del liquido!"
                )

                // Chapter 2: The Workbench / Spare Dock
                ChapterGuideBox(
                    num = "2",
                    title = "Il Banco di Lavoro (Dock)",
                    desc = "In alcuni livelli, hai raccordi staccabili nel Dock in basso. Tocca un raccordo per selezionarlo, poi tocca una casella libera sulla griglia per piazzarlo. Toccalo di nuovo per rimetterlo nel Dock."
                )

                // Chapter 3: Color Mixing
                ChapterGuideBox(
                    num = "3",
                    title = "Sintesi Additiva dei Colori",
                    desc = "I canali non devono mescolarsi, tranne per contenitori speciali:\n" +
                            "• Rosso + Verde = Giallo\n" +
                            "• Rosso + Blu = Magenta\n" +
                            "• Verde + Blu = Ciano\n" +
                            "Unisci i rami usando i raccordi a T o a croce prima che versino nel contenitore!"
                )

                // Chapter 4: Unidirectional Valves
                ChapterGuideBox(
                    num = "4",
                    title = "Valvole Antiritorno",
                    desc = "Le valvole (con la freccia ▲) consentono al liquido di scorrere esclusivamente nella direzione indicata. Se la inverti o l'allinei male, il flusso si bloccherà!"
                )

                // Chapter 5: Efficiency Rating
                ChapterGuideBox(
                    num = "5",
                    title = "Stelle ed Efficienza",
                    desc = "Meno mosse esegui e meno tempo impieghi, maggiore sarà la percentuale di efficienza e le tre stelle d'oro ottenute!"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0C141C)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "RICEVUTO, COMINCIA!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChapterGuideBox(
    num: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF00E5FF).copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                lineHeight = 16.sp
            )
        }
    }
}
