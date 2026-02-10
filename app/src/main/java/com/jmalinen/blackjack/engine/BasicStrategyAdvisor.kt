package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.Rank

/**
 * Standard basic strategy advisor for 4-8 deck blackjack.
 * Each table cell encodes the ideal action with fallback:
 *   H = Hit, S = Stand, D = Double (else Hit), Ds = Double (else Stand),
 *   P = Split, Ph = Split (else Hit), Pd = Split (with DAS, else Hit),
 *   Rh = Surrender (else Hit), Rs = Surrender (else Stand), Rp = Surrender (else Split)
 */
object BasicStrategyAdvisor {

    fun optimalAction(
        hand: Hand,
        dealerUpCard: Card,
        availableActions: Set<PlayerAction>,
        rules: CasinoRules
    ): PlayerAction {
        if (availableActions.isEmpty()) return PlayerAction.STAND

        val dealerIndex = dealerColumnIndex(dealerUpCard.rank)

        // Pairs (only if split is theoretically possible)
        if (hand.isPair && hand.cards.size == 2) {
            val pairAction = pairStrategy(hand.cards.first().rank, dealerIndex, rules)
            if (pairAction != null) {
                val resolved = resolveAction(pairAction, availableActions)
                if (resolved != null) return resolved
            }
        }

        // Soft totals (ace counted as 11)
        if (hand.isSoft && hand.score in 13..20) {
            val cell = softStrategy(hand.score, dealerIndex, rules)
            val resolved = resolveAction(cell, availableActions)
            if (resolved != null) return resolved
        }

        // Hard totals
        val cell = hardStrategy(hand.score, dealerIndex, hand.cards.size, rules)
        return resolveAction(cell, availableActions) ?: PlayerAction.STAND
    }

    // -- Column mapping: dealer upcard rank -> index 0-9 (2,3,4,5,6,7,8,9,T,A) --

    private fun dealerColumnIndex(rank: Rank): Int = when (rank) {
        Rank.TWO -> 0
        Rank.THREE -> 1
        Rank.FOUR -> 2
        Rank.FIVE -> 3
        Rank.SIX -> 4
        Rank.SEVEN -> 5
        Rank.EIGHT -> 6
        Rank.NINE -> 7
        Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> 8
        Rank.ACE -> 9
    }

    // -- Action codes --

    private enum class Cell {
        H,   // Hit
        S,   // Stand
        D,   // Double else Hit
        Ds,  // Double else Stand
        P,   // Split
        Ph,  // Split else Hit
        Pd,  // Split if DAS else Hit
        Rh,  // Surrender else Hit
        Rs,  // Surrender else Stand
        Rp,  // Surrender else Split
    }

    private fun resolveAction(cell: Cell, available: Set<PlayerAction>): PlayerAction? {
        return when (cell) {
            Cell.H -> pickIfAvailable(PlayerAction.HIT, available)
            Cell.S -> pickIfAvailable(PlayerAction.STAND, available)
            Cell.D -> if (PlayerAction.DOUBLE_DOWN in available) PlayerAction.DOUBLE_DOWN
                      else pickIfAvailable(PlayerAction.HIT, available)
            Cell.Ds -> if (PlayerAction.DOUBLE_DOWN in available) PlayerAction.DOUBLE_DOWN
                       else pickIfAvailable(PlayerAction.STAND, available)
            Cell.P -> pickIfAvailable(PlayerAction.SPLIT, available)
                      ?: pickIfAvailable(PlayerAction.HIT, available)
            Cell.Ph -> if (PlayerAction.SPLIT in available) PlayerAction.SPLIT
                       else pickIfAvailable(PlayerAction.HIT, available)
            Cell.Pd -> if (PlayerAction.SPLIT in available) PlayerAction.SPLIT
                       else pickIfAvailable(PlayerAction.HIT, available)
            Cell.Rh -> if (PlayerAction.SURRENDER in available) PlayerAction.SURRENDER
                       else pickIfAvailable(PlayerAction.HIT, available)
            Cell.Rs -> if (PlayerAction.SURRENDER in available) PlayerAction.SURRENDER
                       else pickIfAvailable(PlayerAction.STAND, available)
            Cell.Rp -> if (PlayerAction.SURRENDER in available) PlayerAction.SURRENDER
                       else if (PlayerAction.SPLIT in available) PlayerAction.SPLIT
                       else pickIfAvailable(PlayerAction.HIT, available)
        }
    }

    private fun pickIfAvailable(action: PlayerAction, available: Set<PlayerAction>): PlayerAction? {
        return if (action in available) action else null
    }

    // -- Hard totals strategy (rows: score 5-20, cols: dealer 2-A) --

    //                                    2     3     4     5     6     7     8     9     T     A
    private val hardTable: Map<Int, Array<Cell>> = mapOf(
        5  to arrayOf(Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        6  to arrayOf(Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        7  to arrayOf(Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        8  to arrayOf(Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        9  to arrayOf(Cell.H,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        10 to arrayOf(Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.H,  Cell.H),
        11 to arrayOf(Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.H),
        12 to arrayOf(Cell.H,  Cell.H,  Cell.S,  Cell.S,  Cell.S,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        13 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        14 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        15 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.H,  Cell.H,  Cell.H,  Cell.Rh, Cell.H),
        16 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.H,  Cell.H,  Cell.Rh, Cell.Rh, Cell.Rh),
        17 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S),
        18 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S),
        19 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S),
        20 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S),
    )

    private fun hardStrategy(score: Int, dealerIdx: Int, cardCount: Int = 2, rules: CasinoRules): Cell {
        // H17 deviation: double 11 vs A
        if (score == 11 && dealerIdx == 9 && !rules.dealerStandsOnSoft17) {
            return Cell.D
        }
        // H17 surrender deviations
        if (!rules.dealerStandsOnSoft17) {
            if (score == 17 && dealerIdx == 9) return Cell.Rs
            if (score == 15 && dealerIdx == 9) return Cell.Rh
        }
        // ENHC (no hole card) deviations
        if (!rules.dealerPeeks) {
            if (score == 11 && dealerIdx == 8) return Cell.H    // Don't double 11 vs 10
            if (score == 14 && dealerIdx == 8) return Cell.Rh   // Surrender 14 vs 10
            if (score == 16 && dealerIdx == 8 && cardCount >= 3) return Cell.S  // Multi-card 16 vs 10: Stand
            if (score == 16 && dealerIdx == 9) return Cell.H    // Don't surrender 16 vs A
        }
        if (score <= 4) return Cell.H
        if (score >= 21) return Cell.S
        return hardTable[score.coerceIn(5, 20)]?.get(dealerIdx) ?: Cell.H
    }

    // -- Soft totals (rows: total 13=A2 through 20=A9, cols: dealer 2-A) --

    //                                     2      3      4      5      6      7      8      9      T      A
    private val softTable: Map<Int, Array<Cell>> = mapOf(
        13 to arrayOf(Cell.H,  Cell.H,  Cell.H,  Cell.D,  Cell.D,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        14 to arrayOf(Cell.H,  Cell.H,  Cell.H,  Cell.D,  Cell.D,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        15 to arrayOf(Cell.H,  Cell.H,  Cell.D,  Cell.D,  Cell.D,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        16 to arrayOf(Cell.H,  Cell.H,  Cell.D,  Cell.D,  Cell.D,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        17 to arrayOf(Cell.H,  Cell.D,  Cell.D,  Cell.D,  Cell.D,  Cell.H,  Cell.H,  Cell.H,  Cell.H,  Cell.H),
        18 to arrayOf(Cell.Ds, Cell.Ds, Cell.Ds, Cell.Ds, Cell.Ds, Cell.S,  Cell.S,  Cell.H,  Cell.H,  Cell.H),
        19 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.Ds, Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S),
        20 to arrayOf(Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S,  Cell.S),
    )

    private fun softStrategy(score: Int, dealerIdx: Int, rules: CasinoRules): Cell {
        // ENHC (no hole card) deviations
        if (!rules.dealerPeeks) {
            if (score == 18 && dealerIdx == 0) return Cell.S    // Soft 18 vs 2: Stand
            if (score == 19 && dealerIdx == 4) return Cell.S    // Soft 19 vs 6: Stand
        }
        return softTable[score.coerceIn(13, 20)]?.get(dealerIdx) ?: Cell.H
    }

    // -- Pairs strategy (by pair rank, cols: dealer 2-A) --

    private fun pairStrategy(rank: Rank, dealerIdx: Int, rules: CasinoRules): Cell? {
        val das = rules.doubleAfterSplit
        return when (rank) {
            Rank.ACE -> if (!rules.dealerPeeks && dealerIdx == 9) Cell.H else Cell.P
            Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> Cell.S  // Never split tens
            Rank.FIVE -> null   // Treat as hard 10
            Rank.NINE -> pairNines(dealerIdx)
            Rank.EIGHT -> pairEights(dealerIdx, rules)
            Rank.SEVEN -> pairSevens(dealerIdx)
            Rank.SIX -> pairSixes(dealerIdx, das)
            Rank.FOUR -> pairFours(dealerIdx, das)
            Rank.THREE -> pairThreeTwos(dealerIdx, das)
            Rank.TWO -> pairThreeTwos(dealerIdx, das)
        }
    }

    //                          2   3   4   5   6   7   8   9   T   A
    private fun pairNines(d: Int): Cell = when (d) {
        5 -> Cell.S   // vs 7: Stand
        8 -> Cell.S   // vs T: Stand
        9 -> Cell.S   // vs A: Stand
        else -> Cell.P // vs 2-6,8,9: Split
    }

    private fun pairEights(d: Int, rules: CasinoRules): Cell {
        // ENHC (no hole card) deviations
        if (!rules.dealerPeeks) {
            if (d == 8) return Cell.Rh  // Surrender 8,8 vs 10
            if (d == 9) return Cell.H   // Hit 8,8 vs A
        }
        // H17 deviation: surrender 8s vs A
        if (d == 9 && !rules.dealerStandsOnSoft17) return Cell.Rp
        return Cell.P // Always split 8s
    }

    private fun pairSevens(d: Int): Cell = when {
        d <= 5 -> Cell.P   // vs 2-7: Split
        else -> Cell.H     // vs 8+: Hit
    }

    private fun pairSixes(d: Int, das: Boolean): Cell = when {
        das && d <= 4 -> Cell.P   // DAS: split vs 2-6
        !das && d in 1..4 -> Cell.P  // No DAS: split vs 3-6
        !das && d == 0 -> Cell.H
        d <= 4 -> Cell.P
        else -> Cell.H
    }

    private fun pairFours(d: Int, das: Boolean): Cell = when {
        das && d in 3..4 -> Cell.P  // DAS: split vs 5-6
        else -> Cell.H
    }

    private fun pairThreeTwos(d: Int, das: Boolean): Cell = when {
        das && d <= 5 -> Cell.P   // DAS: split vs 2-7
        !das && d in 2..5 -> Cell.P  // No DAS: split vs 4-7
        else -> Cell.H
    }
}
