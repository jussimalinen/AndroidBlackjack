package com.jmalinen.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    val flipProgress by animateFloatAsState(
        targetValue = if (showHoleCard) 1f else 0f,
        animationSpec = tween(300),
        label = "holeCardFlip"
    )

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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        val scoreAlpha = if (showHoleCard && hand.cards.isNotEmpty()) flipProgress else 0f
        if (hand.cards.isNotEmpty()) {
            Text(
                text = if (showHoleCard) {
                    buildString {
                        append(hand.score)
                        if (hand.isSoft) append(" (soft)")
                    }
                } else "",
                color = Color.White.copy(alpha = scoreAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
            showScore = false,
            result = null,
            modifier = if (!showHoleCard) Modifier else Modifier.graphicsLayer {
                // Subtle scale pop when hole card reveals
                val scale = 1f + (0.03f * (1f - flipProgress))
                scaleX = scale
                scaleY = scale
            }
        )
    }
}
