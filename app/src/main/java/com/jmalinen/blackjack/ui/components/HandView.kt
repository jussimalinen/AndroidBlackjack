package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.ui.theme.GoldAccent

@Composable
fun HandView(
    hand: Hand,
    isActive: Boolean,
    showScore: Boolean,
    result: HandResult?,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f
) {
    val scaledCardWidth = (70 * cardScale).toInt()
    val scaledCardOverlap = (28 * cardScale).toInt()
    val scaledBoxHeight = (110 * cardScale).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        if (showScore && hand.cards.isNotEmpty()) {
            val scoreText = buildString {
                append(hand.score)
                if (hand.isSoft) append(" (soft)")
            }
            Text(
                text = scoreText,
                color = Color.White,
                fontSize = (16 * cardScale).sp,
                fontWeight = FontWeight.Bold
            )
        }

        val handWidth = if (hand.cards.size > 1) {
            scaledCardWidth + (hand.cards.size - 1) * scaledCardOverlap
        } else {
            scaledCardWidth
        }

        Box(
            modifier = Modifier
                .height(scaledBoxHeight)
                .widthIn(min = handWidth.dp)
                .then(
                    if (isActive) Modifier.border(
                        2.dp,
                        GoldAccent,
                        RoundedCornerShape(8.dp)
                    ).padding(2.dp)
                    else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            if (hand.cards.isEmpty()) {
                EmptyCardSlot()
            } else {
                hand.cards.forEachIndexed { index, card ->
                    key("$index-${card.rank}-${card.suit}") {
                        AnimatedCardView(
                            card = card,
                            modifier = Modifier
                                .offset(x = (index * scaledCardOverlap).dp)
                                .zIndex(index.toFloat())
                                .then(
                                    if (cardScale < 1f) Modifier.graphicsLayer {
                                        scaleX = cardScale
                                        scaleY = cardScale
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        }

        if (hand.bet > 0) {
            Text(
                text = "\$${hand.bet}",
                color = GoldAccent,
                fontSize = (14 * cardScale).sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (result != null) {
            val resultColor = when (result) {
                HandResult.THREE_SEVENS -> Color(0xFFFFD700)
                HandResult.BLACKJACK, HandResult.WIN -> Color(0xFF4CAF50)
                HandResult.LOSE, HandResult.BUST -> Color(0xFFEF5350)
                HandResult.PUSH -> Color(0xFFFFEB3B)
                HandResult.SURRENDER -> Color(0xFFFF9800)
            }
            Text(
                text = result.displayName,
                color = resultColor,
                fontSize = (16 * cardScale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
