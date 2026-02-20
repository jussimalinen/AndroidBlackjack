package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.ui.theme.GoldAccent
import kotlin.math.roundToInt

private fun Modifier.layoutScale(scale: Float): Modifier {
    if (scale == 1f) return this
    return this
        .layout { measurable, constraints ->
            val upscaled = Constraints(
                minWidth = constraints.minWidth,
                maxWidth = if (constraints.hasBoundedWidth)
                    (constraints.maxWidth / scale).roundToInt()
                else constraints.maxWidth,
                minHeight = constraints.minHeight,
                maxHeight = if (constraints.hasBoundedHeight)
                    (constraints.maxHeight / scale).roundToInt()
                else constraints.maxHeight
            )
            val placeable = measurable.measure(upscaled)
            layout(
                (placeable.width * scale).roundToInt(),
                (placeable.height * scale).roundToInt()
            ) {
                placeable.place(0, 0)
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
}

@Composable
fun HandView(
    hand: Hand,
    isActive: Boolean,
    showScore: Boolean,
    result: HandResult?,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f
) {
    val scaledPadding = (4f * cardScale).coerceIn(2f, 6f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(scaledPadding)
            .layoutScale(cardScale)
    ) {
        if (showScore && hand.cards.isNotEmpty()) {
            val scoreText = buildString {
                append(hand.score)
                if (hand.isSoft) append(" (soft)")
            }
            Text(
                text = scoreText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val cardWidth = 70
        val cardOverlap = 28
        val handWidth = if (hand.cards.size > 1) {
            cardWidth + (hand.cards.size - 1) * cardOverlap
        } else {
            cardWidth
        }

        Box(
            modifier = Modifier
                .height(100.dp)
                .widthIn(min = handWidth.dp)
                .then(
                    if (isActive) Modifier.border(
                        2.dp,
                        GoldAccent,
                        RoundedCornerShape(8.dp)
                    ).padding(2.dp)
                    else Modifier
                ),
            contentAlignment = Alignment.TopStart
        ) {
            if (hand.cards.isEmpty()) {
                EmptyCardSlot()
            } else {
                hand.cards.forEachIndexed { index, card ->
                    key("$index-${card.rank}-${card.suit}") {
                        AnimatedCardView(
                            card = card,
                            modifier = Modifier
                                .offset(x = (index * cardOverlap).dp)
                                .zIndex(index.toFloat())
                        )
                    }
                }
            }
        }

        if (hand.bet > 0) {
            Text(
                text = "\$${hand.bet}",
                color = GoldAccent,
                fontSize = 14.sp,
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
                fontSize = 16.sp,
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
