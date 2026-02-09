package com.jmalinen.blackjack.model

import com.jmalinen.blackjack.engine.HandEvaluator

data class Hand(
    val cards: List<Card> = emptyList(),
    val bet: Int = 0,
    val isSplitHand: Boolean = false,
    val isDoubledDown: Boolean = false,
    val isSurrendered: Boolean = false,
    val isStanding: Boolean = false,
    val splitFromAces: Boolean = false
) {
    val score: Int get() = HandEvaluator.bestScore(cards)

    val isSoft: Boolean get() = HandEvaluator.isSoft(cards)

    val isBusted: Boolean get() = score > 21

    val isBlackjack: Boolean
        get() = cards.size == 2 && score == 21 && !isSplitHand

    val isPair: Boolean
        get() = cards.size == 2 && cards[0].rank.baseValue == cards[1].rank.baseValue

    val isFinished: Boolean
        get() = isBusted || isStanding || isDoubledDown || isSurrendered || isBlackjack ||
                (splitFromAces && cards.size >= 2)

    fun addCard(card: Card): Hand = copy(cards = cards + card)
}
