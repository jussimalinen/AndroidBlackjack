package com.jmalinen.blackjack.model

enum class Rank(val symbol: String, val baseValue: Int) {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 10),
    QUEEN("Q", 10),
    KING("K", 10),
    ACE("A", 11);

    val isAce: Boolean get() = this == ACE
    val isTenValue: Boolean get() = baseValue == 10 && this != ACE
}
