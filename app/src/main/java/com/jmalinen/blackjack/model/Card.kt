package com.jmalinen.blackjack.model

data class Card(
    val rank: Rank,
    val suit: Suit,
    val faceUp: Boolean = true
) {
    val display: String get() = "${rank.symbol}${suit.symbol}"

    fun flip(): Card = copy(faceUp = !faceUp)
}
