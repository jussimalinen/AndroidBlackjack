import Testing
@testable import Blackjack

@Suite("BlackjackEngine Tests")
struct BlackjackEngineTests {

    // MARK: - Helpers

    private func card(_ rank: Rank, _ suit: Suit = .hearts) -> Card { Card(rank: rank, suit: suit) }

    private func hand(
        _ ranks: Rank...,
        bet: Int = 10,
        isSplitHand: Bool = false,
        splitFromAces: Bool = false
    ) -> Hand {
        Hand(
            cards: ranks.map { card($0) },
            bet: bet,
            isSplitHand: isSplitHand,
            splitFromAces: splitFromAces
        )
    }

    private let defaultRules = CasinoRules()
    private let dealerUpCard = Card(rank: .ten, suit: .hearts)

    private func actions(
        hand: Hand,
        playerHands: [Hand]? = nil,
        dealerUpCard: Card? = nil,
        chips: Int = 1000,
        rules: CasinoRules = CasinoRules()
    ) -> Set<PlayerAction> {
        BlackjackEngine.availableActions(
            hand: hand,
            playerHands: playerHands ?? [hand],
            dealerUpCard: dealerUpCard ?? self.dealerUpCard,
            chips: chips,
            rules: rules
        )
    }

    // MARK: - Basic actions: hit and stand

    @Test("basic hand has hit and stand")
    func basicHandHasHitAndStand() {
        let result = actions(hand: hand(.ten, .five))
        #expect(result.contains(.hit))
        #expect(result.contains(.stand))
    }

    @Test("finished hand has no actions")
    func finishedHandHasNoActions() {
        var standing = hand(.ten, .seven)
        standing.isStanding = true
        #expect(actions(hand: standing).isEmpty)
    }

    @Test("busted hand has no actions")
    func bustedHandHasNoActions() {
        let busted = hand(.ten, .eight, .five)
        #expect(actions(hand: busted).isEmpty)
    }

    @Test("blackjack hand has no actions")
    func blackjackHandHasNoActions() {
        let bj = hand(.ace, .ten)
        #expect(actions(hand: bj).isEmpty)
    }

    // MARK: - Double down

    @Test("two-card hand can double down")
    func twoCardHandCanDoubleDown() {
        let result = actions(hand: hand(.five, .six))
        #expect(result.contains(.doubleDown))
    }

    @Test("three-card hand cannot double down")
    func threeCardHandCannotDoubleDown() {
        let result = actions(hand: hand(.three, .four, .five))
        #expect(!result.contains(.doubleDown))
    }

    @Test("cannot double down without sufficient chips")
    func cannotDoubleDownWithoutSufficientChips() {
        let h = hand(.five, .six, bet: 100)
        let result = actions(hand: h, chips: 50)
        #expect(!result.contains(.doubleDown))
    }

    @Test("can double down with exact chips")
    func canDoubleDownWithExactChips() {
        let h = hand(.five, .six, bet: 100)
        let result = actions(hand: h, chips: 100)
        #expect(result.contains(.doubleDown))
    }

    @Test("cannot double after split when DAS disabled")
    func cannotDoubleAfterSplitWhenDASDisabled() {
        let rules = CasinoRules(doubleAfterSplit: false)
        let h = hand(.five, .six, isSplitHand: true)
        let result = actions(hand: h, rules: rules)
        #expect(!result.contains(.doubleDown))
    }

    @Test("can double after split when DAS enabled")
    func canDoubleAfterSplitWhenDASEnabled() {
        let h = hand(.five, .six, isSplitHand: true)
        let result = actions(hand: h)
        #expect(result.contains(.doubleDown))
    }

    @Test("cannot double when doubleOnAnyTwo disabled")
    func cannotDoubleWhenDoubleOnAnyTwoDisabled() {
        let rules = CasinoRules(doubleOnAnyTwo: false)
        let h = hand(.five, .six)
        let result = actions(hand: h, rules: rules)
        #expect(!result.contains(.doubleDown))
    }

    // MARK: - Split

    @Test("pair can split")
    func pairCanSplit() {
        let result = actions(hand: hand(.eight, .eight))
        #expect(result.contains(.split))
    }

    @Test("non-pair cannot split")
    func nonPairCannotSplit() {
        let result = actions(hand: hand(.eight, .nine))
        #expect(!result.contains(.split))
    }

    @Test("cannot split at max split hands")
    func cannotSplitAtMaxSplitHands() {
        let h = hand(.eight, .eight)
        let rules = CasinoRules(maxSplitHands: 2)
        let allHands = [h, hand(.ten, .five)]
        let result = actions(hand: h, playerHands: allHands, rules: rules)
        #expect(!result.contains(.split))
    }

    @Test("can split when under max split hands")
    func canSplitWhenUnderMaxSplitHands() {
        let h = hand(.eight, .eight)
        let rules = CasinoRules(maxSplitHands: 4)
        let allHands = [h, hand(.ten, .five)]
        let result = actions(hand: h, playerHands: allHands, rules: rules)
        #expect(result.contains(.split))
    }

    @Test("cannot split without sufficient chips")
    func cannotSplitWithoutSufficientChips() {
        let h = hand(.eight, .eight, bet: 100)
        let result = actions(hand: h, chips: 50)
        #expect(!result.contains(.split))
    }

    @Test("cannot resplit aces when rule disabled")
    func cannotResplitAcesWhenRuleDisabled() {
        let rules = CasinoRules(resplitAces: false)
        let h = hand(.ace, .ace, isSplitHand: true)
        let allHands = [h, hand(.ace, .ten, isSplitHand: true)]
        let result = actions(hand: h, playerHands: allHands, rules: rules)
        #expect(!result.contains(.split))
    }

    @Test("can resplit aces when rule enabled")
    func canResplitAcesWhenRuleEnabled() {
        let rules = CasinoRules(resplitAces: true)
        let h = hand(.ace, .ace, isSplitHand: true)
        let allHands = [h, hand(.ace, .ten, isSplitHand: true)]
        let result = actions(hand: h, playerHands: allHands, rules: rules)
        #expect(result.contains(.split))
    }

    @Test("non-ace pair on split hand can resplit")
    func nonAcePairOnSplitHandCanResplit() {
        let h = hand(.eight, .eight, isSplitHand: true)
        let allHands = [h, hand(.eight, .ten, isSplitHand: true)]
        let result = actions(hand: h, playerHands: allHands)
        #expect(result.contains(.split))
    }

    // MARK: - Split aces restrictions

    @Test("split aces can only stand when hitSplitAces disabled")
    func splitAcesCanOnlyStandWhenHitSplitAcesDisabled() {
        let rules = CasinoRules(hitSplitAces: false)
        let h = Hand(cards: [card(.ace)], bet: 10, splitFromAces: true)
        let result = actions(hand: h, rules: rules)
        #expect(result == [.stand])
    }

    @Test("split aces can hit when hitSplitAces enabled")
    func splitAcesCanHitWhenHitSplitAcesEnabled() {
        let rules = CasinoRules(hitSplitAces: true)
        let h = Hand(cards: [card(.ace)], bet: 10, splitFromAces: true)
        let result = actions(hand: h, rules: rules)
        #expect(result.contains(.hit))
        #expect(result.contains(.stand))
    }

    @Test("split aces with two cards is finished")
    func splitAcesWithTwoCardsIsFinished() {
        let h = Hand(
            cards: [card(.ace), card(.five)],
            bet: 10,
            splitFromAces: true
        )
        #expect(actions(hand: h).isEmpty)
    }

    // MARK: - Surrender

    @Test("late surrender available on initial two-card hand")
    func lateSurrenderAvailable() {
        let rules = CasinoRules(surrenderPolicy: .late)
        let result = actions(hand: hand(.ten, .six), rules: rules)
        #expect(result.contains(.surrender))
    }

    @Test("early surrender available on initial two-card hand")
    func earlySurrenderAvailable() {
        let rules = CasinoRules(surrenderPolicy: .early)
        let result = actions(hand: hand(.ten, .six), rules: rules)
        #expect(result.contains(.surrender))
    }

    @Test("no surrender when policy is none")
    func noSurrenderWhenPolicyIsNone() {
        let result = actions(hand: hand(.ten, .six))
        #expect(!result.contains(.surrender))
    }

    @Test("cannot surrender after hit")
    func cannotSurrenderAfterHit() {
        let rules = CasinoRules(surrenderPolicy: .late)
        let h = hand(.four, .five, .seven)
        let result = actions(hand: h, rules: rules)
        #expect(!result.contains(.surrender))
    }

    @Test("cannot surrender on split hand")
    func cannotSurrenderOnSplitHand() {
        let rules = CasinoRules(surrenderPolicy: .late)
        let h = hand(.ten, .six, isSplitHand: true)
        let allHands = [h, hand(.ten, .five, isSplitHand: true)]
        let result = actions(hand: h, playerHands: allHands, rules: rules)
        #expect(!result.contains(.surrender))
    }

    @Test("cannot surrender when multiple hands exist")
    func cannotSurrenderWhenMultipleHandsExist() {
        let rules = CasinoRules(surrenderPolicy: .late)
        let h = hand(.ten, .six)
        let allHands = [h, hand(.ten, .five)]
        let result = actions(hand: h, playerHands: allHands, rules: rules)
        #expect(!result.contains(.surrender))
    }

    // MARK: - Insurance actions

    @Test("player with blackjack gets even money option")
    func playerWithBlackjackGetsEvenMoneyOption() {
        let bj = hand(.ace, .ten)
        let result = BlackjackEngine.insuranceActions(hand: bj, chips: 1000)
        #expect(result.contains(.evenMoney))
        #expect(result.contains(.declineEvenMoney))
        #expect(!result.contains(.insurance))
    }

    @Test("player without blackjack gets insurance option")
    func playerWithoutBlackjackGetsInsuranceOption() {
        let h = hand(.ten, .seven)
        let result = BlackjackEngine.insuranceActions(hand: h, chips: 1000)
        #expect(result.contains(.insurance))
        #expect(result.contains(.declineInsurance))
        #expect(!result.contains(.evenMoney))
    }

    @Test("cannot take insurance without sufficient chips")
    func cannotTakeInsuranceWithoutSufficientChips() {
        let h = hand(.ten, .seven, bet: 100)
        let result = BlackjackEngine.insuranceActions(hand: h, chips: 40)
        #expect(!result.contains(.insurance))
        #expect(result.contains(.declineInsurance))
    }

    @Test("can take insurance with exact chips")
    func canTakeInsuranceWithExactChips() {
        let h = hand(.ten, .seven, bet: 100)
        let result = BlackjackEngine.insuranceActions(hand: h, chips: 50)
        #expect(result.contains(.insurance))
    }

    // MARK: - Combined scenarios

    @Test("initial pair has all standard actions with late surrender")
    func initialPairHasAllStandardActions() {
        let rules = CasinoRules(surrenderPolicy: .late)
        let h = hand(.eight, .eight)
        let result = actions(hand: h, rules: rules)
        #expect(result == [.hit, .stand, .doubleDown, .split, .surrender])
    }

    @Test("three-card non-pair hand has only hit and stand")
    func threeCardNonPairHasOnlyHitAndStand() {
        let h = hand(.three, .four, .five)
        let result = actions(hand: h)
        #expect(result == [.hit, .stand])
    }
}
