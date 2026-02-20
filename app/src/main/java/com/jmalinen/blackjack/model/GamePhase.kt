package com.jmalinen.blackjack.model

enum class GamePhase {
    BETTING,
    DEALING,
    INSURANCE_OFFERED,
    DEALER_PEEK,
    EXTRA_PLAYERS_TURN,
    PLAYER_TURN,
    DEALER_TURN,
    ROUND_COMPLETE,
    GAME_OVER
}
