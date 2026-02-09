package com.jmalinen.blackjack.viewmodel

import androidx.lifecycle.ViewModel
import com.jmalinen.blackjack.engine.BlackjackEngine
import com.jmalinen.blackjack.engine.DealerStrategy
import com.jmalinen.blackjack.engine.PayoutCalculator
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.model.GameState
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.model.Shoe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var shoe = Shoe(6)

    fun startGame(rules: CasinoRules) {
        shoe = Shoe(rules.numberOfDecks)
        _state.value = GameState(
            phase = GamePhase.BETTING,
            rules = rules,
            chips = rules.initialChips,
            currentBet = rules.minimumBet
        )
    }

    fun adjustBet(newBet: Int) {
        _state.update { state ->
            val clamped = newBet.coerceIn(state.rules.minimumBet, minOf(state.rules.maximumBet, state.chips))
            state.copy(currentBet = clamped)
        }
    }

    fun deal() {
        val state = _state.value
        if (state.phase != GamePhase.BETTING) return
        if (state.currentBet > state.chips) return

        if (shoe.needsReshuffle()) {
            shoe.shuffle()
        }

        val playerCard1 = shoe.draw()
        val dealerCard1 = shoe.draw()
        val playerCard2 = shoe.draw()
        val dealerCard2 = shoe.draw()

        val playerHand = Hand(
            cards = listOf(playerCard1, playerCard2),
            bet = state.currentBet
        )
        val dealerHand = Hand(
            cards = listOf(dealerCard1, dealerCard2)
        )

        _state.update {
            it.copy(
                phase = GamePhase.DEALING,
                playerHands = listOf(playerHand),
                activeHandIndex = 0,
                dealerHand = dealerHand,
                chips = it.chips - state.currentBet,
                insuranceBet = 0,
                handResults = emptyMap(),
                showDealerHoleCard = false,
                roundPayout = 0,
                roundMessage = ""
            )
        }

        afterDeal()
    }

    private fun afterDeal() {
        val state = _state.value
        val dealerUpCard = state.dealerHand.cards.firstOrNull() ?: return

        // Check for insurance opportunity
        if (dealerUpCard.rank.isAce && state.rules.insuranceAvailable) {
            val actions = BlackjackEngine.insuranceActions(state.playerHands.first(), state.chips)
            _state.update {
                it.copy(
                    phase = GamePhase.INSURANCE_OFFERED,
                    availableActions = actions
                )
            }
            return
        }

        // Dealer peek for 10-value cards
        if (state.rules.dealerPeeks && dealerUpCard.rank.isTenValue) {
            if (state.dealerHand.isBlackjack) {
                resolveRound()
                return
            }
        }

        // Check player blackjack
        if (state.playerHands.first().isBlackjack) {
            resolveRound()
            return
        }

        startPlayerTurn()
    }

    fun takeInsurance() {
        _state.update { state ->
            val cost = state.currentBet / 2
            state.copy(
                insuranceBet = cost,
                chips = state.chips - cost
            )
        }
        afterInsurance()
    }

    fun declineInsurance() {
        afterInsurance()
    }

    fun takeEvenMoney() {
        // Even money: pay 1:1 immediately and end round
        _state.update { state ->
            val payout = state.currentBet * 2 // original bet + 1:1 win
            state.copy(
                phase = GamePhase.ROUND_COMPLETE,
                chips = state.chips + payout,
                showDealerHoleCard = true,
                roundPayout = payout,
                roundMessage = "Even money paid!",
                handResults = mapOf(0 to HandResult.WIN),
                handsPlayed = state.handsPlayed + 1,
                handsWon = state.handsWon + 1
            )
        }
    }

    fun declineEvenMoney() {
        afterInsurance()
    }

    private fun afterInsurance() {
        val state = _state.value

        if (state.rules.dealerPeeks) {
            if (state.dealerHand.isBlackjack) {
                resolveRound()
                return
            }
        }

        if (state.playerHands.first().isBlackjack) {
            resolveRound()
            return
        }

        startPlayerTurn()
    }

    private fun startPlayerTurn() {
        val state = _state.value
        val hand = state.playerHands[state.activeHandIndex]
        val dealerUpCard = state.dealerHand.cards.firstOrNull()

        val actions = BlackjackEngine.availableActions(
            hand = hand,
            playerHands = state.playerHands,
            dealerUpCard = dealerUpCard,
            chips = state.chips,
            rules = state.rules
        )

        _state.update {
            it.copy(
                phase = GamePhase.PLAYER_TURN,
                availableActions = actions
            )
        }

        // If hand is already finished (e.g., split aces auto-stand), advance
        if (hand.isFinished) {
            advanceToNextHand()
        }
    }

    fun hit() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return

        val card = shoe.draw()
        val updatedHand = state.activeHand?.addCard(card) ?: return
        val updatedHands = state.playerHands.toMutableList()
        updatedHands[state.activeHandIndex] = updatedHand

        _state.update { it.copy(playerHands = updatedHands) }

        if (updatedHand.isBusted || updatedHand.score == 21) {
            advanceToNextHand()
        } else {
            updateAvailableActions()
        }
    }

    fun stand() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return

        val updatedHand = state.activeHand?.copy(isStanding = true) ?: return
        val updatedHands = state.playerHands.toMutableList()
        updatedHands[state.activeHandIndex] = updatedHand

        _state.update { it.copy(playerHands = updatedHands) }
        advanceToNextHand()
    }

    fun doubleDown() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return

        val hand = state.activeHand ?: return
        if (state.chips < hand.bet) return

        val card = shoe.draw()
        val updatedHand = hand.copy(
            cards = hand.cards + card,
            bet = hand.bet * 2,
            isDoubledDown = true
        )
        val updatedHands = state.playerHands.toMutableList()
        updatedHands[state.activeHandIndex] = updatedHand

        _state.update {
            it.copy(
                playerHands = updatedHands,
                chips = it.chips - hand.bet
            )
        }
        advanceToNextHand()
    }

    fun split() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return

        val hand = state.activeHand ?: return
        if (!hand.isPair || state.chips < hand.bet) return

        val isAceSplit = hand.cards.first().rank.isAce

        val hand1 = Hand(
            cards = listOf(hand.cards[0], shoe.draw()),
            bet = hand.bet,
            isSplitHand = true,
            splitFromAces = isAceSplit && !state.rules.hitSplitAces
        )
        val hand2 = Hand(
            cards = listOf(hand.cards[1], shoe.draw()),
            bet = hand.bet,
            isSplitHand = true,
            splitFromAces = isAceSplit && !state.rules.hitSplitAces
        )

        val updatedHands = state.playerHands.toMutableList()
        updatedHands[state.activeHandIndex] = hand1
        updatedHands.add(state.activeHandIndex + 1, hand2)

        _state.update {
            it.copy(
                playerHands = updatedHands,
                chips = it.chips - hand.bet
            )
        }

        // Check if the first split hand is already finished (e.g., split aces)
        if (hand1.isFinished) {
            advanceToNextHand()
        } else {
            updateAvailableActions()
        }
    }

    fun surrender() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return

        val hand = state.activeHand ?: return
        val updatedHand = hand.copy(isSurrendered = true)
        val updatedHands = state.playerHands.toMutableList()
        updatedHands[state.activeHandIndex] = updatedHand

        _state.update { it.copy(playerHands = updatedHands) }
        advanceToNextHand()
    }

    private fun advanceToNextHand() {
        val state = _state.value
        val nextIndex = state.activeHandIndex + 1

        if (nextIndex < state.playerHands.size) {
            _state.update { it.copy(activeHandIndex = nextIndex) }
            val nextHand = state.playerHands[nextIndex]
            if (nextHand.isFinished) {
                advanceToNextHand()
            } else {
                updateAvailableActions()
            }
        } else {
            // All hands played - check if we need dealer turn
            val allBustedOrSurrendered = _state.value.playerHands.all {
                it.isBusted || it.isSurrendered
            }
            if (allBustedOrSurrendered) {
                resolveRound()
            } else {
                playDealerHand()
            }
        }
    }

    private fun updateAvailableActions() {
        val state = _state.value
        val hand = state.playerHands.getOrNull(state.activeHandIndex) ?: return
        val dealerUpCard = state.dealerHand.cards.firstOrNull()

        val actions = BlackjackEngine.availableActions(
            hand = hand,
            playerHands = state.playerHands,
            dealerUpCard = dealerUpCard,
            chips = state.chips,
            rules = state.rules
        )

        _state.update { it.copy(availableActions = actions) }
    }

    private fun playDealerHand() {
        _state.update {
            it.copy(
                phase = GamePhase.DEALER_TURN,
                showDealerHoleCard = true,
                availableActions = emptySet()
            )
        }

        var dealerHand = _state.value.dealerHand
        while (DealerStrategy.shouldHit(dealerHand, _state.value.rules)) {
            val card = shoe.draw()
            dealerHand = dealerHand.addCard(card)
        }

        _state.update { it.copy(dealerHand = dealerHand) }
        resolveRound()
    }

    private fun resolveRound() {
        val state = _state.value

        val result = PayoutCalculator.calculateResults(
            playerHands = state.playerHands,
            dealerHand = state.dealerHand,
            insuranceBet = state.insuranceBet,
            rules = state.rules
        )

        val newChips = state.chips + result.totalPayout

        val winsThisRound = result.handResults.count {
            it.value == HandResult.WIN || it.value == HandResult.BLACKJACK
        }

        val message = buildResultMessage(result, state)

        _state.update {
            it.copy(
                phase = if (newChips <= 0) GamePhase.GAME_OVER else GamePhase.ROUND_COMPLETE,
                handResults = result.handResults,
                chips = newChips,
                showDealerHoleCard = true,
                roundPayout = result.totalPayout,
                roundMessage = message,
                availableActions = emptySet(),
                handsPlayed = it.handsPlayed + 1,
                handsWon = it.handsWon + winsThisRound
            )
        }
    }

    private fun buildResultMessage(
        result: PayoutCalculator.RoundResult,
        state: GameState
    ): String {
        val totalBet = state.playerHands.sumOf { it.bet } + state.insuranceBet
        val net = result.totalPayout - totalBet

        return when {
            result.handResults.values.all { it == HandResult.BLACKJACK } -> "Blackjack!"
            net > 0 -> "You win \$$net!"
            net < 0 -> "You lose \$${-net}"
            else -> "Push"
        }
    }

    fun newRound() {
        val state = _state.value
        if (state.phase != GamePhase.ROUND_COMPLETE) return

        _state.update {
            it.copy(
                phase = GamePhase.BETTING,
                playerHands = emptyList(),
                dealerHand = Hand(),
                activeHandIndex = 0,
                handResults = emptyMap(),
                insuranceBet = 0,
                showDealerHoleCard = false,
                roundPayout = 0,
                roundMessage = "",
                availableActions = emptySet(),
                currentBet = it.currentBet.coerceAtMost(it.chips)
            )
        }
    }

    fun resetGame() {
        val rules = _state.value.rules
        startGame(rules)
    }
}
