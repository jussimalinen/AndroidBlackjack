import Observation

@Observable
class SettingsViewModel {

    var rules = CasinoRules()

    func updateNumberOfDecks(_ count: Int) {
        rules.numberOfDecks = max(1, min(count, 8))
    }

    func toggleDealerStandsOnSoft17(_ value: Bool) {
        rules.dealerStandsOnSoft17 = value
    }

    func toggleDealerPeeks(_ value: Bool) {
        rules.dealerPeeks = value
    }

    func setBlackjackPayout(_ payout: BlackjackPayout) {
        rules.blackjackPayout = payout
    }

    func setSurrenderPolicy(_ policy: SurrenderPolicy) {
        rules.surrenderPolicy = policy
    }

    func toggleDoubleAfterSplit(_ value: Bool) {
        rules.doubleAfterSplit = value
    }

    func toggleResplitAces(_ value: Bool) {
        rules.resplitAces = value
    }

    func setMaxSplitHands(_ count: Int) {
        rules.maxSplitHands = max(2, min(count, 4))
    }

    func toggleHitSplitAces(_ value: Bool) {
        rules.hitSplitAces = value
    }

    func toggleInsurance(_ value: Bool) {
        rules.insuranceAvailable = value
    }

    func toggleThreeSevensBonus(_ value: Bool) {
        rules.threeSevensPays3to1 = value
    }

    func toggleTrainSoftHands(_ value: Bool) {
        rules.trainSoftHands = value
    }

    func toggleTrainPairedHands(_ value: Bool) {
        rules.trainPairedHands = value
    }

    func setExtraPlayers(_ count: Int) {
        rules.extraPlayers = max(0, min(count, 2))
    }

    func applyPreset(_ preset: String) {
        switch preset {
        case "Vegas":
            rules = CasinoRules(
                dealerStandsOnSoft17: false,
                surrenderPolicy: .late,
                insuranceAvailable: true
            )
        case "European":
            rules = CasinoRules(
                dealerPeeks: false,
                insuranceAvailable: false
            )
        case "Favorable":
            rules = CasinoRules(
                numberOfDecks: 1,
                surrenderPolicy: .early,
                resplitAces: true,
                hitSplitAces: true,
                insuranceAvailable: true
            )
        case "Helsinki":
            rules = CasinoRules(
                dealerPeeks: false,
                surrenderPolicy: .early,
                resplitAces: true,
                insuranceAvailable: true,
                threeSevensPays3to1: true
            )
        default:
            rules = CasinoRules()
        }
    }
}
