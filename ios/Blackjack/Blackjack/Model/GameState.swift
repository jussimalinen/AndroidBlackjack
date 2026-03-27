struct ExtraPlayerState: Equatable {
    var hand: Hand = Hand()
    var result: HandResult? = nil
}

struct GameState: Equatable {
    var phase: GamePhase = .betting
    var rules: CasinoRules = CasinoRules()
    var playerHands: [Hand] = []
    var activeHandIndex: Int = 0
    var dealerHand: Hand = Hand()
    var chips: Int = 1000
    var currentBet: Int = 10
    var insuranceBet: Int = 0
    var availableActions: Set<PlayerAction> = []
    var handResults: [Int: HandResult] = [:]
    var roundMessage: String = ""
    var showDealerHoleCard: Bool = false
    var roundPayout: Int = 0
    var handsPlayed: Int = 0
    var handsWon: Int = 0
    var coachEnabled: Bool = false
    var coachFeedback: String = ""
    var coachCorrect: Int = 0
    var coachTotal: Int = 0
    var deviationsEnabled: Bool = false
    var shoePenetration: Float = 0
    var runningCount: Int = 0
    var trueCount: Float = 0
    var showCount: Bool = false
    var extraPlayers: [ExtraPlayerState] = []

    var activeHand: Hand? {
        activeHandIndex < playerHands.count ? playerHands[activeHandIndex] : nil
    }

    var totalBetOnTable: Int {
        playerHands.reduce(0) { $0 + $1.bet } + insuranceBet
    }
}
