struct DeviationResult: Equatable {
    let action: PlayerAction
    let isDeviation: Bool
    let description: String?
}

/// Count-based deviation advisor. Wraps BasicStrategyAdvisor with Hi-Lo
/// index overrides.
enum DeviationAdvisor {

    static func optimalAction(
        hand: Hand,
        dealerUpCard: Card,
        availableActions: Set<PlayerAction>,
        rules: CasinoRules,
        runningCount: Int,
        trueCount: Float
    ) -> DeviationResult {
        let basicAction = BasicStrategyAdvisor.optimalAction(
            hand: hand, dealerUpCard: dealerUpCard, availableActions: availableActions, rules: rules
        )
        if let deviation = checkDeviation(
            hand: hand, dealerUpCard: dealerUpCard, availableActions: availableActions,
            runningCount: runningCount, trueCount: trueCount
        ) {
            return deviation
        }
        return DeviationResult(action: basicAction, isDeviation: false, description: nil)
    }

    /// Returns a take-insurance deviation when TC >= +3, nil otherwise.
    static func insuranceDeviation(trueCount: Float) -> DeviationResult? {
        if trueCount >= 3 {
            return DeviationResult(
                action: .hit, // placeholder — caller handles insurance action
                isDeviation: true,
                description: "Deviation (TC ≥ +3): Take insurance"
            )
        }
        return nil
    }

    private static func checkDeviation(
        hand: Hand,
        dealerUpCard: Card,
        availableActions: Set<PlayerAction>,
        runningCount: Int,
        trueCount: Float
    ) -> DeviationResult? {
        let dealerRank = dealerUpCard.rank
        let score = hand.score

        // 16 vs 10: Stand at RC > 0 (basic says Hit)
        if score == 16 && !hand.isSoft && dealerRank.isTenValue && runningCount > 0 {
            if availableActions.contains(.stand) {
                if hand.cards.count > 2 || !availableActions.contains(.surrender) {
                    return DeviationResult(
                        action: .stand,
                        isDeviation: true,
                        description: "Deviation (RC > 0): Stand 16 vs 10"
                    )
                }
            }
        }
        // 16 vs 10: Hit at RC <= 0 (basic says Hit)
        if score == 16 && !hand.isSoft && dealerRank.isTenValue && runningCount <= 0 {
            if availableActions.contains(.hit) {
                if hand.cards.count > 2 || !availableActions.contains(.surrender) {
                    return DeviationResult(
                        action: .hit,
                        isDeviation: true,
                        description: "Deviation (RC <= 0): Hit 16 vs 10"
                    )
                }
            }
        }

        // 12 vs 2: Stand at TC >= +3 (basic says Hit)
        if score == 12 && !hand.isSoft && dealerRank == .two && trueCount >= 3 {
            if availableActions.contains(.stand) {
                return DeviationResult(
                    action: .stand,
                    isDeviation: true,
                    description: "Deviation (TC ≥ +3): Stand 12 vs 2"
                )
            }
        }

        // 12 vs 3: Stand at TC >= +2 (basic says Hit)
        if score == 12 && !hand.isSoft && dealerRank == .three && trueCount >= 2 {
            if availableActions.contains(.stand) {
                return DeviationResult(
                    action: .stand,
                    isDeviation: true,
                    description: "Deviation (TC ≥ +2): Stand 12 vs 3"
                )
            }
        }

        // 12 vs 4: Hit at RC < 0 (basic says Stand)
        if score == 12 && !hand.isSoft && dealerRank == .four && runningCount < 0 {
            if availableActions.contains(.hit) {
                return DeviationResult(
                    action: .hit,
                    isDeviation: true,
                    description: "Deviation (RC < 0): Hit 12 vs 4"
                )
            }
        }

        // A,4 (soft 15) vs 4: Hit at RC < 0 (basic says Double)
        if score == 15 && hand.isSoft && dealerRank == .four && runningCount < 0 {
            if availableActions.contains(.hit) {
                return DeviationResult(
                    action: .hit,
                    isDeviation: true,
                    description: "Deviation (RC < 0): Hit A,4 vs 4"
                )
            }
        }

        return nil
    }
}
