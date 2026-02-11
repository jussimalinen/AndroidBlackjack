package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.PlayerAction.*
import com.jmalinen.blackjack.model.Rank
import com.jmalinen.blackjack.model.Rank.*
import com.jmalinen.blackjack.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class DeviationAdvisorTest {

    private fun card(rank: Rank, suit: Suit = Suit.HEARTS) = Card(rank, suit)

    private fun hand(vararg ranks: Rank) = Hand(cards = ranks.map { card(it) })

    private val allActions = setOf(HIT, STAND, DOUBLE_DOWN, SPLIT, SURRENDER)
    private val hitOrStand = setOf(HIT, STAND)

    private val rules = CasinoRules()

    @Test
    fun `16 vs 10 hit at 2 cards if surrender available is surrender`() {
        val result = DeviationAdvisor.optimalAction(
            hand(Rank.TEN, SIX), card(TEN), allActions, rules,
            runningCount = 0, trueCount = 0f
        )
        assertEquals(SURRENDER, result.action)
        assertFalse(result.isDeviation)
    }

    @Test
    fun `16 vs 10 hit at zero running count with 2 cards if no surrender`() {
        val result = DeviationAdvisor.optimalAction(
            hand(Rank.TEN, SIX), card(TEN), hitOrStand, rules,
            runningCount = 0, trueCount = 0f
        )
        assertEquals(HIT, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `16 vs 10 hit at zero running count with 3+ cards`() {
        val result = DeviationAdvisor.optimalAction(
            hand(Rank.FIVE, SIX, Rank.FIVE), card(TEN), hitOrStand, rules,
            runningCount = 0, trueCount = 0f
        )
        assertEquals(HIT, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `16 vs 10 hit at negative running count with 2 cards if no surrender`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, SIX), card(TEN), hitOrStand, rules,
            runningCount = -1, trueCount = -0.5f
        )
        assertEquals(HIT, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `16 vs 10 hit at negative running count with 3+ cards`() {
        val result = DeviationAdvisor.optimalAction(
            hand(FIVE, SIX, Rank.FIVE), card(TEN), hitOrStand, rules,
            runningCount = -1, trueCount = -0.5f
        )
        assertEquals(HIT, result.action)
        assertTrue(result.isDeviation)
    }


    @Test
    fun `multi-card 16 vs 10 stand at positive count`() {
        val result = DeviationAdvisor.optimalAction(
            hand(SEVEN, FIVE, FOUR), card(TEN), hitOrStand, rules,
            runningCount = 2, trueCount = 1f
        )
        assertEquals(STAND, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `multi-card 16 vs 10 stand at positive count on two cards`() {
        val result = DeviationAdvisor.optimalAction(
            hand(SIX, TEN), card(TEN), hitOrStand, rules,
            runningCount = 2, trueCount = 1f
        )
        assertEquals(STAND, result.action)
        assertTrue(result.isDeviation)
    }

    // --- 12 vs 2: Stand at TC >= +3 ---

    @Test
    fun `12 vs 2 stand at true count 3`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, TWO), card(TWO), hitOrStand, rules,
            runningCount = 12, trueCount = 3f
        )
        assertEquals(STAND, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `12 vs 2 hit at true count below 3`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, TWO), card(TWO), hitOrStand, rules,
            runningCount = 5, trueCount = 2.5f
        )
        assertEquals(HIT, result.action)
        assertFalse(result.isDeviation)
    }

    // --- 12 vs 3: Stand at TC >= +2 ---

    @Test
    fun `12 vs 3 stand at true count 2`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, TWO), card(THREE), hitOrStand, rules,
            runningCount = 8, trueCount = 2f
        )
        assertEquals(STAND, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `12 vs 3 hit at true count below 2`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, TWO), card(THREE), hitOrStand, rules,
            runningCount = 3, trueCount = 1.5f
        )
        assertEquals(HIT, result.action)
        assertFalse(result.isDeviation)
    }

    // --- 12 vs 4: Hit at RC < 0 ---

    @Test
    fun `12 vs 4 hit at negative running count`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, TWO), card(FOUR), hitOrStand, rules,
            runningCount = -1, trueCount = -0.5f
        )
        assertEquals(HIT, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `12 vs 4 stand at zero running count`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, TWO), card(FOUR), hitOrStand, rules,
            runningCount = 0, trueCount = 0f
        )
        assertEquals(STAND, result.action)
        assertFalse(result.isDeviation)
    }

    // --- A,4 (soft 15) vs 4: Hit at RC < 0 ---

    @Test
    fun `soft 15 vs 4 hit at negative running count`() {
        val result = DeviationAdvisor.optimalAction(
            hand(ACE, FOUR), card(FOUR), allActions, rules,
            runningCount = -2, trueCount = -1f
        )
        assertEquals(HIT, result.action)
        assertTrue(result.isDeviation)
    }

    @Test
    fun `soft 15 vs 4 double at zero running count`() {
        val result = DeviationAdvisor.optimalAction(
            hand(ACE, FOUR), card(FOUR), allActions, rules,
            runningCount = 0, trueCount = 0f
        )
        assertEquals(DOUBLE_DOWN, result.action)
        assertFalse(result.isDeviation)
    }

    // --- Insurance deviation ---

    @Test
    fun `insurance deviation at true count 3`() {
        val result = DeviationAdvisor.insuranceDeviation(3f)
        assertNotNull(result)
        assertTrue(result!!.isDeviation)
        assertTrue(result.description!!.contains("insurance"))
    }

    @Test
    fun `insurance deviation at true count above 3`() {
        val result = DeviationAdvisor.insuranceDeviation(5f)
        assertNotNull(result)
        assertTrue(result!!.isDeviation)
    }

    @Test
    fun `no insurance deviation at true count below 3`() {
        val result = DeviationAdvisor.insuranceDeviation(2.9f)
        assertNull(result)
    }

    // --- No deviation: falls back to basic strategy ---

    @Test
    fun `no deviation returns basic strategy action`() {
        val result = DeviationAdvisor.optimalAction(
            hand(TEN, SEVEN), card(SIX), hitOrStand, rules,
            runningCount = 5, trueCount = 2f
        )
        assertEquals(STAND, result.action)
        assertFalse(result.isDeviation)
        assertNull(result.description)
    }
}
