package com.jmalinen.blackjack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmalinen.blackjack.engine.BasicStrategyAdvisor
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.model.GameState
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.SurrenderPolicy
import com.jmalinen.blackjack.ui.components.ActionBar
import com.jmalinen.blackjack.ui.components.BetSelector
import com.jmalinen.blackjack.ui.components.DealerArea
import com.jmalinen.blackjack.ui.components.ExtraPlayersArea
import com.jmalinen.blackjack.ui.components.GameInfoBar
import com.jmalinen.blackjack.ui.components.InsuranceBar
import com.jmalinen.blackjack.ui.components.PlayerArea
import com.jmalinen.blackjack.ui.components.StrategyChartOverlay
import com.jmalinen.blackjack.ui.theme.FeltGreen
import com.jmalinen.blackjack.ui.theme.GoldAccent
import com.jmalinen.blackjack.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showChart by remember { mutableStateOf(false) }
    val chartData = remember(state.rules) { BasicStrategyAdvisor.getChartData(state.rules) }

    Box(modifier = Modifier.fillMaxSize()) {
        GameScreenContent(
            state = state,
            onBetChanged = viewModel::adjustBet,
            onDeal = viewModel::deal,
            onHit = viewModel::hit,
            onStand = viewModel::stand,
            onDouble = viewModel::doubleDown,
            onSplit = viewModel::split,
            onSurrender = viewModel::surrender,
            onInsurance = viewModel::takeInsurance,
            onDeclineInsurance = viewModel::declineInsurance,
            onEvenMoney = viewModel::takeEvenMoney,
            onDeclineEvenMoney = viewModel::declineEvenMoney,
            onNewRound = viewModel::newRound,
            onReset = viewModel::resetGame,
            onToggleCoach = viewModel::toggleCoach,
            onToggleDeviations = viewModel::toggleDeviations,
            onToggleCount = viewModel::toggleCount,
            onShowChart = { showChart = true },
            onEnd = onNavigateToSettings
        )

        StrategyChartOverlay(
            visible = showChart,
            chartData = chartData,
            hasSurrender = state.rules.surrenderPolicy != SurrenderPolicy.NONE,
            onDismiss = { showChart = false }
        )
    }
}

@Composable
internal fun GameScreenContent(
    state: GameState,
    onBetChanged: (Int) -> Unit = {},
    onDeal: () -> Unit = {},
    onHit: () -> Unit = {},
    onStand: () -> Unit = {},
    onDouble: () -> Unit = {},
    onSplit: () -> Unit = {},
    onSurrender: () -> Unit = {},
    onInsurance: () -> Unit = {},
    onDeclineInsurance: () -> Unit = {},
    onEvenMoney: () -> Unit = {},
    onDeclineEvenMoney: () -> Unit = {},
    onNewRound: () -> Unit = {},
    onReset: () -> Unit = {},
    onToggleCoach: () -> Unit = {},
    onToggleDeviations: () -> Unit = {},
    onToggleCount: () -> Unit = {},
    onShowChart: () -> Unit = {},
    onEnd: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FeltGreen)
    ) {
        val hasExtraPlayers = state.extraPlayers.isNotEmpty()

        val scales = remember(maxWidth, hasExtraPlayers) {
            computeScales(maxWidth, hasExtraPlayers)
        }

        Column(modifier = Modifier.fillMaxSize()) {
        GameInfoBar(
            chips = state.chips,
            shoePenetration = state.shoePenetration,
            handsPlayed = state.handsPlayed,
            handsWon = state.handsWon,
            coachEnabled = state.coachEnabled,
            onToggleCoach = onToggleCoach,
            deviationsEnabled = state.deviationsEnabled,
            onToggleDeviations = onToggleDeviations,
            showCount = state.showCount,
            runningCount = state.runningCount,
            trueCount = state.trueCount,
            onToggleCount = onToggleCount,
            onShowChart = onShowChart,
            onEnd = onEnd,
            coachFeedback = state.coachFeedback,
            coachCorrect = state.coachCorrect,
            coachTotal = state.coachTotal
        )

        DealerArea(
            hand = state.dealerHand,
            showHoleCard = state.showDealerHoleCard,
            modifier = if (hasExtraPlayers) Modifier else Modifier.weight(1f),
            compact = hasExtraPlayers,
            cardScale = scales.dealerScale
        )

        if (hasExtraPlayers) {
            ExtraPlayersArea(
                extraPlayers = state.extraPlayers,
                cardScale = scales.extraPlayerScale
            )
        }

        PlayerArea(
            hands = state.playerHands,
            activeHandIndex = state.activeHandIndex,
            handResults = state.handResults,
            phase = state.phase,
            currentBet = state.currentBet,
            modifier = Modifier.weight(if (hasExtraPlayers) 1f else 1.2f),
            cardScale = scales.playerScale
        )

        // Bottom action area — fixed height so card areas above don't shift
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(scales.actionBoxHeight),
            contentAlignment = Alignment.Center
        ) {
            when (state.phase) {
                GamePhase.BETTING -> {
                    BetSelector(
                        currentBet = state.currentBet,
                        chips = state.chips,
                        rules = state.rules,
                        onBetChanged = onBetChanged,
                        onDeal = onDeal
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
                            onInsurance = onInsurance,
                            onDecline = onDeclineInsurance,
                            onEvenMoney = onEvenMoney,
                            onDeclineEvenMoney = onDeclineEvenMoney
                        )
                    }
                }

                GamePhase.PLAYER_TURN -> {
                    ActionBar(
                        availableActions = state.availableActions,
                        onHit = onHit,
                        onStand = onStand,
                        onDouble = onDouble,
                        onSplit = onSplit,
                        onSurrender = onSurrender
                    )
                }

                GamePhase.DEALING, GamePhase.DEALER_PEEK, GamePhase.EXTRA_PLAYERS_TURN, GamePhase.DEALER_TURN -> {
                    Text(
                        text = when (state.phase) {
                            GamePhase.DEALER_TURN -> "Dealer's turn..."
                            GamePhase.EXTRA_PLAYERS_TURN -> "Other players..."
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
                        onNewRound = onNewRound,
                        compact = scales.compactActions
                    )
                }

                GamePhase.GAME_OVER -> {
                    GameOverArea(
                        handsPlayed = state.handsPlayed,
                        handsWon = state.handsWon,
                        onReset = onReset,
                        compact = scales.compactActions
                    )
                }
            }
        }

        } // Column
    } // BoxWithConstraints
}

private data class UiScales(
    val dealerScale: Float,
    val playerScale: Float,
    val extraPlayerScale: Float,
    val actionBoxHeight: Dp,
    val compactActions: Boolean = false
)

private fun computeScales(maxWidth: Dp, hasExtraPlayers: Boolean): UiScales {
    return when {
        maxWidth < 380.dp -> UiScales(
            dealerScale = 0.65f,
            playerScale = if (hasExtraPlayers) 0.50f else 0.65f,
            extraPlayerScale = 0.50f,
            actionBoxHeight = 120.dp,
            compactActions = true
        )
        maxWidth < 600.dp -> UiScales(
            dealerScale = 1.0f,
            playerScale = if (hasExtraPlayers) 0.75f else 1.0f,
            extraPlayerScale = 0.65f,
            actionBoxHeight = 160.dp
        )
        else -> UiScales(
            dealerScale = 1.4f,
            playerScale = if (hasExtraPlayers) 1.19f else 1.4f,
            extraPlayerScale = 1.05f,
            actionBoxHeight = 200.dp
        )
    }
}

@Composable
private fun ResultArea(
    message: String,
    payout: Int,
    onNewRound: () -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (compact) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = if (compact) 16.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(if (compact) 4.dp else 12.dp))

        Button(
            onClick = onNewRound,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Next Hand", fontWeight = FontWeight.Bold, fontSize = if (compact) 14.sp else 16.sp)
        }
    }
}

@Composable
private fun GameOverArea(
    handsPlayed: Int,
    handsWon: Int,
    onReset: () -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (compact) 4.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Game Over!",
            color = Color(0xFFEF5350),
            fontSize = if (compact) 18.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
        if (!compact) {
            Text(
                text = "You're out of chips",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        Text(
            text = if (compact) "Out of chips · Played: $handsPlayed  Won: $handsWon"
                else "Played: $handsPlayed  Won: $handsWon",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = if (compact) 11.sp else 13.sp
        )

        Spacer(modifier = Modifier.height(if (compact) 2.dp else 12.dp))

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = Color.Black
            )
        ) {
            Text("Play Again", fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 14.sp)
        }
    }
}
