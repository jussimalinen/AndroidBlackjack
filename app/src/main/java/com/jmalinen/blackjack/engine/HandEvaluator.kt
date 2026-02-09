package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card

object HandEvaluator {

    fun bestScore(cards: List<Card>): Int {
        if (cards.isEmpty()) return 0
        val aceCount = cards.count { it.rank.isAce }
        var sum = cards.filter { !it.rank.isAce }.sumOf { it.rank.baseValue }
        sum += aceCount // all aces as 1
        if (aceCount > 0 && sum + 10 <= 21) {
            sum += 10 // promote one ace to 11
        }
        return sum
    }

    fun isSoft(cards: List<Card>): Boolean {
        if (cards.none { it.rank.isAce }) return false
        val aceCount = cards.count { it.rank.isAce }
        val nonAceSum = cards.filter { !it.rank.isAce }.sumOf { it.rank.baseValue }
        return (nonAceSum + 11 + (aceCount - 1)) <= 21
    }
}
