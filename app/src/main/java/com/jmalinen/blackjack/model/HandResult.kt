package com.jmalinen.blackjack.model

enum class HandResult(val displayName: String) {
    BLACKJACK("Blackjack!"),
    WIN("Win"),
    LOSE("Lose"),
    PUSH("Push"),
    BUST("Bust"),
    SURRENDER("Surrender")
}
