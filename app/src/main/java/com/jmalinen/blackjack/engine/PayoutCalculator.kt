package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.Hand
import com.jmalinen.blackjack.model.HandResult

object PayoutCalculator {

    data class RoundResult(
        val handResults: Map<Int, HandResult>,
        val totalPayout: Int,
        val insurancePayout: Int
    )

    fun calculateResults(
        playerHands: List<Hand>,
        dealerHand: Hand,
        insuranceBet: Int,
        rules: CasinoRules
    ): RoundResult {
        var totalPayout = 0
        var insurancePayout = 0
        val results = mutableMapOf<Int, HandResult>()

        if (insuranceBet > 0 && dealerHand.isBlackjack) {
            insurancePayout = insuranceBet * 3
            totalPayout += insurancePayout
        }

        for ((index, hand) in playerHands.withIndex()) {
            val result: HandResult
            val payout: Int

            when {
                hand.isSurrendered -> {
                    result = HandResult.SURRENDER
                    payout = hand.bet / 2
                }
                hand.isBusted -> {
                    result = HandResult.BUST
                    payout = 0
                }
                hand.isBlackjack && !dealerHand.isBlackjack -> {
                    result = HandResult.BLACKJACK
                    payout = hand.bet + (hand.bet * rules.blackjackPayout.multiplier).toInt()
                }
                hand.isBlackjack && dealerHand.isBlackjack -> {
                    result = HandResult.PUSH
                    payout = hand.bet
                }
                !hand.isBlackjack && dealerHand.isBlackjack -> {
                    result = HandResult.LOSE
                    payout = 0
                }
                dealerHand.isBusted -> {
                    result = HandResult.WIN
                    payout = hand.bet * 2
                }
                hand.score > dealerHand.score -> {
                    result = HandResult.WIN
                    payout = hand.bet * 2
                }
                hand.score == dealerHand.score -> {
                    result = HandResult.PUSH
                    payout = hand.bet
                }
                else -> {
                    result = HandResult.LOSE
                    payout = 0
                }
            }
            results[index] = result
            totalPayout += payout
        }

        return RoundResult(results, totalPayout, insurancePayout)
    }
}
