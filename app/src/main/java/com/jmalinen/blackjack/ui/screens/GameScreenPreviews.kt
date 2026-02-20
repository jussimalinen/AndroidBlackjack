package com.jmalinen.blackjack.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.ExtraPlayerState
import com.jmalinen.blackjack.model.GamePhase
import com.jmalinen.blackjack.model.GameState
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.Rank.*
import com.jmalinen.blackjack.model.Suit.*
import com.jmalinen.blackjack.ui.theme.BlackjackTheme

private fun card(rank: com.jmalinen.blackjack.model.Rank, suit: com.jmalinen.blackjack.model.Suit = HEARTS) =
    Card(rank, suit)

private fun hand(vararg cards: Card, bet: Int = 10, isSplitHand: Boolean = false, isStanding: Boolean = false) =
    Hand(cards = cards.toList(), bet = bet, isSplitHand = isSplitHand, isStanding = isStanding)

// ========================================================================
// Betting
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Betting")
@Composable
private fun PreviewBetting() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.BETTING,
                chips = 1000,
                currentBet = 25,
                handsPlayed = 5,
                handsWon = 3
            )
        )
    }
}

// ========================================================================
// Player Turn
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Player Turn")
@Composable
private fun PreviewPlayerTurn() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.PLAYER_TURN,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(SIX, HEARTS), bet = 25)
                ),
                dealerHand = Hand(cards = listOf(card(SEVEN, CLUBS), card(NINE, DIAMONDS))),
                chips = 975,
                currentBet = 25,
                availableActions = setOf(
                    PlayerAction.HIT, PlayerAction.STAND,
                    PlayerAction.DOUBLE_DOWN, PlayerAction.SURRENDER
                ),
                handsPlayed = 5,
                handsWon = 3
            )
        )
    }
}

// ========================================================================
// Player Turn — Split Hands
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Player Turn - Split")
@Composable
private fun PreviewPlayerTurnSplit() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.PLAYER_TURN,
                playerHands = listOf(
                    hand(card(EIGHT, SPADES), card(THREE, HEARTS), bet = 25, isSplitHand = true, isStanding = true),
                    hand(card(EIGHT, DIAMONDS), card(FIVE, CLUBS), bet = 25, isSplitHand = true)
                ),
                activeHandIndex = 1,
                dealerHand = Hand(cards = listOf(card(SIX, CLUBS), card(QUEEN, DIAMONDS))),
                chips = 950,
                currentBet = 25,
                availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN),
                handsPlayed = 8,
                handsWon = 4
            )
        )
    }
}

// ========================================================================
// Insurance Offered
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Insurance Offered")
@Composable
private fun PreviewInsurance() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.INSURANCE_OFFERED,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(SEVEN, HEARTS), bet = 50)
                ),
                dealerHand = Hand(cards = listOf(card(ACE, CLUBS), Card(NINE, DIAMONDS, faceUp = false))),
                chips = 950,
                currentBet = 50,
                availableActions = setOf(PlayerAction.INSURANCE, PlayerAction.DECLINE_INSURANCE),
                handsPlayed = 3,
                handsWon = 1
            )
        )
    }
}

// ========================================================================
// Dealer Turn
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Dealer Turn")
@Composable
private fun PreviewDealerTurn() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.DEALER_TURN,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(NINE, HEARTS), bet = 25, isStanding = true)
                ),
                dealerHand = Hand(cards = listOf(
                    card(SIX, CLUBS), card(TEN, DIAMONDS), card(TWO, HEARTS)
                )),
                showDealerHoleCard = true,
                chips = 975,
                currentBet = 25,
                handsPlayed = 10,
                handsWon = 5
            )
        )
    }
}

// ========================================================================
// Round Complete — Win
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Round Complete - Win")
@Composable
private fun PreviewRoundWin() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.ROUND_COMPLETE,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(QUEEN, HEARTS), bet = 50, isStanding = true)
                ),
                dealerHand = Hand(cards = listOf(
                    card(SEVEN, CLUBS), card(TEN, DIAMONDS), card(TWO, HEARTS)
                )),
                showDealerHoleCard = true,
                chips = 1050,
                currentBet = 50,
                handResults = mapOf(0 to HandResult.WIN),
                roundPayout = 100,
                roundMessage = "You win \$50!",
                handsPlayed = 11,
                handsWon = 6
            )
        )
    }
}

// ========================================================================
// Round Complete — Blackjack
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Round Complete - Blackjack")
@Composable
private fun PreviewBlackjack() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.ROUND_COMPLETE,
                playerHands = listOf(
                    hand(card(ACE, SPADES), card(KING, HEARTS), bet = 25)
                ),
                dealerHand = Hand(cards = listOf(card(NINE, CLUBS), card(SEVEN, DIAMONDS))),
                showDealerHoleCard = true,
                chips = 1037,
                currentBet = 25,
                handResults = mapOf(0 to HandResult.BLACKJACK),
                roundPayout = 62,
                roundMessage = "Blackjack!",
                handsPlayed = 6,
                handsWon = 4
            )
        )
    }
}

// ========================================================================
// Round Complete — Lose
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Round Complete - Lose")
@Composable
private fun PreviewRoundLose() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.ROUND_COMPLETE,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(SIX, HEARTS), card(EIGHT, CLUBS), bet = 25)
                ),
                dealerHand = Hand(cards = listOf(card(SEVEN, CLUBS), card(TEN, DIAMONDS))),
                showDealerHoleCard = true,
                chips = 950,
                currentBet = 25,
                handResults = mapOf(0 to HandResult.BUST),
                roundPayout = 0,
                roundMessage = "You lose \$25",
                handsPlayed = 7,
                handsWon = 3
            )
        )
    }
}

// ========================================================================
// Game Over
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Game Over")
@Composable
private fun PreviewGameOver() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.GAME_OVER,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(FIVE, HEARTS), card(NINE, CLUBS), bet = 10)
                ),
                dealerHand = Hand(cards = listOf(card(KING, CLUBS), card(EIGHT, DIAMONDS))),
                showDealerHoleCard = true,
                chips = 0,
                currentBet = 10,
                handResults = mapOf(0 to HandResult.BUST),
                handsPlayed = 42,
                handsWon = 18
            )
        )
    }
}

// ========================================================================
// Coach Feedback
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Coach - Correct")
@Composable
private fun PreviewCoachCorrect() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.PLAYER_TURN,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(SIX, HEARTS), card(FOUR, DIAMONDS), bet = 25)
                ),
                dealerHand = Hand(cards = listOf(card(SEVEN, CLUBS), card(NINE, DIAMONDS))),
                chips = 975,
                currentBet = 25,
                availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND),
                coachEnabled = true,
                coachFeedback = "Correct! Hit was optimal.",
                coachCorrect = 7,
                coachTotal = 9,
                handsPlayed = 9,
                handsWon = 4
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Coach - Wrong")
@Composable
private fun PreviewCoachWrong() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.DEALER_TURN,
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(SIX, HEARTS), bet = 25, isStanding = true)
                ),
                dealerHand = Hand(cards = listOf(card(SEVEN, CLUBS), card(TEN, DIAMONDS))),
                showDealerHoleCard = true,
                chips = 975,
                currentBet = 25,
                coachEnabled = true,
                coachFeedback = "Optimal play: Hit (you chose Stand)",
                coachCorrect = 5,
                coachTotal = 10,
                handsPlayed = 10,
                handsWon = 4
            )
        )
    }
}

// ========================================================================
// Extra Players — Player Turn
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Extra Players - Player Turn")
@Composable
private fun PreviewExtraPlayersPlayerTurn() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.PLAYER_TURN,
                rules = CasinoRules(extraPlayers = 2),
                playerHands = listOf(
                    hand(card(NINE, SPADES), card(SEVEN, HEARTS), bet = 25)
                ),
                dealerHand = Hand(cards = listOf(card(TEN, CLUBS), card(FIVE, DIAMONDS))),
                chips = 975,
                currentBet = 25,
                availableActions = setOf(
                    PlayerAction.HIT, PlayerAction.STAND,
                    PlayerAction.DOUBLE_DOWN, PlayerAction.SURRENDER
                ),
                extraPlayers = listOf(
                    ExtraPlayerState(hand = hand(
                        card(KING, DIAMONDS), card(THREE, CLUBS), card(SEVEN, HEARTS)
                    )),
                    ExtraPlayerState(hand = hand(
                        card(FIVE, SPADES), card(SIX, HEARTS), card(NINE, DIAMONDS)
                    ))
                ),
                handsPlayed = 4,
                handsWon = 2
            )
        )
    }
}

// ========================================================================
// Extra Players — Round Complete
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Extra Players - Round Complete")
@Composable
private fun PreviewExtraPlayersRoundComplete() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.ROUND_COMPLETE,
                rules = CasinoRules(extraPlayers = 2),
                playerHands = listOf(
                    hand(card(TEN, SPADES), card(NINE, HEARTS), bet = 25, isStanding = true)
                ),
                dealerHand = Hand(cards = listOf(
                    card(SIX, CLUBS), card(TEN, DIAMONDS), card(SEVEN, HEARTS)
                )),
                showDealerHoleCard = true,
                chips = 1025,
                currentBet = 25,
                handResults = mapOf(0 to HandResult.WIN),
                roundPayout = 50,
                roundMessage = "You win \$25!",
                extraPlayers = listOf(
                    ExtraPlayerState(
                        hand = hand(card(KING, DIAMONDS), card(EIGHT, CLUBS)),
                        result = HandResult.WIN
                    ),
                    ExtraPlayerState(
                        hand = hand(card(FIVE, SPADES), card(SIX, HEARTS), card(QUEEN, DIAMONDS)),
                        result = HandResult.BUST
                    )
                ),
                handsPlayed = 5,
                handsWon = 3
            )
        )
    }
}

// ========================================================================
// Extra Players Turn
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Extra Players Turn")
@Composable
private fun PreviewExtraPlayersTurn() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.EXTRA_PLAYERS_TURN,
                rules = CasinoRules(extraPlayers = 2),
                playerHands = listOf(
                    hand(card(JACK, SPADES), card(FOUR, HEARTS), bet = 50)
                ),
                dealerHand = Hand(cards = listOf(card(EIGHT, CLUBS), card(THREE, DIAMONDS))),
                chips = 950,
                currentBet = 50,
                extraPlayers = listOf(
                    ExtraPlayerState(hand = hand(
                        card(SEVEN, DIAMONDS), card(FIVE, CLUBS), card(SIX, HEARTS)
                    )),
                    ExtraPlayerState(hand = hand(
                        card(QUEEN, SPADES), card(TWO, HEARTS)
                    ))
                ),
                handsPlayed = 12,
                handsWon = 6
            )
        )
    }
}

// ========================================================================
// Extra Players + Triple Split (4 player hands)
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Extra Players + Triple Split")
@Composable
private fun PreviewExtraPlayersTripleSplit() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.PLAYER_TURN,
                rules = CasinoRules(extraPlayers = 2, maxSplitHands = 4),
                playerHands = listOf(
                    hand(card(EIGHT, SPADES), card(THREE, DIAMONDS), bet = 25,
                        isSplitHand = true, isStanding = true),
                    hand(card(EIGHT, HEARTS), card(TEN, CLUBS), bet = 25,
                        isSplitHand = true, isStanding = true),
                    hand(card(EIGHT, CLUBS), card(FIVE, HEARTS), bet = 25,
                        isSplitHand = true),
                    hand(card(EIGHT, DIAMONDS), card(SEVEN, SPADES), bet = 25,
                        isSplitHand = true, isStanding = true)
                ),
                activeHandIndex = 2,
                dealerHand = Hand(cards = listOf(card(SIX, CLUBS), card(QUEEN, DIAMONDS))),
                chips = 900,
                currentBet = 25,
                availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN),
                extraPlayers = listOf(
                    ExtraPlayerState(hand = hand(
                        card(KING, DIAMONDS), card(THREE, CLUBS), card(SEVEN, HEARTS)
                    )),
                    ExtraPlayerState(hand = hand(
                        card(FIVE, SPADES), card(SIX, HEARTS), card(FOUR, DIAMONDS), card(FIVE, CLUBS)
                    ))
                ),
                handsPlayed = 15,
                handsWon = 7
            )
        )
    }
}

// ========================================================================
// Extra Players + Triple Split + Many Cards
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Extra Players + Many Cards")
@Composable
private fun PreviewExtraPlayersManyCards() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.ROUND_COMPLETE,
                rules = CasinoRules(extraPlayers = 2, maxSplitHands = 4),
                playerHands = listOf(
                    hand(card(EIGHT, SPADES), card(THREE, DIAMONDS), card(TWO, CLUBS),
                        card(FOUR, HEARTS), card(THREE, SPADES), bet = 25,
                        isSplitHand = true, isStanding = true),
                    hand(card(EIGHT, HEARTS), card(TWO, CLUBS), card(FIVE, DIAMONDS),
                        card(SIX, SPADES), bet = 50,
                        isSplitHand = true, isStanding = true),
                    hand(card(EIGHT, CLUBS), card(SEVEN, HEARTS), card(FOUR, SPADES), bet = 25,
                        isSplitHand = true, isStanding = true),
                    hand(card(EIGHT, DIAMONDS), card(THREE, CLUBS), card(FIVE, HEARTS),
                        card(FOUR, DIAMONDS), bet = 25,
                        isSplitHand = true, isStanding = true)
                ),
                activeHandIndex = 0,
                dealerHand = Hand(cards = listOf(
                    card(SIX, CLUBS), card(TEN, DIAMONDS), card(SEVEN, HEARTS)
                )),
                showDealerHoleCard = true,
                chips = 1025,
                currentBet = 25,
                handResults = mapOf(
                    0 to HandResult.WIN,
                    1 to HandResult.WIN,
                    2 to HandResult.LOSE,
                    3 to HandResult.WIN
                ),
                roundPayout = 125,
                roundMessage = "You win \$75!",
                extraPlayers = listOf(
                    ExtraPlayerState(
                        hand = hand(card(KING, DIAMONDS), card(THREE, CLUBS),
                            card(FOUR, HEARTS), card(TWO, SPADES)),
                        result = HandResult.LOSE
                    ),
                    ExtraPlayerState(
                        hand = hand(card(FIVE, SPADES), card(SIX, HEARTS),
                            card(FOUR, DIAMONDS), card(THREE, CLUBS), card(TWO, HEARTS)),
                        result = HandResult.WIN
                    )
                ),
                handsPlayed = 20,
                handsWon = 11
            )
        )
    }
}

// ========================================================================
// Count Display
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp,orientation=portrait", name = "Count Display")
@Composable
private fun PreviewCountDisplay() {
    BlackjackTheme {
        GameScreenContent(
            state = GameState(
                phase = GamePhase.PLAYER_TURN,
                playerHands = listOf(
                    hand(card(FIVE, SPADES), card(SIX, HEARTS), bet = 50)
                ),
                dealerHand = Hand(cards = listOf(card(FOUR, CLUBS), card(NINE, DIAMONDS))),
                chips = 950,
                currentBet = 50,
                availableActions = setOf(
                    PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN
                ),
                showCount = true,
                runningCount = 7,
                trueCount = 2.3f,
                shoePenetration = 0.45f,
                handsPlayed = 20,
                handsWon = 10
            )
        )
    }
}
