package com.jmalinen.blackjack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jmalinen.blackjack.model.BlackjackPayout
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.SurrenderPolicy
import com.jmalinen.blackjack.ui.theme.GoldAccent
import com.jmalinen.blackjack.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onStartGame: (CasinoRules) -> Unit
) {
    val rules by settingsViewModel.rules.collectAsStateWithLifecycle()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusBarPadding.calculateTopPadding())
            .padding(16.dp)
    ) {
        Text(
            text = "Blackjack",
            style = MaterialTheme.typography.headlineLarge,
            color = GoldAccent
        )
        Text(
            text = "Configure Casino Rules",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Presets
        SectionHeader("Presets")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton("Vegas", Modifier.weight(1f)) { settingsViewModel.applyPreset("Vegas") }
            PresetButton("European", Modifier.weight(1f)) { settingsViewModel.applyPreset("European") }
            PresetButton("Favorable", Modifier.weight(1f)) { settingsViewModel.applyPreset("Favorable") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton("Helsinki", Modifier.weight(1f)) { settingsViewModel.applyPreset("Helsinki") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))

        // Deck count
        SectionHeader("Number of Decks: ${rules.numberOfDecks}")
        Slider(
            value = rules.numberOfDecks.toFloat(),
            onValueChange = { settingsViewModel.updateNumberOfDecks(it.toInt()) },
            valueRange = 1f..8f,
            steps = 6,
            colors = SliderDefaults.colors(
                thumbColor = GoldAccent,
                activeTrackColor = GoldAccent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dealer rules
        SectionHeader("Dealer Rules")
        SwitchRow(
            label = "Dealer stands on soft 17",
            checked = rules.dealerStandsOnSoft17,
            onCheckedChange = settingsViewModel::toggleDealerStandsOnSoft17
        )
        SwitchRow(
            label = "Dealer peeks for blackjack",
            checked = rules.dealerPeeks,
            onCheckedChange = settingsViewModel::toggleDealerPeeks
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Payout
        SectionHeader("Blackjack Payout")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlackjackPayout.entries.forEach { payout ->
                val selected = rules.blackjackPayout == payout
                Button(
                    onClick = { settingsViewModel.setBlackjackPayout(payout) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) GoldAccent else Color.Gray.copy(alpha = 0.3f),
                        contentColor = if (selected) Color.Black else Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(payout.displayName, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Surrender
        SectionHeader("Surrender Policy")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SurrenderPolicy.entries.forEach { policy ->
                val selected = rules.surrenderPolicy == policy
                Button(
                    onClick = { settingsViewModel.setSurrenderPolicy(policy) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) GoldAccent else Color.Gray.copy(alpha = 0.3f),
                        contentColor = if (selected) Color.Black else Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (policy) {
                            SurrenderPolicy.NONE -> "None"
                            SurrenderPolicy.LATE -> "Late"
                            SurrenderPolicy.EARLY -> "Early"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Split rules
        SectionHeader("Split Rules")
        SwitchRow(
            label = "Double after split",
            checked = rules.doubleAfterSplit,
            onCheckedChange = settingsViewModel::toggleDoubleAfterSplit
        )
        SwitchRow(
            label = "Re-split aces",
            checked = rules.resplitAces,
            onCheckedChange = settingsViewModel::toggleResplitAces
        )
        SwitchRow(
            label = "Hit split aces",
            checked = rules.hitSplitAces,
            onCheckedChange = settingsViewModel::toggleHitSplitAces
        )

        SectionHeader("Max Split Hands: ${rules.maxSplitHands}")
        Slider(
            value = rules.maxSplitHands.toFloat(),
            onValueChange = { settingsViewModel.setMaxSplitHands(it.toInt()) },
            valueRange = 2f..4f,
            steps = 1,
            colors = SliderDefaults.colors(
                thumbColor = GoldAccent,
                activeTrackColor = GoldAccent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Other
        SectionHeader("Other")
        SwitchRow(
            label = "Insurance available",
            checked = rules.insuranceAvailable,
            onCheckedChange = settingsViewModel::toggleInsurance
        )
        SwitchRow(
            label = "Three 7s pays 3:1",
            checked = rules.threeSevensPays3to1,
            onCheckedChange = settingsViewModel::toggleThreeSevensBonus
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Training
        SectionHeader("Training")
        SwitchRow(
            label = "Soft hands only",
            checked = rules.trainSoftHands,
            onCheckedChange = settingsViewModel::toggleTrainSoftHands
        )
        SwitchRow(
            label = "Paired hands only",
            checked = rules.trainPairedHands,
            onCheckedChange = settingsViewModel::toggleTrainPairedHands
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Extra players
        SectionHeader("Extra Players: ${rules.extraPlayers}")
        Text(
            text = "Computer-controlled players for card counting practice",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Slider(
            value = rules.extraPlayers.toFloat(),
            onValueChange = { settingsViewModel.setExtraPlayers(it.toInt()) },
            valueRange = 0f..2f,
            steps = 1,
            colors = SliderDefaults.colors(
                thumbColor = GoldAccent,
                activeTrackColor = GoldAccent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Start button
        Button(
            onClick = { onStartGame(rules) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "START GAME",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldAccent,
                checkedTrackColor = GoldAccent.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun PresetButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
