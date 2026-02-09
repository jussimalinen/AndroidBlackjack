package com.jmalinen.blackjack.model

enum class PlayerAction(val displayName: String) {
    HIT("Hit"),
    STAND("Stand"),
    DOUBLE_DOWN("Double"),
    SPLIT("Split"),
    SURRENDER("Surrender"),
    INSURANCE("Insurance"),
    DECLINE_INSURANCE("No Insurance"),
    EVEN_MONEY("Even Money"),
    DECLINE_EVEN_MONEY("No Even Money")
}
