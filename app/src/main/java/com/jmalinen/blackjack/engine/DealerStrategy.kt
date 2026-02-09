package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand

object DealerStrategy {

    fun shouldHit(hand: Hand, rules: CasinoRules): Boolean {
        val score = hand.score
        if (score < 17) return true
        if (score > 17) return false
        // score == 17
        return if (rules.dealerStandsOnSoft17) false else hand.isSoft
    }
}
