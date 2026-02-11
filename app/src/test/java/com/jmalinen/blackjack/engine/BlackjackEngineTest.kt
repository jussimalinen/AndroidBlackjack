package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.PlayerAction.*
import com.jmalinen.blackjack.model.Rank
import com.jmalinen.blackjack.model.Rank.*
import com.jmalinen.blackjack.model.Suit
import com.jmalinen.blackjack.model.SurrenderPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackjackEngineTest {

    // -- Helpers --

    private fun card(rank: Rank, suit: Suit = Suit.HEARTS) = Card(rank, suit)

    private fun hand(
        vararg ranks: Rank,
        bet: Int = 10,
        isSplitHand: Boolean = false,
        splitFromAces: Boolean = false
    ) = Hand(
        cards = ranks.map { card(it) },
        bet = bet,
        isSplitHand = isSplitHand,
        splitFromAces = splitFromAces
    )

    private val defaultRules = CasinoRules()
    private val dealerUpCard = card(TEN)

    private fun actions(
        hand: Hand,
        playerHands: List<Hand> = listOf(hand),
        dealerUpCard: Card = this.dealerUpCard,
        chips: Int = 1000,
        rules: CasinoRules = defaultRules
    ) = BlackjackEngine.availableActions(hand, playerHands, dealerUpCard, chips, rules)

    // ========================================================================
    // Basic actions: hit and stand
    // ========================================================================

    @Test
    fun `basic hand has hit and stand`() {
        val result = actions(hand(TEN, FIVE))
        assertTrue(HIT in result)
        assertTrue(STAND in result)
    }

    @Test
    fun `finished hand has no actions`() {
        val standing = hand(TEN, SEVEN).copy(isStanding = true)
        assertTrue(actions(standing).isEmpty())
    }

    @Test
    fun `busted hand has no actions`() {
        val busted = hand(TEN, EIGHT, FIVE)
        assertTrue(actions(busted).isEmpty())
    }

    @Test
    fun `blackjack hand has no actions`() {
        val bj = hand(ACE, TEN)
        assertTrue(actions(bj).isEmpty())
    }

    // ========================================================================
    // Double down
    // ========================================================================

    @Test
    fun `two-card hand can double down`() {
        val result = actions(hand(FIVE, SIX))
        assertTrue(DOUBLE_DOWN in result)
    }

    @Test
    fun `three-card hand cannot double down`() {
        val result = actions(hand(THREE, FOUR, FIVE))
        assertFalse(DOUBLE_DOWN in result)
    }

    @Test
    fun `cannot double down without sufficient chips`() {
        val h = hand(FIVE, SIX, bet = 100)
        val result = actions(h, chips = 50)
        assertFalse(DOUBLE_DOWN in result)
    }

    @Test
    fun `can double down with exact chips`() {
        val h = hand(FIVE, SIX, bet = 100)
        val result = actions(h, chips = 100)
        assertTrue(DOUBLE_DOWN in result)
    }

    @Test
    fun `cannot double after split when DAS disabled`() {
        val rules = CasinoRules(doubleAfterSplit = false)
        val h = hand(FIVE, SIX, isSplitHand = true)
        val result = actions(h, rules = rules)
        assertFalse(DOUBLE_DOWN in result)
    }

    @Test
    fun `can double after split when DAS enabled`() {
        val h = hand(FIVE, SIX, isSplitHand = true)
        val result = actions(h)
        assertTrue(DOUBLE_DOWN in result)
    }

    @Test
    fun `cannot double when doubleOnAnyTwo disabled`() {
        val rules = CasinoRules(doubleOnAnyTwo = false)
        val h = hand(FIVE, SIX)
        val result = actions(h, rules = rules)
        assertFalse(DOUBLE_DOWN in result)
    }

    // ========================================================================
    // Split
    // ========================================================================

    @Test
    fun `pair can split`() {
        val h = hand(EIGHT, EIGHT)
        val result = actions(h)
        assertTrue(SPLIT in result)
    }

    @Test
    fun `non-pair cannot split`() {
        val h = hand(EIGHT, NINE)
        val result = actions(h)
        assertFalse(SPLIT in result)
    }

    @Test
    fun `cannot split at max split hands`() {
        val h = hand(EIGHT, EIGHT)
        val rules = CasinoRules(maxSplitHands = 2)
        // Already have 2 hands
        val allHands = listOf(h, hand(TEN, FIVE))
        val result = actions(h, playerHands = allHands, rules = rules)
        assertFalse(SPLIT in result)
    }

    @Test
    fun `can split when under max split hands`() {
        val h = hand(EIGHT, EIGHT)
        val rules = CasinoRules(maxSplitHands = 4)
        val allHands = listOf(h, hand(TEN, FIVE))
        val result = actions(h, playerHands = allHands, rules = rules)
        assertTrue(SPLIT in result)
    }

    @Test
    fun `cannot split without sufficient chips`() {
        val h = hand(EIGHT, EIGHT, bet = 100)
        val result = actions(h, chips = 50)
        assertFalse(SPLIT in result)
    }

    @Test
    fun `cannot resplit aces when rule disabled`() {
        val rules = CasinoRules(resplitAces = false)
        val h = hand(ACE, ACE, isSplitHand = true)
        val allHands = listOf(h, hand(ACE, TEN, isSplitHand = true))
        val result = actions(h, playerHands = allHands, rules = rules)
        assertFalse(SPLIT in result)
    }

    @Test
    fun `can resplit aces when rule enabled`() {
        val rules = CasinoRules(resplitAces = true)
        val h = hand(ACE, ACE, isSplitHand = true)
        val allHands = listOf(h, hand(ACE, TEN, isSplitHand = true))
        val result = actions(h, playerHands = allHands, rules = rules)
        assertTrue(SPLIT in result)
    }

    @Test
    fun `non-ace pair on split hand can resplit`() {
        val h = hand(EIGHT, EIGHT, isSplitHand = true)
        val allHands = listOf(h, hand(EIGHT, TEN, isSplitHand = true))
        val result = actions(h, playerHands = allHands)
        assertTrue(SPLIT in result)
    }

    // ========================================================================
    // Split aces restrictions
    // ========================================================================

    @Test
    fun `split aces can only stand when hitSplitAces disabled`() {
        val rules = CasinoRules(hitSplitAces = false)
        // 1-card hand: ace just dealt from split, awaiting second card
        val h = Hand(cards = listOf(card(ACE)), bet = 10, splitFromAces = true)
        val result = actions(h, rules = rules)
        assertEquals(setOf(STAND), result)
    }

    @Test
    fun `split aces can hit when hitSplitAces enabled`() {
        val rules = CasinoRules(hitSplitAces = true)
        // 1-card hand: the splitFromAces && !hitSplitAces check is skipped
        val h = Hand(cards = listOf(card(ACE)), bet = 10, splitFromAces = true)
        val result = actions(h, rules = rules)
        assertTrue(HIT in result)
        assertTrue(STAND in result)
    }

    @Test
    fun `split aces with two cards is finished`() {
        // Once the second card arrives, splitFromAces hands are finished
        val h = Hand(
            cards = listOf(card(ACE), card(FIVE)),
            bet = 10,
            splitFromAces = true
        )
        assertTrue(actions(h).isEmpty())
    }

    // ========================================================================
    // Surrender
    // ========================================================================

    @Test
    fun `late surrender available on initial two-card hand`() {
        val rules = CasinoRules(surrenderPolicy = SurrenderPolicy.LATE)
        val h = hand(TEN, SIX)
        val result = actions(h, rules = rules)
        assertTrue(SURRENDER in result)
    }

    @Test
    fun `early surrender available on initial two-card hand`() {
        val rules = CasinoRules(surrenderPolicy = SurrenderPolicy.EARLY)
        val h = hand(TEN, SIX)
        val result = actions(h, rules = rules)
        assertTrue(SURRENDER in result)
    }

    @Test
    fun `no surrender when policy is none`() {
        val result = actions(hand(TEN, SIX))
        assertFalse(SURRENDER in result)
    }

    @Test
    fun `cannot surrender after hit`() {
        val rules = CasinoRules(surrenderPolicy = SurrenderPolicy.LATE)
        val h = hand(FOUR, FIVE, SEVEN)  // 3 cards
        val result = actions(h, rules = rules)
        assertFalse(SURRENDER in result)
    }

    @Test
    fun `cannot surrender on split hand`() {
        val rules = CasinoRules(surrenderPolicy = SurrenderPolicy.LATE)
        val h = hand(TEN, SIX, isSplitHand = true)
        val allHands = listOf(h, hand(TEN, FIVE, isSplitHand = true))
        val result = actions(h, playerHands = allHands, rules = rules)
        assertFalse(SURRENDER in result)
    }

    @Test
    fun `cannot surrender when multiple hands exist`() {
        val rules = CasinoRules(surrenderPolicy = SurrenderPolicy.LATE)
        val h = hand(TEN, SIX)
        val allHands = listOf(h, hand(TEN, FIVE))
        val result = actions(h, playerHands = allHands, rules = rules)
        assertFalse(SURRENDER in result)
    }

    // ========================================================================
    // Insurance actions
    // ========================================================================

    @Test
    fun `player with blackjack gets even money option`() {
        val bj = hand(ACE, TEN)
        val result = BlackjackEngine.insuranceActions(bj, chips = 1000)
        assertTrue(EVEN_MONEY in result)
        assertTrue(DECLINE_EVEN_MONEY in result)
        assertFalse(INSURANCE in result)
    }

    @Test
    fun `player without blackjack gets insurance option`() {
        val h = hand(TEN, SEVEN)
        val result = BlackjackEngine.insuranceActions(h, chips = 1000)
        assertTrue(INSURANCE in result)
        assertTrue(DECLINE_INSURANCE in result)
        assertFalse(EVEN_MONEY in result)
    }

    @Test
    fun `cannot take insurance without sufficient chips`() {
        val h = hand(TEN, SEVEN, bet = 100)
        val result = BlackjackEngine.insuranceActions(h, chips = 40)  // need 50
        assertFalse(INSURANCE in result)
        assertTrue(DECLINE_INSURANCE in result)
    }

    @Test
    fun `can take insurance with exact chips`() {
        val h = hand(TEN, SEVEN, bet = 100)
        val result = BlackjackEngine.insuranceActions(h, chips = 50)
        assertTrue(INSURANCE in result)
    }

    // ========================================================================
    // Combined scenarios
    // ========================================================================

    @Test
    fun `initial pair has all standard actions with late surrender`() {
        val rules = CasinoRules(surrenderPolicy = SurrenderPolicy.LATE)
        val h = hand(EIGHT, EIGHT)
        val result = actions(h, rules = rules)
        assertEquals(setOf(HIT, STAND, DOUBLE_DOWN, SPLIT, SURRENDER), result)
    }

    @Test
    fun `three-card non-pair hand has only hit and stand`() {
        val h = hand(THREE, FOUR, FIVE)
        val result = actions(h)
        assertEquals(setOf(HIT, STAND), result)
    }
}
