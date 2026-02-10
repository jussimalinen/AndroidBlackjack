package com.jmalinen.blackjack.model

data class GameState(
    val phase: GamePhase = GamePhase.BETTING,
    val rules: CasinoRules = CasinoRules(),
    val playerHands: List<Hand> = emptyList(),
    val activeHandIndex: Int = 0,
    val dealerHand: Hand = Hand(),
    val chips: Int = 1000,
    val currentBet: Int = 10,
    val insuranceBet: Int = 0,
    val availableActions: Set<PlayerAction> = emptySet(),
    val handResults: Map<Int, HandResult> = emptyMap(),
    val roundMessage: String = "",
    val showDealerHoleCard: Boolean = false,
    val roundPayout: Int = 0,
    val handsPlayed: Int = 0,
    val handsWon: Int = 0,
    val coachEnabled: Boolean = false,
    val coachFeedback: String = "",
    val runningCount: Int = 0,
    val trueCount: Float = 0f,
    val showCount: Boolean = false
) {
    val activeHand: Hand? get() = playerHands.getOrNull(activeHandIndex)
    val totalBetOnTable: Int get() = playerHands.sumOf { it.bet } + insuranceBet
}
