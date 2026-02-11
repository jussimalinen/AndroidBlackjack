package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.BlackjackPayout
import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.model.Rank
import com.jmalinen.blackjack.model.Rank.*
import com.jmalinen.blackjack.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Test

class PayoutCalculatorTest {

    // -- Helpers --

    private fun card(rank: Rank, suit: Suit = Suit.HEARTS) = Card(rank, suit)

    private fun hand(vararg ranks: Rank, bet: Int = 10) =
        Hand(cards = ranks.map { card(it) }, bet = bet)

    private fun blackjackHand(bet: Int = 10) = hand(ACE, TEN, bet = bet)

    private fun bustedHand(bet: Int = 10) = hand(TEN, EIGHT, FIVE, bet = bet)

    private fun surrenderedHand(bet: Int = 10) =
        Hand(cards = listOf(card(TEN), card(SIX)), bet = bet, isSurrendered = true)

    private val defaultRules = CasinoRules()

    private fun calculate(
        playerHands: List<Hand>,
        dealerHand: Hand,
        insuranceBet: Int = 0,
        rules: CasinoRules = defaultRules
    ) = PayoutCalculator.calculateResults(playerHands, dealerHand, insuranceBet, rules)

    // ========================================================================
    // Basic win/lose/push
    // ========================================================================

    @Test
    fun `player wins when score is higher than dealer`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, NINE, bet = 10)),  // 19
            dealerHand = hand(TEN, SEVEN)                      // 17
        )
        assertEquals(HandResult.WIN, result.handResults[0])
        assertEquals(20, result.totalPayout)  // bet * 2
    }

    @Test
    fun `player loses when score is lower than dealer`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, SEVEN, bet = 10)),  // 17
            dealerHand = hand(TEN, NINE)                        // 19
        )
        assertEquals(HandResult.LOSE, result.handResults[0])
        assertEquals(0, result.totalPayout)
    }

    @Test
    fun `push when scores are equal`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, EIGHT, bet = 10)),  // 18
            dealerHand = hand(NINE, NINE)                       // 18
        )
        assertEquals(HandResult.PUSH, result.handResults[0])
        assertEquals(10, result.totalPayout)  // bet returned
    }

    @Test
    fun `player wins when dealer busts`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, SEVEN, bet = 10)),  // 17
            dealerHand = hand(TEN, SIX, EIGHT)                  // 24 busted
        )
        assertEquals(HandResult.WIN, result.handResults[0])
        assertEquals(20, result.totalPayout)
    }

    // ========================================================================
    // Bust
    // ========================================================================

    @Test
    fun `player bust loses regardless of dealer`() {
        val result = calculate(
            playerHands = listOf(bustedHand(bet = 10)),
            dealerHand = hand(TEN, SIX, EIGHT)  // dealer also busts
        )
        assertEquals(HandResult.BUST, result.handResults[0])
        assertEquals(0, result.totalPayout)
    }

    // ========================================================================
    // Blackjack
    // ========================================================================

    @Test
    fun `blackjack pays 3 to 2 by default`() {
        val result = calculate(
            playerHands = listOf(blackjackHand(bet = 10)),
            dealerHand = hand(TEN, SEVEN)
        )
        assertEquals(HandResult.BLACKJACK, result.handResults[0])
        assertEquals(25, result.totalPayout)  // 10 + 10*1.5 = 25
    }

    @Test
    fun `blackjack pays 6 to 5 with rule`() {
        val rules = CasinoRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE)
        val result = calculate(
            playerHands = listOf(blackjackHand(bet = 10)),
            dealerHand = hand(TEN, SEVEN),
            rules = rules
        )
        assertEquals(HandResult.BLACKJACK, result.handResults[0])
        assertEquals(22, result.totalPayout)  // 10 + 10*1.2 = 22
    }

    @Test
    fun `blackjack vs blackjack is push`() {
        val result = calculate(
            playerHands = listOf(blackjackHand(bet = 10)),
            dealerHand = blackjackHand()
        )
        assertEquals(HandResult.PUSH, result.handResults[0])
        assertEquals(10, result.totalPayout)
    }

    @Test
    fun `non-blackjack 21 loses to dealer blackjack`() {
        val threeCard21 = hand(SEVEN, SEVEN, SEVEN, bet = 10)
        val result = calculate(
            playerHands = listOf(threeCard21),
            dealerHand = blackjackHand()
        )
        assertEquals(HandResult.LOSE, result.handResults[0])
        assertEquals(0, result.totalPayout)
    }

    // ========================================================================
    // Surrender
    // ========================================================================

    @Test
    fun `surrender returns half the bet`() {
        val result = calculate(
            playerHands = listOf(surrenderedHand(bet = 10)),
            dealerHand = hand(TEN, SEVEN)
        )
        assertEquals(HandResult.SURRENDER, result.handResults[0])
        assertEquals(5, result.totalPayout)
    }

    @Test
    fun `surrender takes priority over other outcomes`() {
        // Even if dealer busts, surrender result stays
        val result = calculate(
            playerHands = listOf(surrenderedHand(bet = 20)),
            dealerHand = hand(TEN, SIX, EIGHT)  // dealer busts
        )
        assertEquals(HandResult.SURRENDER, result.handResults[0])
        assertEquals(10, result.totalPayout)
    }

    // ========================================================================
    // Insurance
    // ========================================================================

    @Test
    fun `insurance pays 3x when dealer has blackjack`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, SEVEN, bet = 10)),
            dealerHand = blackjackHand(),
            insuranceBet = 5
        )
        assertEquals(15, result.insurancePayout)  // 5 * 3
    }

    @Test
    fun `insurance pays nothing when dealer has no blackjack`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, SEVEN, bet = 10)),
            dealerHand = hand(ACE, SIX),
            insuranceBet = 5
        )
        assertEquals(0, result.insurancePayout)
    }

    @Test
    fun `insurance payout included in total`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, SEVEN, bet = 10)),
            dealerHand = blackjackHand(),
            insuranceBet = 5
        )
        // Player loses hand (0) + insurance pays (15) = 15
        assertEquals(HandResult.LOSE, result.handResults[0])
        assertEquals(15, result.totalPayout)
    }

    @Test
    fun `no insurance bet means zero insurance payout`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, SEVEN, bet = 10)),
            dealerHand = blackjackHand(),
            insuranceBet = 0
        )
        assertEquals(0, result.insurancePayout)
    }

    // ========================================================================
    // Three sevens bonus
    // ========================================================================

    @Test
    fun `three sevens pays 3 to 1 when rule enabled`() {
        val rules = CasinoRules(threeSevensPays3to1 = true)
        val result = calculate(
            playerHands = listOf(hand(SEVEN, SEVEN, SEVEN, bet = 10)),
            dealerHand = hand(TEN, SEVEN),
            rules = rules
        )
        assertEquals(HandResult.THREE_SEVENS, result.handResults[0])
        assertEquals(40, result.totalPayout)  // bet * 4
    }

    @Test
    fun `three sevens treated as normal win when rule disabled`() {
        val result = calculate(
            playerHands = listOf(hand(SEVEN, SEVEN, SEVEN, bet = 10)),  // 21
            dealerHand = hand(TEN, SEVEN)                                // 17
        )
        assertEquals(HandResult.WIN, result.handResults[0])
        assertEquals(20, result.totalPayout)
    }

    @Test
    fun `three sevens takes priority over dealer blackjack`() {
        val rules = CasinoRules(threeSevensPays3to1 = true)
        val result = calculate(
            playerHands = listOf(hand(SEVEN, SEVEN, SEVEN, bet = 10)),
            dealerHand = blackjackHand(),
            rules = rules
        )
        assertEquals(HandResult.THREE_SEVENS, result.handResults[0])
        assertEquals(40, result.totalPayout)
    }

    // ========================================================================
    // Multiple hands (split scenarios)
    // ========================================================================

    @Test
    fun `multiple hands each calculated independently`() {
        val result = calculate(
            playerHands = listOf(
                hand(TEN, NINE, bet = 10),     // 19 - wins vs 17
                hand(TEN, FIVE, bet = 10),     // 15 - loses vs 17
            ),
            dealerHand = hand(TEN, SEVEN)       // 17
        )
        assertEquals(HandResult.WIN, result.handResults[0])
        assertEquals(HandResult.LOSE, result.handResults[1])
        assertEquals(20, result.totalPayout)  // 20 + 0
    }

    @Test
    fun `multiple hands with mixed results sum correctly`() {
        val result = calculate(
            playerHands = listOf(
                hand(TEN, NINE, bet = 10),     // 19 - wins
                hand(TEN, EIGHT, bet = 10),    // 18 - push
                bustedHand(bet = 10),           // bust - loses
            ),
            dealerHand = hand(TEN, EIGHT)       // 18
        )
        assertEquals(HandResult.WIN, result.handResults[0])
        assertEquals(HandResult.PUSH, result.handResults[1])
        assertEquals(HandResult.BUST, result.handResults[2])
        assertEquals(30, result.totalPayout)  // 20 + 10 + 0
    }

    // ========================================================================
    // Bet amounts
    // ========================================================================

    @Test
    fun `payout scales with bet size`() {
        val result = calculate(
            playerHands = listOf(hand(TEN, NINE, bet = 100)),
            dealerHand = hand(TEN, SEVEN)
        )
        assertEquals(200, result.totalPayout)
    }

    @Test
    fun `blackjack payout scales with bet size`() {
        val result = calculate(
            playerHands = listOf(blackjackHand(bet = 100)),
            dealerHand = hand(TEN, SEVEN)
        )
        assertEquals(250, result.totalPayout)  // 100 + 100*1.5
    }

    @Test
    fun `doubled down hand pays double the doubled bet`() {
        val doubledHand = Hand(
            cards = listOf(card(FIVE), card(THREE), card(TEN)),  // 18
            bet = 20,  // doubled from 10
            isDoubledDown = true
        )
        val result = calculate(
            playerHands = listOf(doubledHand),
            dealerHand = hand(TEN, SEVEN)  // 17
        )
        assertEquals(HandResult.WIN, result.handResults[0])
        assertEquals(40, result.totalPayout)  // 20 * 2
    }
}
