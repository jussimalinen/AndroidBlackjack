package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.ui.theme.GoldAccent

@Composable
fun ActionBar(
    availableActions: Set<PlayerAction>,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDouble: () -> Unit,
    onSplit: () -> Unit,
    onSurrender: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton("Hit", PlayerAction.HIT in availableActions, onHit)
        ActionButton("Stand", PlayerAction.STAND in availableActions, onStand)
        ActionButton("Double", PlayerAction.DOUBLE_DOWN in availableActions, onDouble)
        ActionButton("Split", PlayerAction.SPLIT in availableActions, onSplit)
        ActionButton("Surr.", PlayerAction.SURRENDER in availableActions, onSurrender)
    }
}

@Composable
fun InsuranceBar(
    availableActions: Set<PlayerAction>,
    onInsurance: () -> Unit,
    onDecline: () -> Unit,
    onEvenMoney: () -> Unit,
    onDeclineEvenMoney: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (PlayerAction.EVEN_MONEY in availableActions) {
            ActionButton("Even Money", true, onEvenMoney)
            ActionButton("Decline", true, onDeclineEvenMoney)
        } else {
            ActionButton("Insurance", PlayerAction.INSURANCE in availableActions, onInsurance)
            ActionButton("No Thanks", PlayerAction.DECLINE_INSURANCE in availableActions, onDecline)
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldAccent,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
