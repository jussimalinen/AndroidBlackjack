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

private const val PHONE = "spec:width=411dp,height=891dp,orientation=portrait"
private const val SMALL_PHONE = "spec:width=360dp,height=640dp,orientation=portrait"
private const val TABLET = "spec:width=800dp,height=1280dp,orientation=portrait"

private fun card(rank: com.jmalinen.blackjack.model.Rank, suit: com.jmalinen.blackjack.model.Suit = HEARTS) =
    Card(rank, suit)

private fun hand(vararg cards: Card, bet: Int = 10, isSplitHand: Boolean = false, isStanding: Boolean = false) =
    Hand(cards = cards.toList(), bet = bet, isSplitHand = isSplitHand, isStanding = isStanding)

// ========================================================================
// State factories
// ========================================================================

private fun bettingState() = GameState(
    phase = GamePhase.BETTING,
    chips = 1000,
    currentBet = 25,
    handsPlayed = 5,
    handsWon = 3
)

private fun playerTurnState() = GameState(
    phase = GamePhase.PLAYER_TURN,
    playerHands = listOf(hand(card(TEN, SPADES), card(SIX, HEARTS), bet = 25)),
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

private fun playerTurnSplitState() = GameState(
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

private fun insuranceState() = GameState(
    phase = GamePhase.INSURANCE_OFFERED,
    playerHands = listOf(hand(card(TEN, SPADES), card(SEVEN, HEARTS), bet = 50)),
    dealerHand = Hand(cards = listOf(card(ACE, CLUBS), Card(NINE, DIAMONDS, faceUp = false))),
    chips = 950,
    currentBet = 50,
    availableActions = setOf(PlayerAction.INSURANCE, PlayerAction.DECLINE_INSURANCE),
    handsPlayed = 3,
    handsWon = 1
)

private fun dealerTurnState() = GameState(
    phase = GamePhase.DEALER_TURN,
    playerHands = listOf(hand(card(TEN, SPADES), card(NINE, HEARTS), bet = 25, isStanding = true)),
    dealerHand = Hand(cards = listOf(card(SIX, CLUBS), card(TEN, DIAMONDS), card(TWO, HEARTS))),
    showDealerHoleCard = true,
    chips = 975,
    currentBet = 25,
    handsPlayed = 10,
    handsWon = 5
)

private fun roundWinState() = GameState(
    phase = GamePhase.ROUND_COMPLETE,
    playerHands = listOf(hand(card(TEN, SPADES), card(QUEEN, HEARTS), bet = 50, isStanding = true)),
    dealerHand = Hand(cards = listOf(card(SEVEN, CLUBS), card(TEN, DIAMONDS), card(TWO, HEARTS))),
    showDealerHoleCard = true,
    chips = 1050,
    currentBet = 50,
    handResults = mapOf(0 to HandResult.WIN),
    roundPayout = 100,
    roundMessage = "You win \$50!",
    handsPlayed = 11,
    handsWon = 6
)

private fun blackjackState() = GameState(
    phase = GamePhase.ROUND_COMPLETE,
    playerHands = listOf(hand(card(ACE, SPADES), card(KING, HEARTS), bet = 25)),
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

private fun roundLoseState() = GameState(
    phase = GamePhase.ROUND_COMPLETE,
    playerHands = listOf(hand(card(TEN, SPADES), card(SIX, HEARTS), card(EIGHT, CLUBS), bet = 25)),
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

private fun gameOverState() = GameState(
    phase = GamePhase.GAME_OVER,
    playerHands = listOf(hand(card(TEN, SPADES), card(FIVE, HEARTS), card(NINE, CLUBS), bet = 10)),
    dealerHand = Hand(cards = listOf(card(KING, CLUBS), card(EIGHT, DIAMONDS))),
    showDealerHoleCard = true,
    chips = 0,
    currentBet = 10,
    handResults = mapOf(0 to HandResult.BUST),
    handsPlayed = 42,
    handsWon = 18
)

private fun coachCorrectState() = GameState(
    phase = GamePhase.PLAYER_TURN,
    playerHands = listOf(hand(card(TEN, SPADES), card(SIX, HEARTS), card(FOUR, DIAMONDS), bet = 25)),
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

private fun coachWrongState() = GameState(
    phase = GamePhase.DEALER_TURN,
    playerHands = listOf(hand(card(TEN, SPADES), card(SIX, HEARTS), bet = 25, isStanding = true)),
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

private fun extraPlayersPlayerTurnState() = GameState(
    phase = GamePhase.PLAYER_TURN,
    rules = CasinoRules(extraPlayers = 2),
    playerHands = listOf(hand(card(NINE, SPADES), card(SEVEN, HEARTS), bet = 25)),
    dealerHand = Hand(cards = listOf(card(TEN, CLUBS), card(FIVE, DIAMONDS))),
    chips = 975,
    currentBet = 25,
    availableActions = setOf(
        PlayerAction.HIT, PlayerAction.STAND,
        PlayerAction.DOUBLE_DOWN, PlayerAction.SURRENDER
    ),
    extraPlayers = listOf(
        ExtraPlayerState(hand = hand(card(KING, DIAMONDS), card(THREE, CLUBS), card(SEVEN, HEARTS))),
        ExtraPlayerState(hand = hand(card(FIVE, SPADES), card(SIX, HEARTS), card(NINE, DIAMONDS)))
    ),
    handsPlayed = 4,
    handsWon = 2
)

private fun extraPlayersRoundCompleteState() = GameState(
    phase = GamePhase.ROUND_COMPLETE,
    rules = CasinoRules(extraPlayers = 2),
    playerHands = listOf(hand(card(TEN, SPADES), card(NINE, HEARTS), bet = 25, isStanding = true)),
    dealerHand = Hand(cards = listOf(card(SIX, CLUBS), card(TEN, DIAMONDS), card(SEVEN, HEARTS))),
    showDealerHoleCard = true,
    chips = 1025,
    currentBet = 25,
    handResults = mapOf(0 to HandResult.WIN),
    roundPayout = 50,
    roundMessage = "You win \$25!",
    extraPlayers = listOf(
        ExtraPlayerState(hand = hand(card(KING, DIAMONDS), card(EIGHT, CLUBS)), result = HandResult.WIN),
        ExtraPlayerState(
            hand = hand(card(FIVE, SPADES), card(SIX, HEARTS), card(QUEEN, DIAMONDS)),
            result = HandResult.BUST
        )
    ),
    handsPlayed = 5,
    handsWon = 3
)

private fun extraPlayersTurnState() = GameState(
    phase = GamePhase.EXTRA_PLAYERS_TURN,
    rules = CasinoRules(extraPlayers = 2),
    playerHands = listOf(hand(card(JACK, SPADES), card(FOUR, HEARTS), bet = 50)),
    dealerHand = Hand(cards = listOf(card(EIGHT, CLUBS), card(THREE, DIAMONDS))),
    chips = 950,
    currentBet = 50,
    extraPlayers = listOf(
        ExtraPlayerState(hand = hand(card(SEVEN, DIAMONDS), card(FIVE, CLUBS), card(SIX, HEARTS))),
        ExtraPlayerState(hand = hand(card(QUEEN, SPADES), card(TWO, HEARTS)))
    ),
    handsPlayed = 12,
    handsWon = 6
)

private fun extraPlayersTripleSplitState() = GameState(
    phase = GamePhase.PLAYER_TURN,
    rules = CasinoRules(extraPlayers = 2, maxSplitHands = 4),
    playerHands = listOf(
        hand(card(EIGHT, SPADES), card(THREE, DIAMONDS), bet = 25, isSplitHand = true, isStanding = true),
        hand(card(EIGHT, HEARTS), card(TEN, CLUBS), bet = 25, isSplitHand = true, isStanding = true),
        hand(card(EIGHT, CLUBS), card(FIVE, HEARTS), bet = 25, isSplitHand = true),
        hand(card(EIGHT, DIAMONDS), card(SEVEN, SPADES), bet = 25, isSplitHand = true, isStanding = true)
    ),
    activeHandIndex = 2,
    dealerHand = Hand(cards = listOf(card(SIX, CLUBS), card(QUEEN, DIAMONDS))),
    chips = 900,
    currentBet = 25,
    availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN),
    extraPlayers = listOf(
        ExtraPlayerState(hand = hand(card(KING, DIAMONDS), card(THREE, CLUBS), card(SEVEN, HEARTS))),
        ExtraPlayerState(hand = hand(card(FIVE, SPADES), card(SIX, HEARTS), card(FOUR, DIAMONDS), card(FIVE, CLUBS)))
    ),
    handsPlayed = 15,
    handsWon = 7
)

private fun extraPlayersManyCardsState() = GameState(
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
    dealerHand = Hand(cards = listOf(card(SIX, CLUBS), card(TEN, DIAMONDS), card(SEVEN, HEARTS))),
    showDealerHoleCard = true,
    chips = 1025,
    currentBet = 25,
    handResults = mapOf(0 to HandResult.WIN, 1 to HandResult.WIN, 2 to HandResult.LOSE, 3 to HandResult.WIN),
    roundPayout = 125,
    roundMessage = "You win \$75!",
    extraPlayers = listOf(
        ExtraPlayerState(
            hand = hand(card(KING, DIAMONDS), card(THREE, CLUBS), card(FOUR, HEARTS), card(TWO, SPADES)),
            result = HandResult.LOSE
        ),
        ExtraPlayerState(
            hand = hand(card(FIVE, SPADES), card(SIX, HEARTS), card(FOUR, DIAMONDS), card(THREE, CLUBS), card(TWO, HEARTS)),
            result = HandResult.WIN
        )
    ),
    handsPlayed = 20,
    handsWon = 11
)

private fun countDisplayState() = GameState(
    phase = GamePhase.PLAYER_TURN,
    playerHands = listOf(hand(card(FIVE, SPADES), card(SIX, HEARTS), bet = 50)),
    dealerHand = Hand(cards = listOf(card(FOUR, CLUBS), card(NINE, DIAMONDS))),
    chips = 950,
    currentBet = 50,
    availableActions = setOf(PlayerAction.HIT, PlayerAction.STAND, PlayerAction.DOUBLE_DOWN),
    showCount = true,
    runningCount = 7,
    trueCount = 2.3f,
    shoePenetration = 0.45f,
    handsPlayed = 20,
    handsWon = 10
)

// ========================================================================
// Phone (411x891dp)
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Betting")
@Composable
private fun PreviewBetting() { BlackjackTheme { GameScreenContent(state = bettingState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Player Turn")
@Composable
private fun PreviewPlayerTurn() { BlackjackTheme { GameScreenContent(state = playerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Player Turn - Split")
@Composable
private fun PreviewPlayerTurnSplit() { BlackjackTheme { GameScreenContent(state = playerTurnSplitState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Insurance Offered")
@Composable
private fun PreviewInsurance() { BlackjackTheme { GameScreenContent(state = insuranceState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Dealer Turn")
@Composable
private fun PreviewDealerTurn() { BlackjackTheme { GameScreenContent(state = dealerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Round Complete - Win")
@Composable
private fun PreviewRoundWin() { BlackjackTheme { GameScreenContent(state = roundWinState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Round Complete - Blackjack")
@Composable
private fun PreviewBlackjack() { BlackjackTheme { GameScreenContent(state = blackjackState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Round Complete - Lose")
@Composable
private fun PreviewRoundLose() { BlackjackTheme { GameScreenContent(state = roundLoseState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Game Over")
@Composable
private fun PreviewGameOver() { BlackjackTheme { GameScreenContent(state = gameOverState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Coach - Correct")
@Composable
private fun PreviewCoachCorrect() { BlackjackTheme { GameScreenContent(state = coachCorrectState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Coach - Wrong")
@Composable
private fun PreviewCoachWrong() { BlackjackTheme { GameScreenContent(state = coachWrongState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Extra Players - Player Turn")
@Composable
private fun PreviewExtraPlayersPlayerTurn() { BlackjackTheme { GameScreenContent(state = extraPlayersPlayerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Extra Players - Round Complete")
@Composable
private fun PreviewExtraPlayersRoundComplete() { BlackjackTheme { GameScreenContent(state = extraPlayersRoundCompleteState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Extra Players Turn")
@Composable
private fun PreviewExtraPlayersTurn() { BlackjackTheme { GameScreenContent(state = extraPlayersTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Extra Players + Triple Split")
@Composable
private fun PreviewExtraPlayersTripleSplit() { BlackjackTheme { GameScreenContent(state = extraPlayersTripleSplitState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Extra Players + Many Cards")
@Composable
private fun PreviewExtraPlayersManyCards() { BlackjackTheme { GameScreenContent(state = extraPlayersManyCardsState()) } }

@Preview(showBackground = true, showSystemUi = true, device = PHONE, name = "Count Display")
@Composable
private fun PreviewCountDisplay() { BlackjackTheme { GameScreenContent(state = countDisplayState()) } }

// ========================================================================
// Small Phone (360x640dp)
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Betting")
@Composable
private fun PreviewSmallBetting() { BlackjackTheme { GameScreenContent(state = bettingState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Player Turn")
@Composable
private fun PreviewSmallPlayerTurn() { BlackjackTheme { GameScreenContent(state = playerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Player Turn - Split")
@Composable
private fun PreviewSmallPlayerTurnSplit() { BlackjackTheme { GameScreenContent(state = playerTurnSplitState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Insurance Offered")
@Composable
private fun PreviewSmallInsurance() { BlackjackTheme { GameScreenContent(state = insuranceState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Dealer Turn")
@Composable
private fun PreviewSmallDealerTurn() { BlackjackTheme { GameScreenContent(state = dealerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Round Complete - Win")
@Composable
private fun PreviewSmallRoundWin() { BlackjackTheme { GameScreenContent(state = roundWinState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Round Complete - Blackjack")
@Composable
private fun PreviewSmallBlackjack() { BlackjackTheme { GameScreenContent(state = blackjackState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Round Complete - Lose")
@Composable
private fun PreviewSmallRoundLose() { BlackjackTheme { GameScreenContent(state = roundLoseState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Game Over")
@Composable
private fun PreviewSmallGameOver() { BlackjackTheme { GameScreenContent(state = gameOverState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Coach - Correct")
@Composable
private fun PreviewSmallCoachCorrect() { BlackjackTheme { GameScreenContent(state = coachCorrectState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Coach - Wrong")
@Composable
private fun PreviewSmallCoachWrong() { BlackjackTheme { GameScreenContent(state = coachWrongState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Extra Players - Player Turn")
@Composable
private fun PreviewSmallExtraPlayersPlayerTurn() { BlackjackTheme { GameScreenContent(state = extraPlayersPlayerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Extra Players - Round Complete")
@Composable
private fun PreviewSmallExtraPlayersRoundComplete() { BlackjackTheme { GameScreenContent(state = extraPlayersRoundCompleteState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Extra Players Turn")
@Composable
private fun PreviewSmallExtraPlayersTurn() { BlackjackTheme { GameScreenContent(state = extraPlayersTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Extra Players + Triple Split")
@Composable
private fun PreviewSmallExtraPlayersTripleSplit() { BlackjackTheme { GameScreenContent(state = extraPlayersTripleSplitState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Extra Players + Many Cards")
@Composable
private fun PreviewSmallExtraPlayersManyCards() { BlackjackTheme { GameScreenContent(state = extraPlayersManyCardsState()) } }

@Preview(showBackground = true, showSystemUi = true, device = SMALL_PHONE, name = "Small - Count Display")
@Composable
private fun PreviewSmallCountDisplay() { BlackjackTheme { GameScreenContent(state = countDisplayState()) } }

// ========================================================================
// Tablet (800x1280dp)
// ========================================================================

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Betting")
@Composable
private fun PreviewTabletBetting() { BlackjackTheme { GameScreenContent(state = bettingState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Player Turn")
@Composable
private fun PreviewTabletPlayerTurn() { BlackjackTheme { GameScreenContent(state = playerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Player Turn - Split")
@Composable
private fun PreviewTabletPlayerTurnSplit() { BlackjackTheme { GameScreenContent(state = playerTurnSplitState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Insurance Offered")
@Composable
private fun PreviewTabletInsurance() { BlackjackTheme { GameScreenContent(state = insuranceState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Dealer Turn")
@Composable
private fun PreviewTabletDealerTurn() { BlackjackTheme { GameScreenContent(state = dealerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Round Complete - Win")
@Composable
private fun PreviewTabletRoundWin() { BlackjackTheme { GameScreenContent(state = roundWinState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Round Complete - Blackjack")
@Composable
private fun PreviewTabletBlackjack() { BlackjackTheme { GameScreenContent(state = blackjackState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Round Complete - Lose")
@Composable
private fun PreviewTabletRoundLose() { BlackjackTheme { GameScreenContent(state = roundLoseState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Game Over")
@Composable
private fun PreviewTabletGameOver() { BlackjackTheme { GameScreenContent(state = gameOverState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Coach - Correct")
@Composable
private fun PreviewTabletCoachCorrect() { BlackjackTheme { GameScreenContent(state = coachCorrectState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Coach - Wrong")
@Composable
private fun PreviewTabletCoachWrong() { BlackjackTheme { GameScreenContent(state = coachWrongState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Extra Players - Player Turn")
@Composable
private fun PreviewTabletExtraPlayersPlayerTurn() { BlackjackTheme { GameScreenContent(state = extraPlayersPlayerTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Extra Players - Round Complete")
@Composable
private fun PreviewTabletExtraPlayersRoundComplete() { BlackjackTheme { GameScreenContent(state = extraPlayersRoundCompleteState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Extra Players Turn")
@Composable
private fun PreviewTabletExtraPlayersTurn() { BlackjackTheme { GameScreenContent(state = extraPlayersTurnState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Extra Players + Triple Split")
@Composable
private fun PreviewTabletExtraPlayersTripleSplit() { BlackjackTheme { GameScreenContent(state = extraPlayersTripleSplitState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Extra Players + Many Cards")
@Composable
private fun PreviewTabletExtraPlayersManyCards() { BlackjackTheme { GameScreenContent(state = extraPlayersManyCardsState()) } }

@Preview(showBackground = true, showSystemUi = true, device = TABLET, name = "Tablet - Count Display")
@Composable
private fun PreviewTabletCountDisplay() { BlackjackTheme { GameScreenContent(state = countDisplayState()) } }
