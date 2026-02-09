package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun PlayerArea(
    hands: List<Hand>,
    activeHandIndex: Int,
    handResults: Map<Int, HandResult>,
    phase: GamePhase,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hands.size > 1) {
            Text(
                text = "Hand ${activeHandIndex + 1} of ${hands.size}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        Text(
            text = "YOUR HAND${if (hands.size > 1) "S" else ""}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            hands.forEachIndexed { index, hand ->
                HandView(
                    hand = hand,
                    isActive = index == activeHandIndex && phase == GamePhase.PLAYER_TURN,
                    showScore = true,
                    result = handResults[index]
                )
            }
        }
    }
}
