struct CasinoRules: Equatable {
    var numberOfDecks: Int = 6
    var dealerStandsOnSoft17: Bool = true
    var dealerPeeks: Bool = true
    var blackjackPayout: BlackjackPayout = .threeToTwo
    var surrenderPolicy: SurrenderPolicy = .none
    var doubleAfterSplit: Bool = true
    var resplitAces: Bool = false
    var maxSplitHands: Int = 4
    var hitSplitAces: Bool = false
    var doubleOnAnyTwo: Bool = true
    var insuranceAvailable: Bool = true
    var threeSevensPays3to1: Bool = false
    var initialChips: Int = 1000
    var minimumBet: Int = 10
    var maximumBet: Int = 500
    var trainSoftHands: Bool = false
    var trainPairedHands: Bool = false
    var extraPlayers: Int = 0

    var isTrainingMode: Bool { trainSoftHands || trainPairedHands }
}

enum BlackjackPayout: Equatable {
    case threeToTwo
    case sixToFive

    var multiplier: Float {
        switch self {
        case .threeToTwo: 1.5
        case .sixToFive: 1.2
        }
    }

    var displayName: String {
        switch self {
        case .threeToTwo: "3:2"
        case .sixToFive: "6:5"
        }
    }
}

enum SurrenderPolicy: Equatable {
    case none
    case late
    case early

    var displayName: String {
        switch self {
        case .none: "No Surrender"
        case .late: "Late Surrender"
        case .early: "Early Surrender"
        }
    }
}
