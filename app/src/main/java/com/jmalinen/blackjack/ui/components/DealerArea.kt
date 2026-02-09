package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.model.Hand

@Composable
fun DealerArea(
    hand: Hand,
    showHoleCard: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DEALER",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        val displayHand = if (showHoleCard) {
            hand
        } else {
            hand.copy(
                cards = hand.cards.mapIndexed { index, card ->
                    if (index == 1) card.copy(faceUp = false) else card
                }
            )
        }

        HandView(
            hand = displayHand,
            isActive = false,
            showScore = showHoleCard && hand.cards.isNotEmpty(),
            result = null
        )
    }
}
