package com.jmalinen.blackjack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.ui.theme.CardBack
import com.jmalinen.blackjack.ui.theme.CardBlack
import com.jmalinen.blackjack.ui.theme.CardRed
import com.jmalinen.blackjack.ui.theme.CardWhite

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)

    if (!card.faceUp) {
        Box(
            modifier = modifier
                .size(width = 70.dp, height = 100.dp)
                .clip(shape)
                .background(CardBack)
                .border(1.dp, Color.White.copy(alpha = 0.3f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u2660",
                fontSize = 30.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    } else {
        val textColor = if (card.suit.isRed) CardRed else CardBlack

        Box(
            modifier = modifier
                .size(width = 70.dp, height = 100.dp)
                .clip(shape)
                .background(CardWhite)
                .border(1.dp, Color.Gray.copy(alpha = 0.5f), shape)
                .padding(5.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = card.rank.symbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 16.sp
                )
                Text(
                    text = card.suit.symbol.toString(),
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 14.sp
                )
            }
            Text(
                text = card.suit.symbol.toString(),
                fontSize = 34.sp,
                color = textColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

val LocalAnimationsEnabled = compositionLocalOf { true }

@Composable
fun AnimatedCardView(
    card: Card,
    modifier: Modifier = Modifier
) {
    if (!LocalAnimationsEnabled.current) {
        CardView(card = card, modifier = modifier)
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 2 }
    ) {
        CardView(card = card, modifier = modifier)
    }
}

@Composable
fun EmptyCardSlot(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(width = 70.dp, height = 100.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
    )
}
