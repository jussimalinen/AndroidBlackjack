enum BlackjackEngine {

    static func availableActions(
        hand: Hand,
        playerHands: [Hand],
        dealerUpCard: Card?,
        chips: Int,
        rules: CasinoRules
    ) -> Set<PlayerAction> {
        var actions = Set<PlayerAction>()

        if hand.isFinished { return actions }

        if hand.splitFromAces && !rules.hitSplitAces {
            actions.insert(.stand)
            return actions
        }

        actions.insert(.hit)
        actions.insert(.stand)

        if hand.cards.count == 2 && chips >= hand.bet {
            if rules.doubleOnAnyTwo {
                if !hand.isSplitHand || rules.doubleAfterSplit {
                    actions.insert(.doubleDown)
                }
            }
        }

        if hand.isPair && playerHands.count < rules.maxSplitHands && chips >= hand.bet {
            if hand.cards.first!.rank.isAce && hand.isSplitHand && !rules.resplitAces {
                // cannot resplit aces
            } else {
                actions.insert(.split)
            }
        }

        if hand.cards.count == 2 && !hand.isSplitHand && playerHands.count == 1 {
            switch rules.surrenderPolicy {
            case .late, .early:
                actions.insert(.surrender)
            case .none:
                break
            }
        }

        return actions
    }

    static func insuranceActions(hand: Hand, chips: Int) -> Set<PlayerAction> {
        var actions = Set<PlayerAction>()
        if hand.isBlackjack {
            actions.insert(.evenMoney)
            actions.insert(.declineEvenMoney)
        } else {
            if chips >= hand.bet / 2 {
                actions.insert(.insurance)
            }
            actions.insert(.declineInsurance)
        }
        return actions
    }
}
