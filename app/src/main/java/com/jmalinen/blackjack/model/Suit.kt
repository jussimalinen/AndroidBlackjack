package com.jmalinen.blackjack.model

enum class Suit(val symbol: Char, val isRed: Boolean) {
    HEARTS('\u2665', true),
    DIAMONDS('\u2666', true),
    CLUBS('\u2663', false),
    SPADES('\u2660', false)
}
