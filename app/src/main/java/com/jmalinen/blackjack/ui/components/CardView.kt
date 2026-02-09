package com.jmalinen.blackjack.ui.components

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
                .size(width = 56.dp, height = 80.dp)
                .clip(shape)
                .background(CardBack)
                .border(1.dp, Color.White.copy(alpha = 0.3f), shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u2660",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    } else {
        val textColor = if (card.suit.isRed) CardRed else CardBlack

        Box(
            modifier = modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(shape)
                .background(CardWhite)
                .border(1.dp, Color.Gray.copy(alpha = 0.5f), shape)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = card.rank.symbol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 14.sp
                )
                Text(
                    text = card.suit.symbol.toString(),
                    fontSize = 12.sp,
                    color = textColor,
                    lineHeight = 12.sp
                )
            }
            Text(
                text = card.suit.symbol.toString(),
                fontSize = 28.sp,
                color = textColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun EmptyCardSlot(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 80.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
    )
}
