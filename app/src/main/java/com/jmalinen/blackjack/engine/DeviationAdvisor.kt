package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.Card
import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.PlayerAction
import com.jmalinen.blackjack.model.Rank

data class DeviationResult(
    val action: PlayerAction,
    val isDeviation: Boolean,
    val description: String?
)

/**
 * Count-based deviation advisor. Wraps BasicStrategyAdvisor with Hi-Lo
 * index overrides from docs/Deviations.md.
 */
object DeviationAdvisor {

    fun optimalAction(
        hand: Hand,
        dealerUpCard: Card,
        availableActions: Set<PlayerAction>,
        rules: CasinoRules,
        runningCount: Int,
        trueCount: Float
    ): DeviationResult {
        val basicAction = BasicStrategyAdvisor.optimalAction(hand, dealerUpCard, availableActions, rules)
        val deviation = checkDeviation(hand, dealerUpCard, availableActions, runningCount, trueCount)

        return if (deviation != null) {
            deviation
        } else {
            DeviationResult(action = basicAction, isDeviation = false, description = null)
        }
    }

    /**
     * Returns a take-insurance deviation when TC >= +3, null otherwise.
     */
    fun insuranceDeviation(trueCount: Float): DeviationResult? {
        if (trueCount >= 3f) {
            return DeviationResult(
                action = PlayerAction.HIT, // placeholder — caller handles insurance action
                isDeviation = true,
                description = "Deviation (TC ≥ +3): Take insurance"
            )
        }
        return null
    }

    private fun checkDeviation(
        hand: Hand,
        dealerUpCard: Card,
        availableActions: Set<PlayerAction>,
        runningCount: Int,
        trueCount: Float
    ): DeviationResult? {
        val dealerRank = dealerUpCard.rank
        val score = hand.score

        // 16 vs 10: Stand at RC > 0 (basic says Hit)
        if (score == 16 && !hand.isSoft && dealerRank.isTenValue && runningCount > 0) {
            if (PlayerAction.STAND in availableActions) {
                if (hand.cards.size > 2 || PlayerAction.SURRENDER !in availableActions) {
                    return DeviationResult(
                        action = PlayerAction.STAND,
                        isDeviation = true,
                        description = "Deviation (RC > 0): Stand 16 vs 10"
                    )
                }
            }
        }
        // 16 vs 10: Stand at RC > 0 (basic says Hit)
        if (score == 16 && !hand.isSoft && dealerRank.isTenValue && runningCount <= 0) {
            if (PlayerAction.HIT in availableActions) {
                if (hand.cards.size > 2 || PlayerAction.SURRENDER !in availableActions) {
                    return DeviationResult(
                        action = PlayerAction.HIT,
                        isDeviation = true,
                        description = "Deviation (RC <= 0): Hit 16 vs 10"
                    )
                }
            }
        }

        // 12 vs 2: Stand at TC >= +3 (basic says Hit)
        if (score == 12 && !hand.isSoft && dealerRank == Rank.TWO && trueCount >= 3f) {
            if (PlayerAction.STAND in availableActions) {
                return DeviationResult(
                    action = PlayerAction.STAND,
                    isDeviation = true,
                    description = "Deviation (TC ≥ +3): Stand 12 vs 2"
                )
            }
        }

        // 12 vs 3: Stand at TC >= +2 (basic says Hit)
        if (score == 12 && !hand.isSoft && dealerRank == Rank.THREE && trueCount >= 2f) {
            if (PlayerAction.STAND in availableActions) {
                return DeviationResult(
                    action = PlayerAction.STAND,
                    isDeviation = true,
                    description = "Deviation (TC ≥ +2): Stand 12 vs 3"
                )
            }
        }

        // 12 vs 4: Hit at RC < 0 (basic says Stand)
        if (score == 12 && !hand.isSoft && dealerRank == Rank.FOUR && runningCount < 0) {
            if (PlayerAction.HIT in availableActions) {
                return DeviationResult(
                    action = PlayerAction.HIT,
                    isDeviation = true,
                    description = "Deviation (RC < 0): Hit 12 vs 4"
                )
            }
        }

        // A,4 (soft 15) vs 4: Hit at RC < 0 (basic says Double)
        if (score == 15 && hand.isSoft && dealerRank == Rank.FOUR && runningCount < 0) {
            if (PlayerAction.HIT in availableActions) {
                return DeviationResult(
                    action = PlayerAction.HIT,
                    isDeviation = true,
                    description = "Deviation (RC < 0): Hit A,4 vs 4"
                )
            }
        }

        return null
    }
}
