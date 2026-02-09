package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.SurrenderPolicy

object BlackjackEngine {

    fun availableActions(
        hand: Hand,
        playerHands: List<Hand>,
        dealerUpCard: Card?,
        chips: Int,
        rules: CasinoRules
    ): Set<PlayerAction> {
        val actions = mutableSetOf<PlayerAction>()

        if (hand.isFinished) return actions

        if (hand.splitFromAces && !rules.hitSplitAces) {
            actions.add(PlayerAction.STAND)
            return actions
        }

        actions.add(PlayerAction.HIT)
        actions.add(PlayerAction.STAND)

        if (hand.cards.size == 2 && chips >= hand.bet) {
            if (rules.doubleOnAnyTwo) {
                if (!hand.isSplitHand || rules.doubleAfterSplit) {
                    actions.add(PlayerAction.DOUBLE_DOWN)
                }
            }
        }

        if (hand.isPair && playerHands.size < rules.maxSplitHands && chips >= hand.bet) {
            if (hand.cards.first().rank.isAce && hand.isSplitHand && !rules.resplitAces) {
                // cannot resplit aces
            } else {
                actions.add(PlayerAction.SPLIT)
            }
        }

        if (hand.cards.size == 2 && !hand.isSplitHand && playerHands.size == 1) {
            when (rules.surrenderPolicy) {
                SurrenderPolicy.LATE -> actions.add(PlayerAction.SURRENDER)
                SurrenderPolicy.EARLY -> actions.add(PlayerAction.SURRENDER)
                SurrenderPolicy.NONE -> {}
            }
        }

        return actions
    }

    fun insuranceActions(hand: Hand, chips: Int): Set<PlayerAction> {
        val actions = mutableSetOf<PlayerAction>()
        if (hand.isBlackjack) {
            actions.add(PlayerAction.EVEN_MONEY)
            actions.add(PlayerAction.DECLINE_EVEN_MONEY)
        } else {
            if (chips >= hand.bet / 2) {
                actions.add(PlayerAction.INSURANCE)
            }
            actions.add(PlayerAction.DECLINE_INSURANCE)
        }
        return actions
    }
}
