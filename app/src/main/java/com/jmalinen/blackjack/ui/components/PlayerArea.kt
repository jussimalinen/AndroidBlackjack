package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.ui.theme.GoldAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerArea(
    hands: List<Hand>,
    activeHandIndex: Int,
    handResults: Map<Int, HandResult>,
    phase: GamePhase,
    currentBet: Int,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f
) {
    val verticalPadding = if (cardScale < 1f) 4.dp else 16.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hands.size > 1) {
            Text(
                text = "Hand ${activeHandIndex + 1} of ${hands.size}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }

        Text(
            text = "YOUR HAND${if (hands.size > 1) "S" else ""}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            hands.forEachIndexed { index, hand ->
                HandView(
                    hand = hand,
                    isActive = index == activeHandIndex && phase == GamePhase.PLAYER_TURN,
                    showScore = true,
                    result = handResults[index],
                    cardScale = cardScale
                )
            }
        }

        val totalBet = if (hands.isNotEmpty()) hands.sumOf { it.bet } else currentBet
        if (totalBet > 0) {
            Text(
                text = "Bet: \$$totalBet",
                color = GoldAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
