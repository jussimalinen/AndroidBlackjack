package com.jmalinen.blackjack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.ui.components.ActionBar
import com.jmalinen.blackjack.ui.components.BetSelector
import com.jmalinen.blackjack.ui.components.DealerArea
import com.jmalinen.blackjack.ui.components.GameInfoBar
import com.jmalinen.blackjack.ui.components.InsuranceBar
import com.jmalinen.blackjack.ui.components.PlayerArea
import com.jmalinen.blackjack.ui.theme.FeltGreen
import com.jmalinen.blackjack.ui.theme.GoldAccent
import com.jmalinen.blackjack.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FeltGreen)
    ) {
        GameInfoBar(
            chips = state.chips,
            currentBet = state.currentBet,
            shoePenetration = state.shoePenetration,
            handsPlayed = state.handsPlayed,
            handsWon = state.handsWon,
            coachEnabled = state.coachEnabled,
            onToggleCoach = viewModel::toggleCoach,
            showCount = state.showCount,
            runningCount = state.runningCount,
            trueCount = state.trueCount,
            onToggleCount = viewModel::toggleCount
        )

        DealerArea(
            hand = state.dealerHand,
            showHoleCard = state.showDealerHoleCard,
            modifier = Modifier.weight(1f)
        )

        PlayerArea(
            hands = state.playerHands,
            activeHandIndex = state.activeHandIndex,
            handResults = state.handResults,
            phase = state.phase,
            currentBet = state.currentBet,
            modifier = Modifier.weight(1.2f)
        )

        if (state.coachEnabled && state.coachFeedback.isNotEmpty()) {
            val isCorrect = state.coachFeedback.startsWith("Correct")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.coachFeedback,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                if (state.coachTotal > 0) {
                    Text(
                        text = "${state.coachCorrect}/${state.coachTotal}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom action area — fixed height so card areas above don't shift
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state.phase) {
                GamePhase.BETTING -> {
                    BetSelector(
                        currentBet = state.currentBet,
                        chips = state.chips,
                        rules = state.rules,
                        onBetChanged = viewModel::adjustBet,
                        onDeal = viewModel::deal
                    )
                }

                GamePhase.INSURANCE_OFFERED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Insurance?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        InsuranceBar(
                            availableActions = state.availableActions,
                            onInsurance = viewModel::takeInsurance,
                            onDecline = viewModel::declineInsurance,
                            onEvenMoney = viewModel::takeEvenMoney,
                            onDeclineEvenMoney = viewModel::declineEvenMoney
                        )
                    }
                }

                GamePhase.PLAYER_TURN -> {
                    ActionBar(
                        availableActions = state.availableActions,
                        onHit = viewModel::hit,
                        onStand = viewModel::stand,
                        onDouble = viewModel::doubleDown,
                        onSplit = viewModel::split,
                        onSurrender = viewModel::surrender
                    )
                }

                GamePhase.DEALING, GamePhase.DEALER_PEEK, GamePhase.DEALER_TURN -> {
                    Text(
                        text = when (state.phase) {
                            GamePhase.DEALER_TURN -> "Dealer's turn..."
                            else -> "Dealing..."
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }

                GamePhase.ROUND_COMPLETE -> {
                    ResultArea(
                        message = state.roundMessage,
                        payout = state.roundPayout,
                        onNewRound = viewModel::newRound,
                        onSettings = onNavigateToSettings
                    )
                }

                GamePhase.GAME_OVER -> {
                    GameOverArea(
                        handsPlayed = state.handsPlayed,
                        handsWon = state.handsWon,
                        onReset = viewModel::resetGame,
                        onSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultArea(
    message: String,
    payout: Int,
    onNewRound: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onNewRound,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Next Hand", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            OutlinedButton(onClick = onSettings) {
                Text("Settings", color = Color.White)
            }
        }
    }
}

@Composable
private fun GameOverArea(
    handsPlayed: Int,
    handsWon: Int,
    onReset: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Game Over!",
            color = Color(0xFFEF5350),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "You're out of chips",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Text(
            text = "Played: $handsPlayed  Won: $handsWon",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color.Black
                )
            ) {
                Text("Play Again", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(onClick = onSettings) {
                Text("Settings", color = Color.White)
            }
        }
    }
}
