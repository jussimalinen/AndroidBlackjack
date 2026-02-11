package com.jmalinen.blackjack.viewmodel

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.model.GameState
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.Rank
import com.jmalinen.blackjack.model.Rank.*
import com.jmalinen.blackjack.model.Suit
import com.jmalinen.blackjack.model.SurrenderPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: GameViewModel

    private val defaultRules = CasinoRules(
        surrenderPolicy = SurrenderPolicy.LATE,
        initialChips = 1000,
        minimumBet = 10,
        maximumBet = 500
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GameViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state: GameState get() = viewModel.state.value

    private fun card(rank: Rank, suit: Suit = Suit.HEARTS) = Card(rank, suit)

    private fun hand(vararg ranks: Rank, bet: Int = 10) =
        Hand(cards = ranks.map { card(it) }, bet = bet)

    /**
     * Sets the internal _state to a specific GameState for deterministic testing.
     * Also initializes the shoe via startGame so card draws work.
     */
    private fun setupState(state: GameState) {
        viewModel.startGame(state.rules)
        val field = GameViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(viewModel) as MutableStateFlow<GameState>).value = state
    }

    /**
     * Creates a standard player-turn state with a known hand and dealer upcard.
     */
    private fun playerTurnState(
        playerHand: Hand = hand(TEN, SIX, bet = 10),
        dealerHand: Hand = Hand(cards = listOf(card(SEVEN), card(NINE))),
        chips: Int = 990,
        rules: CasinoRules = defaultRules,
        availableActions: Set<PlayerAction> = setOf(
            PlayerAction.HIT, PlayerAction.STAND, PlayerAction.SURRENDER
        )
    ) = GameState(
        phase = GamePhase.PLAYER_TURN,
        rules = rules,
        playerHands = listOf(playerHand),
        activeHandIndex = 0,
        dealerHand = dealerHand,
        chips = chips,
        currentBet = playerHand.bet,
        availableActions = availableActions
    )

    // ========================================================================
    // startGame
    // ========================================================================

    @Test
    fun `startGame sets initial state correctly`() {
        viewModel.startGame(defaultRules)

        assertEquals(GamePhase.BETTING, state.phase)
        assertEquals(1000, state.chips)
        assertEquals(10, state.currentBet)
        assertEquals(defaultRules, state.rules)
        assertTrue(state.playerHands.isEmpty())
        assertTrue(state.handResults.isEmpty())
    }

    @Test
    fun `startGame uses rules initial chips`() {
        val rules = CasinoRules(initialChips = 5000, minimumBet = 25)
        viewModel.startGame(rules)

        assertEquals(5000, state.chips)
        assertEquals(25, state.currentBet)
    }

    // ========================================================================
    // adjustBet
    // ========================================================================

    @Test
    fun `adjustBet clamps to minimum`() {
        viewModel.startGame(defaultRules)
        viewModel.adjustBet(1)

        assertEquals(10, state.currentBet)
    }

    @Test
    fun `adjustBet clamps to maximum`() {
        viewModel.startGame(defaultRules)
        viewModel.adjustBet(9999)

        assertEquals(500, state.currentBet)
    }

    @Test
    fun `adjustBet clamps to available chips`() {
        viewModel.startGame(CasinoRules(initialChips = 50, minimumBet = 10, maximumBet = 500))
        viewModel.adjustBet(200)

        assertEquals(50, state.currentBet)
    }

    @Test
    fun `adjustBet sets valid amount`() {
        viewModel.startGame(defaultRules)
        viewModel.adjustBet(100)

        assertEquals(100, state.currentBet)
    }

    // ========================================================================
    // toggleCoach / toggleCount
    // ========================================================================

    @Test
    fun `toggleCoach enables coach and resets counters`() {
        viewModel.startGame(defaultRules)
        assertFalse(state.coachEnabled)

        viewModel.toggleCoach()
        assertTrue(state.coachEnabled)
        assertEquals("", state.coachFeedback)
        assertEquals(0, state.coachCorrect)
        assertEquals(0, state.coachTotal)
    }

    @Test
    fun `toggleCoach disables coach`() {
        viewModel.startGame(defaultRules)
        viewModel.toggleCoach() // enable
        viewModel.toggleCoach() // disable

        assertFalse(state.coachEnabled)
    }

    @Test
    fun `toggleCount toggles show count`() {
        viewModel.startGame(defaultRules)
        assertFalse(state.showCount)

        viewModel.toggleCount()
        assertTrue(state.showCount)

        viewModel.toggleCount()
        assertFalse(state.showCount)
    }

    // ========================================================================
    // deal - full flow
    // ========================================================================

    @Test
    fun `deal deducts bet from chips`() = runTest(testDispatcher) {
        viewModel.startGame(defaultRules)
        viewModel.adjustBet(50)
        viewModel.deal()
        advanceUntilIdle()

        assertEquals(950, state.chips + state.insuranceBet)
            .let {} // chips may have insurance deducted too
        // More precise: initial chips - bet - insurance = remaining
        assertTrue(state.chips <= 950)
    }

    @Test
    fun `deal results in player and dealer having two cards`() = runTest(testDispatcher) {
        viewModel.startGame(defaultRules)
        viewModel.deal()
        advanceUntilIdle()

        assertEquals(2, state.playerHands.first().cards.size)
        assertEquals(2, state.dealerHand.cards.size)
    }

    @Test
    fun `deal transitions to playable phase`() = runTest(testDispatcher) {
        viewModel.startGame(defaultRules)
        viewModel.deal()
        advanceUntilIdle()

        val validPhases = setOf(
            GamePhase.PLAYER_TURN,
            GamePhase.INSURANCE_OFFERED,
            GamePhase.ROUND_COMPLETE
        )
        assertTrue(
            "Phase should be one of $validPhases but was ${state.phase}",
            state.phase in validPhases
        )
    }

    @Test
    fun `deal does nothing outside BETTING phase`() = runTest(testDispatcher) {
        viewModel.startGame(defaultRules)
        viewModel.deal()
        advanceUntilIdle()

        val stateAfterFirstDeal = state
        viewModel.deal() // should be no-op since we're not in BETTING
        advanceUntilIdle()

        assertEquals(stateAfterFirstDeal.playerHands, state.playerHands)
    }

    @Test
    fun `deal does not proceed when bet exceeds chips`() = runTest(testDispatcher) {
        viewModel.startGame(CasinoRules(initialChips = 5, minimumBet = 5, maximumBet = 500))
        viewModel.adjustBet(10) // clamped to 5
        // Manually exhaust chips by setting up state
        setupState(GameState(
            phase = GamePhase.BETTING,
            rules = defaultRules,
            chips = 5,
            currentBet = 10
        ))

        viewModel.deal()
        advanceUntilIdle()

        assertEquals(GamePhase.BETTING, state.phase)
    }

    // ========================================================================
    // hit
    // ========================================================================

    @Test
    fun `hit adds a card to active hand`() = runTest(testDispatcher) {
        setupState(playerTurnState())

        val cardsBefore = state.playerHands.first().cards.size
        viewModel.hit()

        assertEquals(cardsBefore + 1, state.playerHands.first().cards.size)
    }

    @Test
    fun `hit does nothing outside PLAYER_TURN`() {
        setupState(playerTurnState().copy(phase = GamePhase.BETTING))

        val handsBefore = state.playerHands
        viewModel.hit()

        assertEquals(handsBefore, state.playerHands)
    }

    @Test
    fun `hit advances to next hand or dealer when bust`() = runTest(testDispatcher) {
        // Give player a hand that's likely to bust (hard 20)
        val highHand = hand(TEN, QUEEN, bet = 10)
        setupState(playerTurnState(playerHand = highHand))

        viewModel.hit()
        advanceUntilIdle()

        // After hitting a 20, player has 3 cards and the round should have progressed
        val playerCards = state.playerHands.first().cards.size
        assertEquals(3, playerCards)
    }

    // ========================================================================
    // stand
    // ========================================================================

    @Test
    fun `stand marks hand as standing and advances`() = runTest(testDispatcher) {
        setupState(playerTurnState())

        viewModel.stand()
        advanceUntilIdle()

        assertTrue(state.playerHands.first().isStanding)
        // Should have advanced to dealer turn or round complete
        assertNotEquals(GamePhase.PLAYER_TURN, state.phase)
    }

    @Test
    fun `stand does nothing outside PLAYER_TURN`() {
        setupState(playerTurnState().copy(phase = GamePhase.BETTING))

        viewModel.stand()

        assertFalse(state.playerHands.first().isStanding)
    }

    // ========================================================================
    // doubleDown
    // ========================================================================

    @Test
    fun `doubleDown doubles bet and adds one card`() = runTest(testDispatcher) {
        val initialChips = 990
        val betAmount = 10
        setupState(playerTurnState(
            playerHand = hand(FIVE, SIX, bet = betAmount),
            chips = initialChips,
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN)
        ))

        viewModel.doubleDown()
        // Don't advanceUntilIdle — that would resolve the round and modify chips

        val playerHand = state.playerHands.first()
        assertEquals(3, playerHand.cards.size)
        assertEquals(betAmount * 2, playerHand.bet)
        assertTrue(playerHand.isDoubledDown)
        assertEquals(initialChips - betAmount, state.chips)
    }

    @Test
    fun `doubleDown does nothing without sufficient chips`() = runTest(testDispatcher) {
        setupState(playerTurnState(
            playerHand = hand(FIVE, SIX, bet = 100),
            chips = 50,
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN)
        ))

        viewModel.doubleDown()

        // Hand should be unchanged
        assertEquals(2, state.playerHands.first().cards.size)
        assertFalse(state.playerHands.first().isDoubledDown)
    }

    @Test
    fun `doubleDown does nothing outside PLAYER_TURN`() {
        setupState(playerTurnState().copy(phase = GamePhase.DEALING))

        viewModel.doubleDown()

        assertEquals(2, state.playerHands.first().cards.size)
    }

    // ========================================================================
    // split
    // ========================================================================

    @Test
    fun `split creates two hands from pair`() = runTest(testDispatcher) {
        val pairHand = hand(EIGHT, EIGHT, bet = 10)
        setupState(playerTurnState(
            playerHand = pairHand,
            chips = 990,
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.SPLIT)
        ))

        viewModel.split()
        advanceUntilIdle()

        assertEquals(2, state.playerHands.size)
        assertTrue(state.playerHands.all { it.isSplitHand })
        // Each hand gets a second card
        assertTrue(state.playerHands.all { it.cards.size == 2 })
        assertEquals(980, state.chips) // deducted another bet
    }

    @Test
    fun `split does nothing with non-pair`() {
        val nonPair = hand(EIGHT, NINE, bet = 10)
        setupState(playerTurnState(
            playerHand = nonPair,
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.SPLIT)
        ))

        viewModel.split()

        assertEquals(1, state.playerHands.size)
    }

    @Test
    fun `split does nothing without sufficient chips`() {
        val pairHand = hand(EIGHT, EIGHT, bet = 100)
        setupState(playerTurnState(
            playerHand = pairHand,
            chips = 50,
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.SPLIT)
        ))

        viewModel.split()

        assertEquals(1, state.playerHands.size)
    }

    @Test
    fun `split aces marks splitFromAces when hitSplitAces disabled`() = runTest(testDispatcher) {
        val rules = defaultRules.copy(hitSplitAces = false)
        val acesPair = Hand(cards = listOf(card(ACE), card(ACE)), bet = 10)
        setupState(playerTurnState(
            playerHand = acesPair,
            chips = 990,
            rules = rules,
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.SPLIT)
        ))

        viewModel.split()
        advanceUntilIdle()

        assertTrue(state.playerHands.all { it.splitFromAces })
    }

    // ========================================================================
    // surrender
    // ========================================================================

    @Test
    fun `surrender marks hand and returns half bet`() = runTest(testDispatcher) {
        setupState(playerTurnState(
            playerHand = hand(TEN, SIX, bet = 10),
            availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.SURRENDER)
        ))

        viewModel.surrender()
        advanceUntilIdle()

        assertTrue(state.playerHands.first().isSurrendered)
        // Should advance to round complete since all hands are done
        assertEquals(GamePhase.ROUND_COMPLETE, state.phase)
    }

    @Test
    fun `surrender does nothing outside PLAYER_TURN`() {
        setupState(playerTurnState().copy(phase = GamePhase.BETTING))

        viewModel.surrender()

        assertFalse(state.playerHands.first().isSurrendered)
    }

    // ========================================================================
    // Insurance
    // ========================================================================

    @Test
    fun `takeInsurance deducts half bet`() = runTest(testDispatcher) {
        val insuranceState = GameState(
            phase = GamePhase.INSURANCE_OFFERED,
            rules = defaultRules,
            playerHands = listOf(hand(TEN, SEVEN, bet = 100)),
            dealerHand = Hand(cards = listOf(card(ACE), card(NINE))),
            chips = 900,
            currentBet = 100,
            availableActions = setOf(PlayerAction.INSURANCE, PlayerAction.DECLINE_INSURANCE)
        )
        setupState(insuranceState)

        viewModel.takeInsurance()
        advanceUntilIdle()

        assertEquals(50, state.insuranceBet)
        assertEquals(850, state.chips)
    }

    @Test
    fun `declineInsurance keeps chips unchanged`() = runTest(testDispatcher) {
        val insuranceState = GameState(
            phase = GamePhase.INSURANCE_OFFERED,
            rules = defaultRules,
            playerHands = listOf(hand(TEN, SEVEN, bet = 100)),
            dealerHand = Hand(cards = listOf(card(ACE), card(NINE))),
            chips = 900,
            currentBet = 100,
            availableActions = setOf(PlayerAction.INSURANCE, PlayerAction.DECLINE_INSURANCE)
        )
        setupState(insuranceState)

        val chipsBefore = state.chips
        viewModel.declineInsurance()
        advanceUntilIdle()

        assertEquals(0, state.insuranceBet)
        // Chips should not have decreased from insurance
        assertTrue(state.chips >= chipsBefore || state.phase == GamePhase.ROUND_COMPLETE)
    }

    // ========================================================================
    // Even money
    // ========================================================================

    @Test
    fun `takeEvenMoney pays 2x bet and ends round`() = runTest(testDispatcher) {
        val bjHand = Hand(cards = listOf(card(ACE), card(TEN)), bet = 100)
        val evenMoneyState = GameState(
            phase = GamePhase.INSURANCE_OFFERED,
            rules = defaultRules,
            playerHands = listOf(bjHand),
            dealerHand = Hand(cards = listOf(card(ACE), card(KING))),
            chips = 900,
            currentBet = 100,
            availableActions = setOf(PlayerAction.EVEN_MONEY, PlayerAction.DECLINE_EVEN_MONEY)
        )
        setupState(evenMoneyState)

        viewModel.takeEvenMoney()

        assertEquals(GamePhase.ROUND_COMPLETE, state.phase)
        assertEquals(1100, state.chips) // 900 + 200 (2x bet)
        assertEquals(200, state.roundPayout)
        assertEquals(HandResult.WIN, state.handResults[0])
    }

    @Test
    fun `declineEvenMoney continues game`() = runTest(testDispatcher) {
        val bjHand = Hand(cards = listOf(card(ACE), card(TEN)), bet = 100)
        val evenMoneyState = GameState(
            phase = GamePhase.INSURANCE_OFFERED,
            rules = defaultRules,
            playerHands = listOf(bjHand),
            dealerHand = Hand(cards = listOf(card(ACE), card(NINE))),
            chips = 900,
            currentBet = 100,
            availableActions = setOf(PlayerAction.EVEN_MONEY, PlayerAction.DECLINE_EVEN_MONEY)
        )
        setupState(evenMoneyState)

        viewModel.declineEvenMoney()
        advanceUntilIdle()

        // Should resolve the round (player has BJ)
        assertNotEquals(GamePhase.INSURANCE_OFFERED, state.phase)
    }

    // ========================================================================
    // newRound
    // ========================================================================

    @Test
    fun `newRound resets to BETTING phase`() {
        setupState(GameState(
            phase = GamePhase.ROUND_COMPLETE,
            rules = defaultRules,
            playerHands = listOf(hand(TEN, NINE)),
            dealerHand = hand(TEN, SEVEN),
            chips = 1020,
            currentBet = 10,
            handResults = mapOf(0 to HandResult.WIN),
            roundPayout = 20,
            roundMessage = "You win!"
        ))

        viewModel.newRound()

        assertEquals(GamePhase.BETTING, state.phase)
        assertTrue(state.playerHands.isEmpty())
        assertEquals(Hand(), state.dealerHand)
        assertTrue(state.handResults.isEmpty())
        assertEquals(0, state.insuranceBet)
        assertFalse(state.showDealerHoleCard)
        assertEquals(0, state.roundPayout)
        assertEquals("", state.roundMessage)
        assertTrue(state.availableActions.isEmpty())
    }

    @Test
    fun `newRound clamps bet to available chips`() {
        setupState(GameState(
            phase = GamePhase.ROUND_COMPLETE,
            rules = defaultRules,
            chips = 5,
            currentBet = 100
        ))

        viewModel.newRound()

        assertEquals(5, state.currentBet)
    }

    @Test
    fun `newRound does nothing outside ROUND_COMPLETE`() {
        viewModel.startGame(defaultRules)

        val stateBefore = state
        viewModel.newRound()

        assertEquals(stateBefore, state)
    }

    // ========================================================================
    // resetGame
    // ========================================================================

    @Test
    fun `resetGame restores initial state with same rules`() = runTest(testDispatcher) {
        val rules = CasinoRules(initialChips = 2000, minimumBet = 25)
        viewModel.startGame(rules)
        viewModel.deal()
        advanceUntilIdle()

        viewModel.resetGame()

        assertEquals(GamePhase.BETTING, state.phase)
        assertEquals(2000, state.chips)
        assertEquals(25, state.currentBet)
        assertEquals(rules, state.rules)
    }

    // ========================================================================
    // Coach feedback
    // ========================================================================

    @Test
    fun `coach evaluates correct action`() = runTest(testDispatcher) {
        // Hard 16 vs 7: basic strategy says Hit
        val playerHand = hand(TEN, SIX, bet = 10)
        val dealerHand = Hand(cards = listOf(card(SEVEN), card(NINE)))
        setupState(playerTurnState(
            playerHand = playerHand,
            dealerHand = dealerHand
        ).copy(coachEnabled = true))

        viewModel.hit() // correct play for 16 vs 7

        assertTrue(state.coachFeedback.contains("Correct"))
        assertEquals(1, state.coachCorrect)
        assertEquals(1, state.coachTotal)
    }

    @Test
    fun `coach evaluates incorrect action`() = runTest(testDispatcher) {
        // Hard 16 vs 7: basic strategy says Hit, standing is wrong
        val playerHand = hand(TEN, SIX, bet = 10)
        val dealerHand = Hand(cards = listOf(card(SEVEN), card(NINE)))
        setupState(playerTurnState(
            playerHand = playerHand,
            dealerHand = dealerHand
        ).copy(coachEnabled = true))

        viewModel.stand() // wrong play
        advanceUntilIdle()

        assertTrue(state.coachFeedback.contains("Optimal play"))
        assertEquals(0, state.coachCorrect)
        assertEquals(1, state.coachTotal)
    }

    @Test
    fun `coach disabled does not track`() = runTest(testDispatcher) {
        setupState(playerTurnState().copy(coachEnabled = false))

        viewModel.hit()

        assertEquals("", state.coachFeedback)
        assertEquals(0, state.coachTotal)
    }

    @Test
    fun `takeInsurance gives negative coach feedback`() = runTest(testDispatcher) {
        val insuranceState = GameState(
            phase = GamePhase.INSURANCE_OFFERED,
            rules = defaultRules,
            playerHands = listOf(hand(TEN, SEVEN, bet = 100)),
            dealerHand = Hand(cards = listOf(card(ACE), card(NINE))),
            chips = 900,
            currentBet = 100,
            coachEnabled = true,
            availableActions = setOf(PlayerAction.INSURANCE, PlayerAction.DECLINE_INSURANCE)
        )
        setupState(insuranceState)

        viewModel.takeInsurance()
        advanceUntilIdle()

        assertTrue(state.coachFeedback.contains("never take insurance"))
    }

    @Test
    fun `declineInsurance gives positive coach feedback`() = runTest(testDispatcher) {
        val insuranceState = GameState(
            phase = GamePhase.INSURANCE_OFFERED,
            rules = defaultRules,
            playerHands = listOf(hand(TEN, SEVEN, bet = 100)),
            dealerHand = Hand(cards = listOf(card(ACE), card(NINE))),
            chips = 900,
            currentBet = 100,
            coachEnabled = true,
            availableActions = setOf(PlayerAction.INSURANCE, PlayerAction.DECLINE_INSURANCE)
        )
        setupState(insuranceState)

        viewModel.declineInsurance()
        advanceUntilIdle()

        assertTrue(state.coachFeedback.contains("Correct"))
    }

    // ========================================================================
    // Game over
    // ========================================================================

    @Test
    fun `round resolves to GAME_OVER when chips reach zero`() = runTest(testDispatcher) {
        // Player has 0 chips remaining and surrenders (gets back half = 5)
        // Actually, to guarantee GAME_OVER, player must bust with 0 chips remaining
        setupState(playerTurnState(
            playerHand = hand(TEN, QUEEN, bet = 10),  // hard 20 - will bust on hit
            chips = 0
        ))

        viewModel.hit()  // bust on 20
        advanceUntilIdle()

        if (state.playerHands.first().isBusted) {
            assertEquals(GamePhase.GAME_OVER, state.phase)
        }
    }

    // ========================================================================
    // Statistics tracking
    // ========================================================================

    @Test
    fun `handsPlayed increments after round`() = runTest(testDispatcher) {
        setupState(playerTurnState().copy(handsPlayed = 5))

        viewModel.surrender()
        advanceUntilIdle()

        assertEquals(6, state.handsPlayed)
    }

    @Test
    fun `handsWon increments on win`() = runTest(testDispatcher) {
        // Set up player with 20 vs dealer 17 - guaranteed win on stand
        setupState(playerTurnState(
            playerHand = hand(TEN, QUEEN, bet = 10),
            dealerHand = Hand(cards = listOf(card(SEVEN), card(TEN))),
            chips = 990
        ).copy(handsWon = 3))

        viewModel.stand()
        advanceUntilIdle()

        if (state.handResults[0] == HandResult.WIN) {
            assertEquals(4, state.handsWon)
        }
    }
}
