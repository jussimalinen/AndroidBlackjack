enum DealerStrategy {

    static func shouldHit(hand: Hand, rules: CasinoRules) -> Bool {
        let score = hand.score
        if score < 17 { return true }
        if score > 17 { return false }
        // score == 17
        return rules.dealerStandsOnSoft17 ? false : hand.isSoft
    }
}
