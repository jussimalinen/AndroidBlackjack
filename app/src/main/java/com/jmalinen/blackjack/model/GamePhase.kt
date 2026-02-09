package com.jmalinen.blackjack.model

enum class GamePhase {
    BETTING,
    DEALING,
    INSURANCE_OFFERED,
    DEALER_PEEK,
    PLAYER_TURN,
    DEALER_TURN,
    ROUND_COMPLETE,
    GAME_OVER
}
