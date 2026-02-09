package com.jmalinen.blackjack.viewmodel

import androidx.lifecycle.ViewModel
import com.jmalinen.blackjack.model.BlackjackPayout
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.SurrenderPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _rules = MutableStateFlow(CasinoRules())
    val rules: StateFlow<CasinoRules> = _rules.asStateFlow()

    fun updateNumberOfDecks(count: Int) {
        _rules.update { it.copy(numberOfDecks = count.coerceIn(1, 8)) }
    }

    fun toggleDealerStandsOnSoft17(value: Boolean) {
        _rules.update { it.copy(dealerStandsOnSoft17 = value) }
    }

    fun toggleDealerPeeks(value: Boolean) {
        _rules.update { it.copy(dealerPeeks = value) }
    }

    fun setBlackjackPayout(payout: BlackjackPayout) {
        _rules.update { it.copy(blackjackPayout = payout) }
    }

    fun setSurrenderPolicy(policy: SurrenderPolicy) {
        _rules.update { it.copy(surrenderPolicy = policy) }
    }

    fun toggleDoubleAfterSplit(value: Boolean) {
        _rules.update { it.copy(doubleAfterSplit = value) }
    }

    fun toggleResplitAces(value: Boolean) {
        _rules.update { it.copy(resplitAces = value) }
    }

    fun setMaxSplitHands(count: Int) {
        _rules.update { it.copy(maxSplitHands = count.coerceIn(2, 4)) }
    }

    fun toggleHitSplitAces(value: Boolean) {
        _rules.update { it.copy(hitSplitAces = value) }
    }

    fun toggleInsurance(value: Boolean) {
        _rules.update { it.copy(insuranceAvailable = value) }
    }

    fun applyPreset(preset: String) {
        _rules.value = when (preset) {
            "Vegas" -> CasinoRules(
                numberOfDecks = 6,
                dealerStandsOnSoft17 = false,
                dealerPeeks = true,
                blackjackPayout = BlackjackPayout.THREE_TO_TWO,
                surrenderPolicy = SurrenderPolicy.LATE,
                doubleAfterSplit = true,
                resplitAces = false,
                maxSplitHands = 4,
                hitSplitAces = false,
                insuranceAvailable = true
            )
            "European" -> CasinoRules(
                numberOfDecks = 6,
                dealerStandsOnSoft17 = true,
                dealerPeeks = false,
                blackjackPayout = BlackjackPayout.THREE_TO_TWO,
                surrenderPolicy = SurrenderPolicy.NONE,
                doubleAfterSplit = true,
                resplitAces = false,
                maxSplitHands = 4,
                hitSplitAces = false,
                insuranceAvailable = false
            )
            "Favorable" -> CasinoRules(
                numberOfDecks = 1,
                dealerStandsOnSoft17 = true,
                dealerPeeks = true,
                blackjackPayout = BlackjackPayout.THREE_TO_TWO,
                surrenderPolicy = SurrenderPolicy.EARLY,
                doubleAfterSplit = true,
                resplitAces = true,
                maxSplitHands = 4,
                hitSplitAces = true,
                insuranceAvailable = true
            )
            else -> CasinoRules()
        }
    }
}
