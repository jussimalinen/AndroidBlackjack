enum PayoutCalculator {

    struct RoundResult: Equatable {
        let handResults: [Int: HandResult]
        let totalPayout: Int
        let insurancePayout: Int
    }

    static func calculateResults(
        playerHands: [Hand],
        dealerHand: Hand,
        insuranceBet: Int,
        rules: CasinoRules
    ) -> RoundResult {
        var totalPayout = 0
        var insurancePayout = 0
        var results = [Int: HandResult]()

        if insuranceBet > 0 && dealerHand.isBlackjack {
            insurancePayout = insuranceBet * 3
            totalPayout += insurancePayout
        }

        for (index, hand) in playerHands.enumerated() {
            let result: HandResult
            let payout: Int

            let isThreeSevens = rules.threeSevensPays3to1 &&
                hand.cards.count == 3 &&
                hand.cards.allSatisfy { $0.rank == .seven }

            if hand.isSurrendered {
                result = .surrender
                payout = hand.bet / 2
            } else if hand.isBusted {
                result = .bust
                payout = 0
            } else if isThreeSevens {
                result = .threeSevens
                payout = hand.bet * 4
            } else if hand.isBlackjack && !dealerHand.isBlackjack {
                result = .blackjack
                payout = hand.bet + Int(Float(hand.bet) * rules.blackjackPayout.multiplier)
            } else if hand.isBlackjack && dealerHand.isBlackjack {
                result = .push
                payout = hand.bet
            } else if !hand.isBlackjack && dealerHand.isBlackjack {
                result = .lose
                payout = 0
            } else if dealerHand.isBusted {
                result = .win
                payout = hand.bet * 2
            } else if hand.score > dealerHand.score {
                result = .win
                payout = hand.bet * 2
            } else if hand.score == dealerHand.score {
                result = .push
                payout = hand.bet
            } else {
                result = .lose
                payout = 0
            }

            results[index] = result
            totalPayout += payout
        }

        return RoundResult(handResults: results, totalPayout: totalPayout, insurancePayout: insurancePayout)
    }
}
