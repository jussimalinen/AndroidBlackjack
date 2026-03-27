import Testing
@testable import Blackjack

@Suite("BasicStrategyAdvisor Tests")
struct BasicStrategyAdvisorTests {

    // MARK: - Helpers

    private func card(_ rank: Rank, _ suit: Suit = .hearts) -> Card { Card(rank: rank, suit: suit) }

    private func hand(_ ranks: Rank...) -> Hand {
        Hand(cards: ranks.map { card($0) })
    }

    private let allActions: Set<PlayerAction> = [.hit, .stand, .doubleDown, .split, .surrender]
    private let noSurrender: Set<PlayerAction> = [.hit, .stand, .doubleDown, .split]
    private let noDouble: Set<PlayerAction> = [.hit, .stand, .split, .surrender]
    private let hitOrStand: Set<PlayerAction> = [.hit, .stand]

    private let standardRules = CasinoRules()
    private let h17Rules = CasinoRules(dealerStandsOnSoft17: false)
    private let enhcRules = CasinoRules(dealerPeeks: false)
    private let noDasRules = CasinoRules(doubleAfterSplit: false)

    private func advise(
        hand: Hand,
        dealer: Rank,
        available: Set<PlayerAction>? = nil,
        rules: CasinoRules? = nil
    ) -> PlayerAction {
        BasicStrategyAdvisor.optimalAction(
            hand: hand,
            dealerUpCard: card(dealer),
            availableActions: available ?? allActions,
            rules: rules ?? standardRules
        )
    }

    // MARK: - Hard totals

    @Test("hard 5 through 8 always hit")
    func hard5Through8AlwaysHit() {
        let dealerRanks: [Rank] = [.two, .five, .seven, .ten, .ace]
        for dealer in dealerRanks {
            #expect(advise(hand: hand(.two, .three), dealer: dealer) == .hit, "Hard 5 vs \(dealer)")
            #expect(advise(hand: hand(.two, .four), dealer: dealer) == .hit, "Hard 6 vs \(dealer)")
            #expect(advise(hand: hand(.two, .five), dealer: dealer) == .hit, "Hard 7 vs \(dealer)")
            #expect(advise(hand: hand(.three, .five), dealer: dealer) == .hit, "Hard 8 vs \(dealer)")
        }
    }

    @Test("hard 9 doubles vs 3 through 6 else hits")
    func hard9DoublesVs3Through6() {
        #expect(advise(hand: hand(.two, .seven), dealer: .two) == .hit)
        #expect(advise(hand: hand(.two, .seven), dealer: .three) == .doubleDown)
        #expect(advise(hand: hand(.two, .seven), dealer: .four) == .doubleDown)
        #expect(advise(hand: hand(.two, .seven), dealer: .five) == .doubleDown)
        #expect(advise(hand: hand(.two, .seven), dealer: .six) == .doubleDown)
        #expect(advise(hand: hand(.two, .seven), dealer: .seven) == .hit)
        #expect(advise(hand: hand(.two, .seven), dealer: .ten) == .hit)
        #expect(advise(hand: hand(.two, .seven), dealer: .ace) == .hit)
    }

    @Test("hard 9 hits when double not available")
    func hard9HitsWhenDoubleNotAvailable() {
        #expect(advise(hand: hand(.two, .seven), dealer: .five, available: hitOrStand) == .hit)
    }

    @Test("hard 10 doubles vs 2 through 9")
    func hard10DoublesVs2Through9() {
        for dealer in [Rank.two, .three, .four, .five, .six, .seven, .eight, .nine] {
            #expect(advise(hand: hand(.four, .six), dealer: dealer) == .doubleDown, "10 vs \(dealer)")
        }
        #expect(advise(hand: hand(.four, .six), dealer: .ten) == .hit)
        #expect(advise(hand: hand(.four, .six), dealer: .ace) == .hit)
    }

    @Test("hard 11 doubles vs 2 through 10 and hits vs A in S17")
    func hard11DoublesVs2Through10() {
        for dealer in [Rank.two, .three, .four, .five, .six, .seven, .eight, .nine, .ten] {
            #expect(advise(hand: hand(.three, .eight), dealer: dealer) == .doubleDown, "11 vs \(dealer)")
        }
        #expect(advise(hand: hand(.three, .eight), dealer: .ace) == .hit)
    }

    @Test("hard 12 stands vs 4-6 hits otherwise")
    func hard12StandsVs4to6() {
        #expect(advise(hand: hand(.four, .eight), dealer: .two) == .hit)
        #expect(advise(hand: hand(.four, .eight), dealer: .three) == .hit)
        #expect(advise(hand: hand(.four, .eight), dealer: .four) == .stand)
        #expect(advise(hand: hand(.four, .eight), dealer: .five) == .stand)
        #expect(advise(hand: hand(.four, .eight), dealer: .six) == .stand)
        #expect(advise(hand: hand(.four, .eight), dealer: .seven) == .hit)
        #expect(advise(hand: hand(.four, .eight), dealer: .ten) == .hit)
        #expect(advise(hand: hand(.four, .eight), dealer: .ace) == .hit)
    }

    @Test("hard 13 through 16 stands vs 2-6")
    func hard13Through16StandsVs2to6() {
        let hands = [hand(.four, .nine), hand(.five, .nine), hand(.six, .nine), hand(.seven, .nine)]
        for h in hands {
            for dealer in [Rank.two, .three, .four, .five, .six] {
                #expect(advise(hand: h, dealer: dealer) == .stand, "\(h.score) vs \(dealer)")
            }
        }
    }

    @Test("hard 13-14 hits vs 7 through A")
    func hard13to14HitsVs7ThroughA() {
        for dealer in [Rank.seven, .eight, .nine, .ten, .ace] {
            #expect(advise(hand: hand(.four, .nine), dealer: dealer) == .hit, "13 vs \(dealer)")
            #expect(advise(hand: hand(.five, .nine), dealer: dealer) == .hit, "14 vs \(dealer)")
        }
    }

    @Test("hard 15 surrenders vs 10 else hits vs 7+")
    func hard15SurrendersVs10() {
        #expect(advise(hand: hand(.six, .nine), dealer: .seven) == .hit)
        #expect(advise(hand: hand(.six, .nine), dealer: .eight) == .hit)
        #expect(advise(hand: hand(.six, .nine), dealer: .nine) == .hit)
        #expect(advise(hand: hand(.six, .nine), dealer: .ten) == .surrender)
        #expect(advise(hand: hand(.six, .nine), dealer: .ace) == .hit)
    }

    @Test("hard 15 hits vs 10 when surrender unavailable")
    func hard15HitsVs10WhenSurrenderUnavailable() {
        #expect(advise(hand: hand(.six, .nine), dealer: .ten, available: noSurrender) == .hit)
    }

    @Test("hard 16 surrenders vs 9 10 A else hits vs 7+")
    func hard16SurrendersVs9_10_A() {
        #expect(advise(hand: hand(.seven, .nine), dealer: .seven) == .hit)
        #expect(advise(hand: hand(.seven, .nine), dealer: .eight) == .hit)
        #expect(advise(hand: hand(.seven, .nine), dealer: .nine) == .surrender)
        #expect(advise(hand: hand(.seven, .nine), dealer: .ten) == .surrender)
        #expect(advise(hand: hand(.seven, .nine), dealer: .ace) == .surrender)
    }

    @Test("hard 17 through 20 always stands")
    func hard17Through20AlwaysStands() {
        let hand17 = hand(.eight, .nine)
        let hand18 = hand(.eight, .ten)
        let hand19 = hand(.ten, .nine)
        let hand20 = hand(.ten, .queen)
        for dealer in [Rank.two, .five, .seven, .ten, .ace] {
            #expect(advise(hand: hand17, dealer: dealer) == .stand)
            #expect(advise(hand: hand18, dealer: dealer) == .stand)
            #expect(advise(hand: hand19, dealer: dealer) == .stand)
            #expect(advise(hand: hand20, dealer: dealer) == .stand)
        }
    }

    // MARK: - Soft totals

    @Test("soft 13-14 doubles vs 5-6 else hits")
    func soft13to14DoublesVs5to6() {
        for h in [hand(.ace, .two), hand(.ace, .three)] {
            #expect(advise(hand: h, dealer: .two) == .hit)
            #expect(advise(hand: h, dealer: .three) == .hit)
            #expect(advise(hand: h, dealer: .four) == .hit)
            #expect(advise(hand: h, dealer: .five) == .doubleDown)
            #expect(advise(hand: h, dealer: .six) == .doubleDown)
            #expect(advise(hand: h, dealer: .seven) == .hit)
            #expect(advise(hand: h, dealer: .ten) == .hit)
            #expect(advise(hand: h, dealer: .ace) == .hit)
        }
    }

    @Test("soft 15-16 doubles vs 4-6 else hits")
    func soft15to16DoublesVs4to6() {
        for h in [hand(.ace, .four), hand(.ace, .five)] {
            #expect(advise(hand: h, dealer: .two) == .hit)
            #expect(advise(hand: h, dealer: .three) == .hit)
            #expect(advise(hand: h, dealer: .four) == .doubleDown)
            #expect(advise(hand: h, dealer: .five) == .doubleDown)
            #expect(advise(hand: h, dealer: .six) == .doubleDown)
            #expect(advise(hand: h, dealer: .seven) == .hit)
            #expect(advise(hand: h, dealer: .ten) == .hit)
        }
    }

    @Test("soft 17 doubles vs 3-6 else hits")
    func soft17DoublesVs3to6() {
        let h = hand(.ace, .six)
        #expect(advise(hand: h, dealer: .two) == .hit)
        #expect(advise(hand: h, dealer: .three) == .doubleDown)
        #expect(advise(hand: h, dealer: .four) == .doubleDown)
        #expect(advise(hand: h, dealer: .five) == .doubleDown)
        #expect(advise(hand: h, dealer: .six) == .doubleDown)
        #expect(advise(hand: h, dealer: .seven) == .hit)
        #expect(advise(hand: h, dealer: .ten) == .hit)
        #expect(advise(hand: h, dealer: .ace) == .hit)
    }

    @Test("soft 18 doubles vs 2-6 stands vs 7-8 hits vs 9+")
    func soft18Strategy() {
        let h = hand(.ace, .seven)
        #expect(advise(hand: h, dealer: .two) == .doubleDown)
        #expect(advise(hand: h, dealer: .three) == .doubleDown)
        #expect(advise(hand: h, dealer: .four) == .doubleDown)
        #expect(advise(hand: h, dealer: .five) == .doubleDown)
        #expect(advise(hand: h, dealer: .six) == .doubleDown)
        #expect(advise(hand: h, dealer: .seven) == .stand)
        #expect(advise(hand: h, dealer: .eight) == .stand)
        #expect(advise(hand: h, dealer: .nine) == .hit)
        #expect(advise(hand: h, dealer: .ten) == .hit)
        #expect(advise(hand: h, dealer: .ace) == .hit)
    }

    @Test("soft 18 stands when double not available vs 2-6")
    func soft18StandsWhenDoubleUnavailable() {
        #expect(advise(hand: hand(.ace, .seven), dealer: .three, available: hitOrStand) == .stand)
    }

    @Test("soft 19 doubles vs 6 else stands")
    func soft19DoublesVs6() {
        let h = hand(.ace, .eight)
        #expect(advise(hand: h, dealer: .two) == .stand)
        #expect(advise(hand: h, dealer: .three) == .stand)
        #expect(advise(hand: h, dealer: .four) == .stand)
        #expect(advise(hand: h, dealer: .six) == .doubleDown)
        #expect(advise(hand: h, dealer: .seven) == .stand)
        #expect(advise(hand: h, dealer: .ten) == .stand)
        #expect(advise(hand: h, dealer: .ace) == .stand)
    }

    @Test("soft 19 stands vs 6 when double not available")
    func soft19StandsVs6WhenDoubleUnavailable() {
        #expect(advise(hand: hand(.ace, .eight), dealer: .six, available: hitOrStand) == .stand)
    }

    @Test("soft 20 always stands")
    func soft20AlwaysStands() {
        let h = hand(.ace, .nine)
        for dealer in [Rank.two, .five, .six, .seven, .ten, .ace] {
            #expect(advise(hand: h, dealer: dealer) == .stand)
        }
    }

    // MARK: - Pairs

    @Test("always split aces")
    func alwaysSplitAces() {
        let h = hand(.ace, .ace)
        for dealer in [Rank.two, .five, .six, .seven, .ten, .ace] {
            #expect(advise(hand: h, dealer: dealer) == .split, "AA vs \(dealer)")
        }
    }

    @Test("always split eights in standard rules")
    func alwaysSplitEights() {
        let h = hand(.eight, .eight)
        for dealer in [Rank.two, .five, .six, .seven, .nine, .ten, .ace] {
            #expect(advise(hand: h, dealer: dealer) == .split, "88 vs \(dealer)")
        }
    }

    @Test("never split tens")
    func neverSplitTens() {
        for tenRank in [Rank.ten, .jack, .queen, .king] {
            let h = hand(tenRank, tenRank)
            for dealer in [Rank.two, .six, .ten, .ace] {
                #expect(advise(hand: h, dealer: dealer) == .stand)
            }
        }
    }

    @Test("fives are treated as hard 10 not pair")
    func fivesTreatedAsHard10() {
        let h = hand(.five, .five)
        #expect(advise(hand: h, dealer: .five) == .doubleDown)
        #expect(advise(hand: h, dealer: .two) == .doubleDown)
        #expect(advise(hand: h, dealer: .ten) == .hit)
    }

    @Test("nines split vs 2-6 8-9 stand vs 7 10 A")
    func ninesStrategy() {
        let h = hand(.nine, .nine)
        #expect(advise(hand: h, dealer: .two) == .split)
        #expect(advise(hand: h, dealer: .three) == .split)
        #expect(advise(hand: h, dealer: .four) == .split)
        #expect(advise(hand: h, dealer: .five) == .split)
        #expect(advise(hand: h, dealer: .six) == .split)
        #expect(advise(hand: h, dealer: .seven) == .stand)
        #expect(advise(hand: h, dealer: .eight) == .split)
        #expect(advise(hand: h, dealer: .nine) == .split)
        #expect(advise(hand: h, dealer: .ten) == .stand)
        #expect(advise(hand: h, dealer: .ace) == .stand)
    }

    @Test("sevens split vs 2-7 hit vs 8+")
    func sevensStrategy() {
        let h = hand(.seven, .seven)
        for dealer in [Rank.two, .three, .four, .five, .six, .seven] {
            #expect(advise(hand: h, dealer: dealer) == .split, "77 vs \(dealer)")
        }
        for dealer in [Rank.eight, .nine, .ten, .ace] {
            #expect(advise(hand: h, dealer: dealer) == .hit, "77 vs \(dealer)")
        }
    }

    @Test("sixes with DAS split vs 2-6 hit otherwise")
    func sixesWithDAS() {
        let h = hand(.six, .six)
        for dealer in [Rank.two, .three, .four, .five, .six] {
            #expect(advise(hand: h, dealer: dealer) == .split, "66 vs \(dealer) DAS")
        }
        for dealer in [Rank.seven, .eight, .ten, .ace] {
            #expect(advise(hand: h, dealer: dealer) == .hit, "66 vs \(dealer) DAS")
        }
    }

    @Test("sixes without DAS split vs 3-6 hit vs 2")
    func sixesWithoutDAS() {
        let h = hand(.six, .six)
        #expect(advise(hand: h, dealer: .two, rules: noDasRules) == .hit)
        for dealer in [Rank.three, .four, .five, .six] {
            #expect(advise(hand: h, dealer: dealer, rules: noDasRules) == .split, "66 vs \(dealer) no DAS")
        }
        #expect(advise(hand: h, dealer: .seven, rules: noDasRules) == .hit)
    }

    @Test("fours with DAS split vs 5-6 else hit")
    func foursWithDAS() {
        let h = hand(.four, .four)
        #expect(advise(hand: h, dealer: .two) == .hit)
        #expect(advise(hand: h, dealer: .three) == .hit)
        #expect(advise(hand: h, dealer: .four) == .hit)
        #expect(advise(hand: h, dealer: .five) == .split)
        #expect(advise(hand: h, dealer: .six) == .split)
        #expect(advise(hand: h, dealer: .seven) == .hit)
    }

    @Test("fours without DAS always hit")
    func foursWithoutDAS() {
        let h = hand(.four, .four)
        for dealer in [Rank.two, .five, .six, .seven, .ten] {
            #expect(advise(hand: h, dealer: dealer, rules: noDasRules) == .hit)
        }
    }

    @Test("threes and twos with DAS split vs 2-7")
    func threesAndTwosWithDAS() {
        for rank in [Rank.three, .two] {
            let h = hand(rank, rank)
            for dealer in [Rank.two, .three, .four, .five, .six, .seven] {
                #expect(advise(hand: h, dealer: dealer) == .split, "\(rank)\(rank) vs \(dealer) DAS")
            }
            #expect(advise(hand: h, dealer: .eight) == .hit)
            #expect(advise(hand: h, dealer: .ten) == .hit)
        }
    }

    @Test("threes and twos without DAS split vs 4-7")
    func threesAndTwosWithoutDAS() {
        for rank in [Rank.three, .two] {
            let h = hand(rank, rank)
            #expect(advise(hand: h, dealer: .two, rules: noDasRules) == .hit)
            #expect(advise(hand: h, dealer: .three, rules: noDasRules) == .hit)
            for dealer in [Rank.four, .five, .six, .seven] {
                #expect(advise(hand: h, dealer: dealer, rules: noDasRules) == .split, "\(rank)\(rank) vs \(dealer) no DAS")
            }
            #expect(advise(hand: h, dealer: .eight, rules: noDasRules) == .hit)
        }
    }

    // MARK: - H17 deviations

    @Test("H17 double 11 vs A")
    func h17Double11VsA() {
        #expect(advise(hand: hand(.three, .eight), dealer: .ace, rules: h17Rules) == .doubleDown)
    }

    @Test("H17 surrender 17 vs A")
    func h17Surrender17VsA() {
        #expect(advise(hand: hand(.eight, .nine), dealer: .ace, rules: h17Rules) == .surrender)
    }

    @Test("H17 surrender 15 vs A")
    func h17Surrender15VsA() {
        #expect(advise(hand: hand(.six, .nine), dealer: .ace, rules: h17Rules) == .surrender)
    }

    @Test("H17 surrender 8-8 vs A")
    func h17Surrender88VsA() {
        #expect(advise(hand: hand(.eight, .eight), dealer: .ace, rules: h17Rules) == .surrender)
    }

    @Test("H17 8-8 vs A splits when surrender unavailable")
    func h17_88VsASplitsWhenSurrenderUnavailable() {
        #expect(advise(hand: hand(.eight, .eight), dealer: .ace, available: noSurrender, rules: h17Rules) == .split)
    }

    // MARK: - ENHC (no hole card) deviations

    @Test("ENHC hit 11 vs 10 instead of double")
    func enhcHit11Vs10() {
        #expect(advise(hand: hand(.three, .eight), dealer: .ten, rules: enhcRules) == .hit)
    }

    @Test("ENHC surrender 14 vs 10")
    func enhcSurrender14Vs10() {
        #expect(advise(hand: hand(.five, .nine), dealer: .ten, rules: enhcRules) == .surrender)
    }

    @Test("ENHC multi-card 16 vs 10 stands")
    func enhcMultiCard16Vs10Stands() {
        let multiCard16 = hand(.four, .six, .six)
        #expect(advise(hand: multiCard16, dealer: .ten, rules: enhcRules) == .stand)
    }

    @Test("ENHC two-card 16 vs 10 surrenders")
    func enhcTwoCard16Vs10Surrenders() {
        #expect(advise(hand: hand(.seven, .nine), dealer: .ten, rules: enhcRules) == .surrender)
    }

    @Test("ENHC hit 16 vs A instead of surrender")
    func enhcHit16VsA() {
        #expect(advise(hand: hand(.seven, .nine), dealer: .ace, rules: enhcRules) == .hit)
    }

    @Test("ENHC hit A-A vs A instead of split")
    func enhcHitAAVsA() {
        #expect(advise(hand: hand(.ace, .ace), dealer: .ace, rules: enhcRules) == .hit)
    }

    @Test("ENHC surrender 8-8 vs 10")
    func enhcSurrender88Vs10() {
        #expect(advise(hand: hand(.eight, .eight), dealer: .ten, rules: enhcRules) == .surrender)
    }

    @Test("ENHC 8-8 vs 10 hits when surrender unavailable")
    func enhc88Vs10HitsWhenSurrenderUnavailable() {
        #expect(advise(hand: hand(.eight, .eight), dealer: .ten, available: noSurrender, rules: enhcRules) == .hit)
    }

    @Test("ENHC hit 8-8 vs A")
    func enhcHit88VsA() {
        #expect(advise(hand: hand(.eight, .eight), dealer: .ace, rules: enhcRules) == .hit)
    }

    @Test("ENHC soft 18 vs 2 stands instead of double")
    func enhcSoft18Vs2Stands() {
        #expect(advise(hand: hand(.ace, .seven), dealer: .two, rules: enhcRules) == .stand)
    }

    @Test("ENHC soft 19 vs 6 stands instead of double")
    func enhcSoft19Vs6Stands() {
        #expect(advise(hand: hand(.ace, .eight), dealer: .six, rules: enhcRules) == .stand)
    }

    // MARK: - Action resolution and fallback

    @Test("returns STAND when no actions available")
    func returnsStandWhenNoActions() {
        #expect(advise(hand: hand(.ten, .seven), dealer: .five, available: Set<PlayerAction>()) == .stand)
    }

    @Test("double falls back to hit when unavailable")
    func doubleFallsBackToHit() {
        #expect(advise(hand: hand(.four, .six), dealer: .five, available: hitOrStand) == .hit)
    }

    @Test("Ds falls back to stand when double unavailable")
    func dsFallsBackToStand() {
        #expect(advise(hand: hand(.ace, .seven), dealer: .three, available: hitOrStand) == .stand)
    }

    @Test("surrender falls back to hit for Rh")
    func surrenderFallsBackToHitForRh() {
        #expect(advise(hand: hand(.seven, .nine), dealer: .nine, available: noSurrender) == .hit)
    }

    @Test("split falls back to hit when unavailable")
    func splitFallsBackToHit() {
        let noSplit: Set<PlayerAction> = [.hit, .stand, .doubleDown, .surrender]
        #expect(advise(hand: hand(.seven, .seven), dealer: .three, available: noSplit) == .hit)
    }

    // MARK: - Edge cases

    @Test("hard total below 5 hits")
    func hardTotalBelow5Hits() {
        let threeCardFour = Hand(cards: [card(.two), card(.two)])
        let noSplit: Set<PlayerAction> = [.hit, .stand]
        #expect(advise(hand: threeCardFour, dealer: .five, available: noSplit) == .hit)
    }

    @Test("hard 21 or above stands")
    func hard21OrAboveStands() {
        let hand21 = hand(.seven, .seven, .seven)
        #expect(advise(hand: hand21, dealer: .five, available: hitOrStand) == .stand)
    }

    @Test("face cards mapped to ten column for dealer upcard")
    func faceCardsMappedToTenColumn() {
        let h = hand(.six, .nine)
        #expect(advise(hand: h, dealer: .ten) == advise(hand: h, dealer: .jack))
        #expect(advise(hand: h, dealer: .ten) == advise(hand: h, dealer: .queen))
        #expect(advise(hand: h, dealer: .ten) == advise(hand: h, dealer: .king))
    }

    @Test("mixed ten-value pair treated as tens")
    func mixedTenValuePairTreatedAsTens() {
        let h = hand(.jack, .queen)
        #expect(advise(hand: h, dealer: .six) == .stand)
    }
}
