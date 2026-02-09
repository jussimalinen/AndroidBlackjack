package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.ui.theme.ChipBlack
import com.jmalinen.blackjack.ui.theme.ChipBlue
import com.jmalinen.blackjack.ui.theme.ChipGold
import com.jmalinen.blackjack.ui.theme.ChipGreen
import com.jmalinen.blackjack.ui.theme.ChipRed
import com.jmalinen.blackjack.ui.theme.GoldAccent

private data class ChipDenom(val value: Int, val color: Color)

private val chipDenoms = listOf(
    ChipDenom(5, ChipRed),
    ChipDenom(10, ChipBlue),
    ChipDenom(25, ChipGreen),
    ChipDenom(50, ChipBlack),
    ChipDenom(100, ChipGold)
)

@Composable
fun BetSelector(
    currentBet: Int,
    chips: Int,
    rules: CasinoRules,
    onBetChanged: (Int) -> Unit,
    onDeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Place Your Bet",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            chipDenoms.forEach { chip ->
                val canAdd = currentBet + chip.value <= minOf(rules.maximumBet, chips)
                Button(
                    onClick = { onBetChanged(currentBet + chip.value) },
                    enabled = canAdd,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = chip.color,
                        contentColor = Color.White,
                        disabledContainerColor = chip.color.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Text(
                        text = "${chip.value}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onBetChanged(rules.minimumBet) },
                enabled = currentBet > rules.minimumBet
            ) {
                Text("Clear", color = Color.White)
            }

            Button(
                onClick = onDeal,
                enabled = currentBet in rules.minimumBet..minOf(rules.maximumBet, chips),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "DEAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
