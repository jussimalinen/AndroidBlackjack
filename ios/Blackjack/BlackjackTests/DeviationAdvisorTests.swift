import Testing
@testable import Blackjack

@Suite("DeviationAdvisor Tests")
struct DeviationAdvisorTests {

    private func card(_ rank: Rank, _ suit: Suit = .hearts) -> Card { Card(rank: rank, suit: suit) }

    private func hand(_ ranks: Rank...) -> Hand {
        Hand(cards: ranks.map { card($0) })
    }

    private let allActions: Set<PlayerAction> = [.hit, .stand, .doubleDown, .split, .surrender]
    private let hitOrStand: Set<PlayerAction> = [.hit, .stand]

    private let rules = CasinoRules()

    @Test("16 vs 10 hit at 2 cards if surrender available is surrender")
    func sixteenVs10WithSurrenderIsSurrender() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .six), dealerUpCard: card(.ten), availableActions: allActions, rules: rules,
            runningCount: 0, trueCount: 0
        )
        #expect(result.action == .surrender)
        #expect(!result.isDeviation)
    }

    @Test("16 vs 10 hit at zero running count with 2 cards if no surrender")
    func sixteenVs10HitAtZeroRC() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .six), dealerUpCard: card(.ten), availableActions: hitOrStand, rules: rules,
            runningCount: 0, trueCount: 0
        )
        #expect(result.action == .hit)
        #expect(result.isDeviation)
    }

    @Test("16 vs 10 hit at zero running count with 3+ cards")
    func sixteenVs10HitAtZeroRCThreeCards() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.five, .six, .five), dealerUpCard: card(.ten), availableActions: hitOrStand, rules: rules,
            runningCount: 0, trueCount: 0
        )
        #expect(result.action == .hit)
        #expect(result.isDeviation)
    }

    @Test("16 vs 10 hit at negative running count with 2 cards if no surrender")
    func sixteenVs10HitAtNegativeRC() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .six), dealerUpCard: card(.ten), availableActions: hitOrStand, rules: rules,
            runningCount: -1, trueCount: -0.5
        )
        #expect(result.action == .hit)
        #expect(result.isDeviation)
    }

    @Test("16 vs 10 hit at negative running count with 3+ cards")
    func sixteenVs10HitAtNegativeRCThreeCards() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.five, .six, .five), dealerUpCard: card(.ten), availableActions: hitOrStand, rules: rules,
            runningCount: -1, trueCount: -0.5
        )
        #expect(result.action == .hit)
        #expect(result.isDeviation)
    }

    @Test("multi-card 16 vs 10 stand at positive count")
    func multiCard16Vs10StandAtPositiveCount() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.seven, .five, .four), dealerUpCard: card(.ten), availableActions: hitOrStand, rules: rules,
            runningCount: 2, trueCount: 1
        )
        #expect(result.action == .stand)
        #expect(result.isDeviation)
    }

    @Test("multi-card 16 vs 10 stand at positive count on two cards")
    func twoCard16Vs10StandAtPositiveCount() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.six, .ten), dealerUpCard: card(.ten), availableActions: hitOrStand, rules: rules,
            runningCount: 2, trueCount: 1
        )
        #expect(result.action == .stand)
        #expect(result.isDeviation)
    }

    // MARK: - 12 vs 2: Stand at TC >= +3

    @Test("12 vs 2 stand at true count 3")
    func twelveVs2StandAtTC3() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .two), dealerUpCard: card(.two), availableActions: hitOrStand, rules: rules,
            runningCount: 12, trueCount: 3
        )
        #expect(result.action == .stand)
        #expect(result.isDeviation)
    }

    @Test("12 vs 2 hit at true count below 3")
    func twelveVs2HitAtTCBelow3() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .two), dealerUpCard: card(.two), availableActions: hitOrStand, rules: rules,
            runningCount: 5, trueCount: 2.5
        )
        #expect(result.action == .hit)
        #expect(!result.isDeviation)
    }

    // MARK: - 12 vs 3: Stand at TC >= +2

    @Test("12 vs 3 stand at true count 2")
    func twelveVs3StandAtTC2() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .two), dealerUpCard: card(.three), availableActions: hitOrStand, rules: rules,
            runningCount: 8, trueCount: 2
        )
        #expect(result.action == .stand)
        #expect(result.isDeviation)
    }

    @Test("12 vs 3 hit at true count below 2")
    func twelveVs3HitAtTCBelow2() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .two), dealerUpCard: card(.three), availableActions: hitOrStand, rules: rules,
            runningCount: 3, trueCount: 1.5
        )
        #expect(result.action == .hit)
        #expect(!result.isDeviation)
    }

    // MARK: - 12 vs 4: Hit at RC < 0

    @Test("12 vs 4 hit at negative running count")
    func twelveVs4HitAtNegativeRC() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .two), dealerUpCard: card(.four), availableActions: hitOrStand, rules: rules,
            runningCount: -1, trueCount: -0.5
        )
        #expect(result.action == .hit)
        #expect(result.isDeviation)
    }

    @Test("12 vs 4 stand at zero running count")
    func twelveVs4StandAtZeroRC() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .two), dealerUpCard: card(.four), availableActions: hitOrStand, rules: rules,
            runningCount: 0, trueCount: 0
        )
        #expect(result.action == .stand)
        #expect(!result.isDeviation)
    }

    // MARK: - A,4 (soft 15) vs 4: Hit at RC < 0

    @Test("soft 15 vs 4 hit at negative running count")
    func soft15Vs4HitAtNegativeRC() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ace, .four), dealerUpCard: card(.four), availableActions: allActions, rules: rules,
            runningCount: -2, trueCount: -1
        )
        #expect(result.action == .hit)
        #expect(result.isDeviation)
    }

    @Test("soft 15 vs 4 double at zero running count")
    func soft15Vs4DoubleAtZeroRC() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ace, .four), dealerUpCard: card(.four), availableActions: allActions, rules: rules,
            runningCount: 0, trueCount: 0
        )
        #expect(result.action == .doubleDown)
        #expect(!result.isDeviation)
    }

    // MARK: - Insurance deviation

    @Test("insurance deviation at true count 3")
    func insuranceDeviationAtTC3() {
        let result = DeviationAdvisor.insuranceDeviation(trueCount: 3)
        #expect(result != nil)
        #expect(result!.isDeviation)
        #expect(result!.description!.contains("insurance"))
    }

    @Test("insurance deviation at true count above 3")
    func insuranceDeviationAtTCAbove3() {
        let result = DeviationAdvisor.insuranceDeviation(trueCount: 5)
        #expect(result != nil)
        #expect(result!.isDeviation)
    }

    @Test("no insurance deviation at true count below 3")
    func noInsuranceDeviationAtTCBelow3() {
        let result = DeviationAdvisor.insuranceDeviation(trueCount: 2.9)
        #expect(result == nil)
    }

    // MARK: - No deviation: falls back to basic strategy

    @Test("no deviation returns basic strategy action")
    func noDeviationReturnsBasicStrategy() {
        let result = DeviationAdvisor.optimalAction(
            hand: hand(.ten, .seven), dealerUpCard: card(.six), availableActions: hitOrStand, rules: rules,
            runningCount: 5, trueCount: 2
        )
        #expect(result.action == .stand)
        #expect(!result.isDeviation)
        #expect(result.description == nil)
    }
}
