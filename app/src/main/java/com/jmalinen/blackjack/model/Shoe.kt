package com.jmalinen.blackjack.model

class Shoe(private val numberOfDecks: Int) {
    private val cards: MutableList<Card> = mutableListOf()
    private var cutCardPosition: Int = 0
    val totalCards: Int = numberOfDecks * 52
    val cardsRemaining: Int get() = cards.size
    val penetration: Float get() = 1f - (cards.size.toFloat() / totalCards)

    init {
        shuffle()
    }

    fun shuffle() {
        cards.clear()
        for (deck in 0 until numberOfDecks) {
            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    cards.add(Card(rank, suit))
                }
            }
        }
        cards.shuffle()
        cutCardPosition = (totalCards * 0.25).toInt()
    }

    fun draw(): Card {
        check(cards.isNotEmpty()) { "Shoe is empty" }
        return cards.removeFirst()
    }

    fun drawMatching(predicate: (Card) -> Boolean): Card {
        val index = cards.indexOfFirst(predicate)
        check(index != -1) { "No matching card in shoe" }
        return cards.removeAt(index)
    }

    fun needsReshuffle(): Boolean = cards.size <= cutCardPosition
}
