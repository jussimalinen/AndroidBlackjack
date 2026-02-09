package com.jmalinen.blackjack.model

data class CasinoRules(
    val numberOfDecks: Int = 6,
    val dealerStandsOnSoft17: Boolean = true,
    val dealerPeeks: Boolean = true,
    val blackjackPayout: BlackjackPayout = BlackjackPayout.THREE_TO_TWO,
    val surrenderPolicy: SurrenderPolicy = SurrenderPolicy.NONE,
    val doubleAfterSplit: Boolean = true,
    val resplitAces: Boolean = false,
    val maxSplitHands: Int = 4,
    val hitSplitAces: Boolean = false,
    val doubleOnAnyTwo: Boolean = true,
    val insuranceAvailable: Boolean = true,
    val threeSevensPays3to1: Boolean = false,
    val initialChips: Int = 1000,
    val minimumBet: Int = 10,
    val maximumBet: Int = 500
)

enum class BlackjackPayout(val multiplier: Float, val displayName: String) {
    THREE_TO_TWO(1.5f, "3:2"),
    SIX_TO_FIVE(1.2f, "6:5")
}

enum class SurrenderPolicy(val displayName: String) {
    NONE("No Surrender"),
    LATE("Late Surrender"),
    EARLY("Early Surrender")
}
