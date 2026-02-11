package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.PlayerAction.*
import com.jmalinen.blackjack.model.Rank
import com.jmalinen.blackjack.model.Rank.*
import com.jmalinen.blackjack.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Test

class BasicStrategyAdvisorTest {

    // -- Helpers --

    private fun card(rank: Rank, suit: Suit = Suit.HEARTS) = Card(rank, suit)

    private fun hand(vararg ranks: Rank) = Hand(cards = ranks.map { card(it) })

    private val allActions = setOf(HIT, STAND, DOUBLE_DOWN, SPLIT, SURRENDER)
    private val noSurrender = setOf(HIT, STAND, DOUBLE_DOWN, SPLIT)
    private val noDouble = setOf(HIT, STAND, SPLIT, SURRENDER)
    private val hitOrStand = setOf(HIT, STAND)

    private val standardRules = CasinoRules()  // S17, peek, DAS

    private val h17Rules = CasinoRules(dealerStandsOnSoft17 = false)

    private val enhcRules = CasinoRules(dealerPeeks = false)

    private val noDasRules = CasinoRules(doubleAfterSplit = false)

    private fun advise(
        hand: Hand,
        dealerUpRank: Rank,
        available: Set<PlayerAction> = allActions,
        rules: CasinoRules = standardRules
    ): PlayerAction = BasicStrategyAdvisor.optimalAction(hand, card(dealerUpRank), available, rules)

    // ========================================================================
    // Hard totals
    // ========================================================================

    @Test
    fun `hard 5 through 8 always hit`() {
        val dealerRanks = listOf(TWO, FIVE, SEVEN, TEN, ACE)
        for (dealer in dealerRanks) {
            assertEquals("Hard 5 vs $dealer", HIT, advise(hand(TWO, THREE), dealer))
            assertEquals("Hard 6 vs $dealer", HIT, advise(hand(TWO, FOUR), dealer))
            assertEquals("Hard 7 vs $dealer", HIT, advise(hand(TWO, FIVE), dealer))
            assertEquals("Hard 8 vs $dealer", HIT, advise(hand(THREE, FIVE), dealer))
        }
    }

    @Test
    fun `hard 9 doubles vs 3 through 6 else hits`() {
        assertEquals(HIT, advise(hand(TWO, SEVEN), TWO))
        assertEquals(DOUBLE_DOWN, advise(hand(TWO, SEVEN), THREE))
        assertEquals(DOUBLE_DOWN, advise(hand(TWO, SEVEN), FOUR))
        assertEquals(DOUBLE_DOWN, advise(hand(TWO, SEVEN), FIVE))
        assertEquals(DOUBLE_DOWN, advise(hand(TWO, SEVEN), SIX))
        assertEquals(HIT, advise(hand(TWO, SEVEN), SEVEN))
        assertEquals(HIT, advise(hand(TWO, SEVEN), TEN))
        assertEquals(HIT, advise(hand(TWO, SEVEN), ACE))
    }

    @Test
    fun `hard 9 hits when double not available`() {
        assertEquals(HIT, advise(hand(TWO, SEVEN), FIVE, hitOrStand))
    }

    @Test
    fun `hard 10 doubles vs 2 through 9`() {
        for (dealer in listOf(TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE)) {
            assertEquals("10 vs $dealer", DOUBLE_DOWN, advise(hand(FOUR, SIX), dealer))
        }
        assertEquals(HIT, advise(hand(FOUR, SIX), TEN))
        assertEquals(HIT, advise(hand(FOUR, SIX), ACE))
    }

    @Test
    fun `hard 11 doubles vs 2 through 10 and hits vs A in S17`() {
        for (dealer in listOf(TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN)) {
            assertEquals("11 vs $dealer", DOUBLE_DOWN, advise(hand(THREE, EIGHT), dealer))
        }
        assertEquals(HIT, advise(hand(THREE, EIGHT), ACE))
    }

    @Test
    fun `hard 12 stands vs 4-6 hits otherwise`() {
        assertEquals(HIT, advise(hand(FOUR, EIGHT), TWO))
        assertEquals(HIT, advise(hand(FOUR, EIGHT), THREE))
        assertEquals(STAND, advise(hand(FOUR, EIGHT), FOUR))
        assertEquals(STAND, advise(hand(FOUR, EIGHT), FIVE))
        assertEquals(STAND, advise(hand(FOUR, EIGHT), SIX))
        assertEquals(HIT, advise(hand(FOUR, EIGHT), SEVEN))
        assertEquals(HIT, advise(hand(FOUR, EIGHT), TEN))
        assertEquals(HIT, advise(hand(FOUR, EIGHT), ACE))
    }

    @Test
    fun `hard 13 through 16 stands vs 2-6`() {
        for (score in listOf(
            hand(FOUR, NINE),   // 13
            hand(FIVE, NINE),   // 14
            hand(SIX, NINE),    // 15
            hand(SEVEN, NINE),  // 16
        )) {
            for (dealer in listOf(TWO, THREE, FOUR, FIVE, SIX)) {
                assertEquals("${score.score} vs $dealer", STAND, advise(score, dealer))
            }
        }
    }

    @Test
    fun `hard 13-14 hits vs 7 through A`() {
        for (dealer in listOf(SEVEN, EIGHT, NINE, TEN, ACE)) {
            assertEquals("13 vs $dealer", HIT, advise(hand(FOUR, NINE), dealer))
            assertEquals("14 vs $dealer", HIT, advise(hand(FIVE, NINE), dealer))
        }
    }

    @Test
    fun `hard 15 surrenders vs 10 else hits vs 7+`() {
        assertEquals(HIT, advise(hand(SIX, NINE), SEVEN))
        assertEquals(HIT, advise(hand(SIX, NINE), EIGHT))
        assertEquals(HIT, advise(hand(SIX, NINE), NINE))
        assertEquals(SURRENDER, advise(hand(SIX, NINE), TEN))
        assertEquals(HIT, advise(hand(SIX, NINE), ACE))
    }

    @Test
    fun `hard 15 hits vs 10 when surrender unavailable`() {
        assertEquals(HIT, advise(hand(SIX, NINE), TEN, noSurrender))
    }

    @Test
    fun `hard 16 surrenders vs 9 10 A else hits vs 7+`() {
        assertEquals(HIT, advise(hand(SEVEN, NINE), SEVEN))
        assertEquals(HIT, advise(hand(SEVEN, NINE), EIGHT))
        assertEquals(SURRENDER, advise(hand(SEVEN, NINE), NINE))
        assertEquals(SURRENDER, advise(hand(SEVEN, NINE), TEN))
        assertEquals(SURRENDER, advise(hand(SEVEN, NINE), ACE))
    }

    @Test
    fun `hard 17 through 20 always stands`() {
        val hand17 = hand(EIGHT, NINE)
        val hand18 = hand(EIGHT, TEN)
        val hand19 = hand(TEN, NINE)
        val hand20 = hand(TEN, QUEEN)
        for (dealer in listOf(TWO, FIVE, SEVEN, TEN, ACE)) {
            assertEquals(STAND, advise(hand17, dealer))
            assertEquals(STAND, advise(hand18, dealer))
            assertEquals(STAND, advise(hand19, dealer))
            assertEquals(STAND, advise(hand20, dealer))
        }
    }

    // ========================================================================
    // Soft totals
    // ========================================================================

    @Test
    fun `soft 13-14 doubles vs 5-6 else hits`() {
        for (h in listOf(hand(ACE, TWO), hand(ACE, THREE))) {
            assertEquals(HIT, advise(h, TWO))
            assertEquals(HIT, advise(h, THREE))
            assertEquals(HIT, advise(h, FOUR))
            assertEquals(DOUBLE_DOWN, advise(h, FIVE))
            assertEquals(DOUBLE_DOWN, advise(h, SIX))
            assertEquals(HIT, advise(h, SEVEN))
            assertEquals(HIT, advise(h, TEN))
            assertEquals(HIT, advise(h, ACE))
        }
    }

    @Test
    fun `soft 15-16 doubles vs 4-6 else hits`() {
        for (h in listOf(hand(ACE, FOUR), hand(ACE, FIVE))) {
            assertEquals(HIT, advise(h, TWO))
            assertEquals(HIT, advise(h, THREE))
            assertEquals(DOUBLE_DOWN, advise(h, FOUR))
            assertEquals(DOUBLE_DOWN, advise(h, FIVE))
            assertEquals(DOUBLE_DOWN, advise(h, SIX))
            assertEquals(HIT, advise(h, SEVEN))
            assertEquals(HIT, advise(h, TEN))
        }
    }

    @Test
    fun `soft 17 doubles vs 3-6 else hits`() {
        val h = hand(ACE, SIX)
        assertEquals(HIT, advise(h, TWO))
        assertEquals(DOUBLE_DOWN, advise(h, THREE))
        assertEquals(DOUBLE_DOWN, advise(h, FOUR))
        assertEquals(DOUBLE_DOWN, advise(h, FIVE))
        assertEquals(DOUBLE_DOWN, advise(h, SIX))
        assertEquals(HIT, advise(h, SEVEN))
        assertEquals(HIT, advise(h, TEN))
        assertEquals(HIT, advise(h, ACE))
    }

    @Test
    fun `soft 18 doubles vs 2-6 stands vs 7-8 hits vs 9+`() {
        val h = hand(ACE, SEVEN)
        assertEquals(DOUBLE_DOWN, advise(h, TWO))
        assertEquals(DOUBLE_DOWN, advise(h, THREE))
        assertEquals(DOUBLE_DOWN, advise(h, FOUR))
        assertEquals(DOUBLE_DOWN, advise(h, FIVE))
        assertEquals(DOUBLE_DOWN, advise(h, SIX))
        assertEquals(STAND, advise(h, SEVEN))
        assertEquals(STAND, advise(h, EIGHT))
        assertEquals(HIT, advise(h, NINE))
        assertEquals(HIT, advise(h, TEN))
        assertEquals(HIT, advise(h, ACE))
    }

    @Test
    fun `soft 18 stands when double not available vs 2-6`() {
        assertEquals(STAND, advise(hand(ACE, SEVEN), THREE, hitOrStand))
    }

    @Test
    fun `soft 19 doubles vs 6 else stands`() {
        val h = hand(ACE, EIGHT)
        assertEquals(STAND, advise(h, TWO))
        assertEquals(STAND, advise(h, THREE))
        assertEquals(STAND, advise(h, FOUR))
        assertEquals(DOUBLE_DOWN, advise(h, SIX))
        assertEquals(STAND, advise(h, SEVEN))
        assertEquals(STAND, advise(h, TEN))
        assertEquals(STAND, advise(h, ACE))
    }

    @Test
    fun `soft 19 stands vs 6 when double not available`() {
        assertEquals(STAND, advise(hand(ACE, EIGHT), SIX, hitOrStand))
    }

    @Test
    fun `soft 20 always stands`() {
        val h = hand(ACE, NINE)
        for (dealer in listOf(TWO, FIVE, SIX, SEVEN, TEN, ACE)) {
            assertEquals(STAND, advise(h, dealer))
        }
    }

    // ========================================================================
    // Pairs
    // ========================================================================

    @Test
    fun `always split aces`() {
        val h = hand(ACE, ACE)
        for (dealer in listOf(TWO, FIVE, SIX, SEVEN, TEN, ACE)) {
            assertEquals("AA vs $dealer", SPLIT, advise(h, dealer))
        }
    }

    @Test
    fun `always split eights in standard rules`() {
        val h = hand(EIGHT, EIGHT)
        for (dealer in listOf(TWO, FIVE, SIX, SEVEN, NINE, TEN, ACE)) {
            assertEquals("88 vs $dealer", SPLIT, advise(h, dealer))
        }
    }

    @Test
    fun `never split tens`() {
        for (tenRank in listOf(TEN, JACK, QUEEN, KING)) {
            val h = hand(tenRank, tenRank)
            for (dealer in listOf(TWO, SIX, TEN, ACE)) {
                assertEquals(STAND, advise(h, dealer))
            }
        }
    }

    @Test
    fun `fives are treated as hard 10 not pair`() {
        val h = hand(FIVE, FIVE)
        // Should double vs 2-9 (hard 10 strategy), not split
        assertEquals(DOUBLE_DOWN, advise(h, FIVE))
        assertEquals(DOUBLE_DOWN, advise(h, TWO))
        assertEquals(HIT, advise(h, TEN))
    }

    @Test
    fun `nines split vs 2-6 8-9 stand vs 7 10 A`() {
        val h = hand(NINE, NINE)
        assertEquals(SPLIT, advise(h, TWO))
        assertEquals(SPLIT, advise(h, THREE))
        assertEquals(SPLIT, advise(h, FOUR))
        assertEquals(SPLIT, advise(h, FIVE))
        assertEquals(SPLIT, advise(h, SIX))
        assertEquals(STAND, advise(h, SEVEN))
        assertEquals(SPLIT, advise(h, EIGHT))
        assertEquals(SPLIT, advise(h, NINE))
        assertEquals(STAND, advise(h, TEN))
        assertEquals(STAND, advise(h, ACE))
    }

    @Test
    fun `sevens split vs 2-7 hit vs 8+`() {
        val h = hand(SEVEN, SEVEN)
        for (dealer in listOf(TWO, THREE, FOUR, FIVE, SIX, SEVEN)) {
            assertEquals("77 vs $dealer", SPLIT, advise(h, dealer))
        }
        for (dealer in listOf(EIGHT, NINE, TEN, ACE)) {
            assertEquals("77 vs $dealer", HIT, advise(h, dealer))
        }
    }

    @Test
    fun `sixes with DAS split vs 2-6 hit otherwise`() {
        val h = hand(SIX, SIX)
        for (dealer in listOf(TWO, THREE, FOUR, FIVE, SIX)) {
            assertEquals("66 vs $dealer DAS", SPLIT, advise(h, dealer))
        }
        for (dealer in listOf(SEVEN, EIGHT, TEN, ACE)) {
            assertEquals("66 vs $dealer DAS", HIT, advise(h, dealer))
        }
    }

    @Test
    fun `sixes without DAS split vs 3-6 hit vs 2`() {
        val h = hand(SIX, SIX)
        assertEquals(HIT, advise(h, TWO, rules = noDasRules))
        for (dealer in listOf(THREE, FOUR, FIVE, SIX)) {
            assertEquals("66 vs $dealer no DAS", SPLIT, advise(h, dealer, rules = noDasRules))
        }
        assertEquals(HIT, advise(h, SEVEN, rules = noDasRules))
    }

    @Test
    fun `fours with DAS split vs 5-6 else hit`() {
        val h = hand(FOUR, FOUR)
        assertEquals(HIT, advise(h, TWO))
        assertEquals(HIT, advise(h, THREE))
        assertEquals(HIT, advise(h, FOUR))
        assertEquals(SPLIT, advise(h, FIVE))
        assertEquals(SPLIT, advise(h, SIX))
        assertEquals(HIT, advise(h, SEVEN))
    }

    @Test
    fun `fours without DAS always hit`() {
        val h = hand(FOUR, FOUR)
        for (dealer in listOf(TWO, FIVE, SIX, SEVEN, TEN)) {
            assertEquals(HIT, advise(h, dealer, rules = noDasRules))
        }
    }

    @Test
    fun `threes and twos with DAS split vs 2-7`() {
        for (rank in listOf(THREE, TWO)) {
            val h = hand(rank, rank)
            for (dealer in listOf(TWO, THREE, FOUR, FIVE, SIX, SEVEN)) {
                assertEquals("$rank$rank vs $dealer DAS", SPLIT, advise(h, dealer))
            }
            assertEquals(HIT, advise(h, EIGHT))
            assertEquals(HIT, advise(h, TEN))
        }
    }

    @Test
    fun `threes and twos without DAS split vs 4-7`() {
        for (rank in listOf(THREE, TWO)) {
            val h = hand(rank, rank)
            assertEquals(HIT, advise(h, TWO, rules = noDasRules))
            assertEquals(HIT, advise(h, THREE, rules = noDasRules))
            for (dealer in listOf(FOUR, FIVE, SIX, SEVEN)) {
                assertEquals(
                    "$rank$rank vs $dealer no DAS",
                    SPLIT,
                    advise(h, dealer, rules = noDasRules)
                )
            }
            assertEquals(HIT, advise(h, EIGHT, rules = noDasRules))
        }
    }

    // ========================================================================
    // H17 deviations
    // ========================================================================

    @Test
    fun `H17 double 11 vs A`() {
        assertEquals(DOUBLE_DOWN, advise(hand(THREE, EIGHT), ACE, rules = h17Rules))
    }

    @Test
    fun `H17 surrender 17 vs A`() {
        assertEquals(SURRENDER, advise(hand(EIGHT, NINE), ACE, rules = h17Rules))
    }

    @Test
    fun `H17 surrender 15 vs A`() {
        assertEquals(SURRENDER, advise(hand(SIX, NINE), ACE, rules = h17Rules))
    }

    @Test
    fun `H17 surrender 8-8 vs A`() {
        assertEquals(SURRENDER, advise(hand(EIGHT, EIGHT), ACE, rules = h17Rules))
    }

    @Test
    fun `H17 8-8 vs A splits when surrender unavailable`() {
        assertEquals(SPLIT, advise(hand(EIGHT, EIGHT), ACE, noSurrender, h17Rules))
    }

    // ========================================================================
    // ENHC (no hole card) deviations
    // ========================================================================

    @Test
    fun `ENHC hit 11 vs 10 instead of double`() {
        assertEquals(HIT, advise(hand(THREE, EIGHT), TEN, rules = enhcRules))
    }

    @Test
    fun `ENHC surrender 14 vs 10`() {
        assertEquals(SURRENDER, advise(hand(FIVE, NINE), TEN, rules = enhcRules))
    }

    @Test
    fun `ENHC multi-card 16 vs 10 stands`() {
        val multiCard16 = hand(FOUR, SIX, SIX)  // 16 with 3 cards
        assertEquals(STAND, advise(multiCard16, TEN, rules = enhcRules))
    }

    @Test
    fun `ENHC two-card 16 vs 10 surrenders`() {
        // 2-card hard 16 vs 10 should still surrender in ENHC
        assertEquals(SURRENDER, advise(hand(SEVEN, NINE), TEN, rules = enhcRules))
    }

    @Test
    fun `ENHC hit 16 vs A instead of surrender`() {
        assertEquals(HIT, advise(hand(SEVEN, NINE), ACE, rules = enhcRules))
    }

    @Test
    fun `ENHC hit A-A vs A instead of split`() {
        assertEquals(HIT, advise(hand(ACE, ACE), ACE, rules = enhcRules))
    }

    @Test
    fun `ENHC surrender 8-8 vs 10`() {
        assertEquals(SURRENDER, advise(hand(EIGHT, EIGHT), TEN, rules = enhcRules))
    }

    @Test
    fun `ENHC 8-8 vs 10 hits when surrender unavailable`() {
        assertEquals(HIT, advise(hand(EIGHT, EIGHT), TEN, noSurrender, enhcRules))
    }

    @Test
    fun `ENHC hit 8-8 vs A`() {
        assertEquals(HIT, advise(hand(EIGHT, EIGHT), ACE, rules = enhcRules))
    }

    @Test
    fun `ENHC soft 18 vs 2 stands instead of double`() {
        assertEquals(STAND, advise(hand(ACE, SEVEN), TWO, rules = enhcRules))
    }

    @Test
    fun `ENHC soft 19 vs 6 stands instead of double`() {
        assertEquals(STAND, advise(hand(ACE, EIGHT), SIX, rules = enhcRules))
    }

    // ========================================================================
    // Action resolution and fallback
    // ========================================================================

    @Test
    fun `returns STAND when no actions available`() {
        assertEquals(STAND, advise(hand(TEN, SEVEN), FIVE, emptySet()))
    }

    @Test
    fun `double falls back to hit when unavailable`() {
        // Hard 10 vs 5 wants to double; with no double, should hit
        assertEquals(HIT, advise(hand(FOUR, SIX), FIVE, hitOrStand))
    }

    @Test
    fun `Ds falls back to stand when double unavailable`() {
        // Soft 18 vs 3 wants to Ds
        assertEquals(STAND, advise(hand(ACE, SEVEN), THREE, hitOrStand))
    }

    @Test
    fun `surrender falls back to hit for Rh`() {
        // Hard 16 vs 9 wants Rh
        assertEquals(HIT, advise(hand(SEVEN, NINE), NINE, noSurrender))
    }

    @Test
    fun `split falls back to hit when unavailable`() {
        // 7,7 vs 3 wants to split; no split available should hit
        val noSplit = setOf(HIT, STAND, DOUBLE_DOWN, SURRENDER)
        assertEquals(HIT, advise(hand(SEVEN, SEVEN), THREE, noSplit))
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Test
    fun `hard total below 5 hits`() {
        // Hard 4 (shouldn't normally occur but handled)
        val lowHand = hand(TWO, TWO)  // This is a pair, but if we force non-pair path:
        // Actually 2+2 is treated as pair; let's test score <=4 via 3-card hand
        val threeCardFour = Hand(cards = listOf(card(TWO), card(TWO))) // pair so will split
        // Use a scenario where pairs are not available
        val noSplit = setOf(HIT, STAND)
        assertEquals(HIT, advise(threeCardFour, FIVE, noSplit))
    }

    @Test
    fun `hard 21 or above stands`() {
        val hand21 = hand(SEVEN, SEVEN, SEVEN)
        assertEquals(STAND, advise(hand21, FIVE, hitOrStand))
    }

    @Test
    fun `face cards mapped to ten column for dealer upcard`() {
        // Dealer J/Q/K should behave same as 10
        val h = hand(SIX, NINE) // hard 15
        assertEquals(advise(h, TEN), advise(h, JACK))
        assertEquals(advise(h, TEN), advise(h, QUEEN))
        assertEquals(advise(h, TEN), advise(h, KING))
    }

    @Test
    fun `mixed ten-value pair treated as tens`() {
        // J+Q should be treated as pair of tens -> stand
        val h = hand(JACK, QUEEN)
        assertEquals(STAND, advise(h, SIX))
    }
}
