package com.jmalinen.blackjack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jmalinen.blackjack.engine.BasicStrategyAdvisor
import com.jmalinen.blackjack.engine.BlackjackEngine
import com.jmalinen.blackjack.engine.DealerStrategy
import com.jmalinen.blackjack.engine.PayoutCalculator
import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Rank
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.model.GameState
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.Shoe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var shoe = Shoe(6)
    private var dealJob: Job? = null
    private var runningCount = 0
    private var pendingHoleCardValue = 0

    companion object {
        private const val DEAL_CARD_DELAY = 300L
        private const val DEALER_DRAW_DELAY = 500L
        private const val HOLE_CARD_REVEAL_DELAY = 400L
    }

    private fun updateCountState() {
        val decksRemaining = (shoe.cardsRemaining / 52f).coerceAtLeast(0.5f)
        val trueCount = runningCount / decksRemaining
        _state.update { it.copy(shoePenetration = shoe.penetration, runningCount = runningCount, trueCount = trueCount) }
    }

    private fun drawAndCount(): Card {
        val card = shoe.draw()
        runningCount += card.rank.hiLoValue
        updateCountState()
        return card
    }

    private fun countHoleCard() {
        runningCount += pendingHoleCardValue
        pendingHoleCardValue = 0
        updateCountState()
    }

    fun startGame(rules: CasinoRules) {
        dealJob?.cancel()
        shoe = Shoe(rules.numberOfDecks)
        runningCount = 0
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

    fun toggleCoach() {
        _state.update {
            it.copy(
                coachEnabled = !it.coachEnabled,
                coachFeedback = "",
                coachCorrect = 0,
                coachTotal = 0
            )
        }
    }

    fun toggleCount() {
        _state.update { it.copy(showCount = !it.showCount) }
    }

    private fun evaluateCoach(chosenAction: PlayerAction) {
        val state = _state.value
        if (!state.coachEnabled) return
        val hand = state.activeHand ?: return
        val dealerUpCard = state.dealerHand.cards.firstOrNull() ?: return

        val optimal = BasicStrategyAdvisor.optimalAction(
            hand = hand,
            dealerUpCard = dealerUpCard,
            availableActions = state.availableActions,
            rules = state.rules
        )

        val correct = chosenAction == optimal
        val feedback = if (correct) {
            "Correct! ${chosenAction.displayName} was optimal."
        } else {
            "Optimal play: ${optimal.displayName} (you chose ${chosenAction.displayName})"
        }

        _state.update {
            it.copy(
                coachFeedback = feedback,
                coachCorrect = it.coachCorrect + if (correct) 1 else 0,
                coachTotal = it.coachTotal + 1
            )
        }
    }

    fun deal() {
        val state = _state.value
        if (state.phase != GamePhase.BETTING) return
        if (state.currentBet > state.chips) return

        if (state.rules.isTrainingMode || shoe.needsReshuffle()) {
            shoe.shuffle()
            runningCount = 0
            _state.update { it.copy(shoePenetration = 0f, runningCount = 0, trueCount = 0f) }
        }

        // Draw player cards — forced hand type in training mode
        val playerCard1: Card
        val playerCard2: Card
        if (state.rules.isTrainingMode) {
            val dealSoft = when {
                state.rules.trainSoftHands && state.rules.trainPairedHands -> Math.random() < 0.5
                state.rules.trainSoftHands -> true
                else -> false
            }
            if (dealSoft) {
                playerCard1 = shoe.drawMatching { it.rank.isAce }
                playerCard2 = shoe.drawMatching { !it.rank.isAce && !it.rank.isTenValue }
            } else {
                val rank = Rank.entries.random()
                playerCard1 = shoe.drawMatching { it.rank == rank }
                playerCard2 = shoe.drawMatching { it.rank == rank }
            }
        } else {
            playerCard1 = drawAndCount()
            playerCard2 = drawAndCount()
        }

        val dealerCard1 = drawAndCount()
        val dealerCard2 = shoe.draw() // hole card — counted when revealed
        pendingHoleCardValue = dealerCard2.rank.hiLoValue

        // Start with empty hands, transition to DEALING
        _state.update {
            it.copy(
                phase = GamePhase.DEALING,
                playerHands = listOf(Hand(cards = emptyList(), bet = state.currentBet)),
                activeHandIndex = 0,
                dealerHand = Hand(),
                chips = it.chips - state.currentBet,
                insuranceBet = 0,
                handResults = emptyMap(),
                showDealerHoleCard = false,
                roundPayout = 0,
                roundMessage = ""
            )
        }

        // Animate cards one at a time
        dealJob = viewModelScope.launch {
            // Player card 1
            delay(DEAL_CARD_DELAY)
            _state.update {
                val hands = it.playerHands.toMutableList()
                hands[0] = hands[0].addCard(playerCard1)
                it.copy(playerHands = hands)
            }

            // Dealer card 1 (face up)
            delay(DEAL_CARD_DELAY)
            _state.update {
                it.copy(dealerHand = it.dealerHand.addCard(dealerCard1))
            }

            // Player card 2
            delay(DEAL_CARD_DELAY)
            _state.update {
                val hands = it.playerHands.toMutableList()
                hands[0] = hands[0].addCard(playerCard2)
                it.copy(playerHands = hands)
            }

            // Dealer card 2 (face down — hidden by showDealerHoleCard=false)
            delay(DEAL_CARD_DELAY)
            _state.update {
                it.copy(dealerHand = it.dealerHand.addCard(dealerCard2))
            }

            afterDeal()
        }
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
        if (_state.value.coachEnabled) {
            _state.update { it.copy(coachFeedback = "Basic strategy: never take insurance") }
        }
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
        if (_state.value.coachEnabled) {
            _state.update { it.copy(coachFeedback = "Correct! Never take insurance.") }
        }
        afterInsurance()
    }

    fun takeEvenMoney() {
        if (_state.value.coachEnabled) {
            _state.update { it.copy(coachFeedback = "Basic strategy: decline even money") }
        }
        countHoleCard()
        _state.update { state ->
            val payout = state.currentBet * 2
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
        if (_state.value.coachEnabled) {
            _state.update { it.copy(coachFeedback = "Correct! Decline even money.") }
        }
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

        if (hand.isFinished) {
            advanceToNextHand()
        }
    }

    fun hit() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return
        evaluateCoach(PlayerAction.HIT)

        val card = drawAndCount()
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
        evaluateCoach(PlayerAction.STAND)

        val updatedHand = state.activeHand?.copy(isStanding = true) ?: return
        val updatedHands = state.playerHands.toMutableList()
        updatedHands[state.activeHandIndex] = updatedHand

        _state.update { it.copy(playerHands = updatedHands) }
        advanceToNextHand()
    }

    fun doubleDown() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return
        evaluateCoach(PlayerAction.DOUBLE_DOWN)

        val hand = state.activeHand ?: return
        if (state.chips < hand.bet) return

        val card = drawAndCount()
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
        evaluateCoach(PlayerAction.SPLIT)

        val hand = state.activeHand ?: return
        if (!hand.isPair || state.chips < hand.bet) return

        val isAceSplit = hand.cards.first().rank.isAce

        val hand1 = Hand(
            cards = listOf(hand.cards[0], drawAndCount()),
            bet = hand.bet,
            isSplitHand = true,
            splitFromAces = isAceSplit && !state.rules.hitSplitAces
        )
        val hand2 = Hand(
            cards = listOf(hand.cards[1], drawAndCount()),
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

        if (hand1.isFinished) {
            advanceToNextHand()
        } else {
            updateAvailableActions()
        }
    }

    fun surrender() {
        val state = _state.value
        if (state.phase != GamePhase.PLAYER_TURN) return
        evaluateCoach(PlayerAction.SURRENDER)

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
        countHoleCard()
        _state.update {
            it.copy(
                phase = GamePhase.DEALER_TURN,
                showDealerHoleCard = true,
                availableActions = emptySet()
            )
        }

        dealJob = viewModelScope.launch {
            // Pause to let player see the hole card reveal
            delay(HOLE_CARD_REVEAL_DELAY)

            var dealerHand = _state.value.dealerHand
            while (DealerStrategy.shouldHit(dealerHand, _state.value.rules)) {
                delay(DEALER_DRAW_DELAY)
                val card = drawAndCount()
                dealerHand = dealerHand.addCard(card)
                _state.update { it.copy(dealerHand = dealerHand) }
            }

            // Short pause before showing results
            delay(DEAL_CARD_DELAY)
            resolveRound()
        }
    }

    private fun resolveRound() {
        if (!_state.value.showDealerHoleCard) {
            countHoleCard()
        }
        val state = _state.value

        val result = PayoutCalculator.calculateResults(
            playerHands = state.playerHands,
            dealerHand = state.dealerHand,
            insuranceBet = state.insuranceBet,
            rules = state.rules
        )

        val newChips = state.chips + result.totalPayout

        val winsThisRound = result.handResults.count {
            it.value == HandResult.WIN || it.value == HandResult.BLACKJACK || it.value == HandResult.THREE_SEVENS
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
            result.handResults.values.any { it == HandResult.THREE_SEVENS } -> "Three 7s! You win \$$net!"
            result.handResults.values.all { it == HandResult.BLACKJACK } -> "Blackjack!"
            net > 0 -> "You win \$$net!"
            net < 0 -> "You lose \$${-net}"
            else -> "Push"
        }
    }

    fun newRound() {
        val state = _state.value
        if (state.phase != GamePhase.ROUND_COMPLETE) return

        dealJob?.cancel()
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
                currentBet = it.currentBet.coerceAtMost(it.chips),
                coachFeedback = ""
            )
        }
    }

    fun resetGame() {
        dealJob?.cancel()
        val rules = _state.value.rules
        startGame(rules)
    }
}
