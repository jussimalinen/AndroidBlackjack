package com.jmalinen.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.ui.theme.FeltGreenDark
import com.jmalinen.blackjack.ui.theme.GoldAccent

@Composable
fun GameInfoBar(
    chips: Int,
    currentBet: Int,
    shoePenetration: Float,
    handsPlayed: Int,
    handsWon: Int,
    coachEnabled: Boolean,
    onToggleCoach: () -> Unit,
    deviationsEnabled: Boolean,
    onToggleDeviations: () -> Unit,
    showCount: Boolean,
    runningCount: Int,
    trueCount: Float,
    onToggleCount: () -> Unit,
    onShowChart: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FeltGreenDark)
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Chips:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "\$$chips",
                    color = GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "W: $handsWon / $handsPlayed",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val coachColor = if (coachEnabled) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f)
                Text(
                    text = "Coach",
                    color = coachColor,
                    fontSize = 12.sp,
                    fontWeight = if (coachEnabled) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (coachEnabled) Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                        .clickable { onToggleCoach() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )

                if (coachEnabled) {
                    val devColor = if (deviationsEnabled) Color(0xFFFF9800) else Color.White.copy(alpha = 0.4f)
                    Text(
                        text = "Dev",
                        color = devColor,
                        fontSize = 12.sp,
                        fontWeight = if (deviationsEnabled) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (deviationsEnabled) Modifier.background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            )
                            .clickable { onToggleDeviations() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                val countColor = if (showCount) Color(0xFF42A5F5) else Color.White.copy(alpha = 0.4f)
                val trueCountStr = if (trueCount >= 0) "+%.1f".format(trueCount) else "%.1f".format(trueCount)
                val rcStr = if (runningCount >= 0) "+$runningCount" else "$runningCount"
                Text(
                    text = if (showCount) "RC:$rcStr TC:$trueCountStr" else "Count",
                    color = countColor,
                    fontSize = 12.sp,
                    fontWeight = if (showCount) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (showCount) Modifier.background(Color(0xFF42A5F5).copy(alpha = 0.2f))
                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                        .clickable { onToggleCount() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )

            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bet:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "\$$currentBet",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shoe",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { 1f - shoePenetration },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = if (shoePenetration > 0.7f) Color(0xFFFF9800) else GoldAccent,
                trackColor = Color.White.copy(alpha = 0.1f),
                drawStopIndicator = {}
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEnd) {
                Text("End", color = Color.White)
            }
            OutlinedButton(onClick = onShowChart) {
                Text("Chart", color = Color.White)
            }
        }
    }
}
