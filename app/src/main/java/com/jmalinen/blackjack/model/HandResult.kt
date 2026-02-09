package com.jmalinen.blackjack.model

enum class HandResult(val displayName: String) {
    THREE_SEVENS("Three 7s!"),
    BLACKJACK("Blackjack!"),
    WIN("Win"),
    LOSE("Lose"),
    PUSH("Push"),
    BUST("Bust"),
    SURRENDER("Surrender")
}
