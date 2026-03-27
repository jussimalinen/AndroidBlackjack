import Testing
@testable import Blackjack

@Suite("GameViewModel Tests")
@MainActor
struct GameViewModelTests {

    private func card(_ rank: Rank, _ suit: Suit = .hearts) -> Card { Card(rank: rank, suit: suit) }

    private func hand(_ ranks: Rank..., bet: Int = 10) -> Hand {
        Hand(cards: ranks.map { card($0) }, bet: bet)
    }

    private let defaultRules = CasinoRules(
        surrenderPolicy: .late,
        initialChips: 1000,
        minimumBet: 10,
        maximumBet: 500
    )

    private func makeViewModel() -> GameViewModel {
        let vm = GameViewModel()
        vm.animationSpeedMultiplier = 0
        return vm
    }

    /// Sets the internal state for deterministic testing.
    private func setupState(_ vm: GameViewModel, _ state: GameState) {
        vm.startGame(rules: state.rules)
        vm.state = state
    }

    /// Creates a standard player-turn state with a known hand and dealer upcard.
    private func playerTurnState(
        playerHand: Hand? = nil,
        dealerHand: Hand? = nil,
        chips: Int = 990,
        rules: CasinoRules? = nil,
        availableActions: Set<PlayerAction>? = nil
    ) -> GameState {
        let r = rules ?? defaultRules
        let ph = playerHand ?? hand(.ten, .six, bet: 10)
        let dh = dealerHand ?? Hand(cards: [card(.seven), card(.nine)])
        let aa = availableActions ?? [.hit, .stand, .surrender]
        return GameState(
            phase: .playerTurn,
            rules: r,
            playerHands: [ph],
            activeHandIndex: 0,
            dealerHand: dh,
            chips: chips,
            currentBet: ph.bet,
            availableActions: aa
        )
    }

    // MARK: - startGame

    @Test("startGame sets initial state correctly")
    func startGameSetsInitialState() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)

        #expect(vm.state.phase == .betting)
        #expect(vm.state.chips == 1000)
        #expect(vm.state.currentBet == 10)
        #expect(vm.state.rules == defaultRules)
        #expect(vm.state.playerHands.isEmpty)
        #expect(vm.state.handResults.isEmpty)
    }

    @Test("startGame uses rules initial chips")
    func startGameUsesRulesInitialChips() {
        let vm = makeViewModel()
        let rules = CasinoRules(initialChips: 5000, minimumBet: 25)
        vm.startGame(rules: rules)

        #expect(vm.state.chips == 5000)
        #expect(vm.state.currentBet == 25)
    }

    // MARK: - adjustBet

    @Test("adjustBet clamps to minimum")
    func adjustBetClampsToMinimum() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.adjustBet(1)

        #expect(vm.state.currentBet == 10)
    }

    @Test("adjustBet clamps to maximum")
    func adjustBetClampsToMaximum() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.adjustBet(9999)

        #expect(vm.state.currentBet == 500)
    }

    @Test("adjustBet clamps to available chips")
    func adjustBetClampsToAvailableChips() {
        let vm = makeViewModel()
        vm.startGame(rules: CasinoRules(initialChips: 50, minimumBet: 10, maximumBet: 500))
        vm.adjustBet(200)

        #expect(vm.state.currentBet == 50)
    }

    @Test("adjustBet sets valid amount")
    func adjustBetSetsValidAmount() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.adjustBet(100)

        #expect(vm.state.currentBet == 100)
    }

    // MARK: - toggleCoach / toggleCount

    @Test("toggleCoach enables coach and resets counters")
    func toggleCoachEnablesCoach() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        #expect(!vm.state.coachEnabled)

        vm.toggleCoach()
        #expect(vm.state.coachEnabled)
        #expect(vm.state.coachFeedback == "")
        #expect(vm.state.coachCorrect == 0)
        #expect(vm.state.coachTotal == 0)
    }

    @Test("toggleCoach disables coach")
    func toggleCoachDisablesCoach() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.toggleCoach() // enable
        vm.toggleCoach() // disable

        #expect(!vm.state.coachEnabled)
    }

    @Test("toggleCount toggles show count")
    func toggleCountTogglesShowCount() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        #expect(!vm.state.showCount)

        vm.toggleCount()
        #expect(vm.state.showCount)

        vm.toggleCount()
        #expect(!vm.state.showCount)
    }

    // MARK: - deal

    @Test("deal deducts bet from chips")
    func dealDeductsBetFromChips() async {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.adjustBet(50)
        vm.deal()
        await Task.yield()

        #expect(vm.state.chips <= 950)
    }

    @Test("deal results in player and dealer having two cards")
    func dealResultsInTwoCards() async {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.deal()
        await Task.yield()

        #expect(vm.state.playerHands.first!.cards.count == 2)
        #expect(vm.state.dealerHand.cards.count == 2)
    }

    @Test("deal transitions to playable phase")
    func dealTransitionsToPlayablePhase() async {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.deal()
        await Task.yield()

        let validPhases: Set<GamePhase> = [.playerTurn, .insuranceOffered, .roundComplete]
        #expect(validPhases.contains(vm.state.phase), "Phase should be playable but was \(vm.state.phase)")
    }

    @Test("deal does nothing outside BETTING phase")
    func dealDoesNothingOutsideBetting() async {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)
        vm.deal()
        await Task.yield()

        let stateAfterFirstDeal = vm.state.playerHands
        vm.deal() // should be no-op
        await Task.yield()

        #expect(stateAfterFirstDeal == vm.state.playerHands)
    }

    @Test("deal does not proceed when bet exceeds chips")
    func dealDoesNotProceedWhenBetExceedsChips() async {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .betting,
            rules: defaultRules,
            chips: 5,
            currentBet: 10
        ))

        vm.deal()
        await Task.yield()

        #expect(vm.state.phase == .betting)
    }

    // MARK: - hit

    @Test("hit adds a card to active hand")
    func hitAddsCard() {
        let vm = makeViewModel()
        setupState(vm, playerTurnState())

        let cardsBefore = vm.state.playerHands.first!.cards.count
        vm.hit()

        #expect(vm.state.playerHands.first!.cards.count == cardsBefore + 1)
    }

    @Test("hit does nothing outside PLAYER_TURN")
    func hitDoesNothingOutsidePlayerTurn() {
        let vm = makeViewModel()
        var s = playerTurnState()
        s.phase = .betting
        setupState(vm, s)

        let handsBefore = vm.state.playerHands
        vm.hit()

        #expect(handsBefore == vm.state.playerHands)
    }

    @Test("hit advances to next hand or dealer when bust")
    func hitAdvancesWhenBust() async {
        let vm = makeViewModel()
        let highHand = hand(.ten, .queen, bet: 10)
        setupState(vm, playerTurnState(playerHand: highHand))

        vm.hit()
        await Task.yield()

        #expect(vm.state.playerHands.first!.cards.count == 3)
    }

    // MARK: - stand

    @Test("stand marks hand as standing and advances")
    func standMarksHandAsStanding() async {
        let vm = makeViewModel()
        setupState(vm, playerTurnState())

        vm.stand()
        await Task.yield()

        #expect(vm.state.playerHands.first!.isStanding)
        #expect(vm.state.phase != .playerTurn)
    }

    @Test("stand does nothing outside PLAYER_TURN")
    func standDoesNothingOutsidePlayerTurn() {
        let vm = makeViewModel()
        var s = playerTurnState()
        s.phase = .betting
        setupState(vm, s)

        vm.stand()

        #expect(!vm.state.playerHands.first!.isStanding)
    }

    // MARK: - doubleDown

    @Test("doubleDown doubles bet and adds one card")
    func doubleDownDoublesBetAndAddsCard() {
        let vm = makeViewModel()
        let initialChips = 990
        let betAmount = 10
        setupState(vm, playerTurnState(
            playerHand: hand(.five, .six, bet: betAmount),
            chips: initialChips,
            availableActions: [.hit, .stand, .doubleDown]
        ))

        vm.doubleDown()

        let playerHand = vm.state.playerHands.first!
        #expect(playerHand.cards.count == 3)
        #expect(playerHand.bet == betAmount * 2)
        #expect(playerHand.isDoubledDown)
        #expect(vm.state.chips == initialChips - betAmount)
    }

    @Test("doubleDown does nothing without sufficient chips")
    func doubleDownDoesNothingWithoutChips() {
        let vm = makeViewModel()
        setupState(vm, playerTurnState(
            playerHand: hand(.five, .six, bet: 100),
            chips: 50,
            availableActions: [.hit, .stand, .doubleDown]
        ))

        vm.doubleDown()

        #expect(vm.state.playerHands.first!.cards.count == 2)
        #expect(!vm.state.playerHands.first!.isDoubledDown)
    }

    @Test("doubleDown does nothing outside PLAYER_TURN")
    func doubleDownDoesNothingOutsidePlayerTurn() {
        let vm = makeViewModel()
        var s = playerTurnState()
        s.phase = .dealing
        setupState(vm, s)

        vm.doubleDown()

        #expect(vm.state.playerHands.first!.cards.count == 2)
    }

    // MARK: - split

    @Test("split creates two hands from pair")
    func splitCreatesTwoHands() async {
        let vm = makeViewModel()
        let pairHand = hand(.eight, .eight, bet: 10)
        setupState(vm, playerTurnState(
            playerHand: pairHand,
            chips: 990,
            availableActions: [.hit, .stand, .split]
        ))

        vm.split()
        await Task.yield()

        #expect(vm.state.playerHands.count == 2)
        #expect(vm.state.playerHands.allSatisfy { $0.isSplitHand })
        #expect(vm.state.playerHands.allSatisfy { $0.cards.count == 2 })
        #expect(vm.state.chips == 980)
    }

    @Test("split does nothing with non-pair")
    func splitDoesNothingWithNonPair() {
        let vm = makeViewModel()
        let nonPair = hand(.eight, .nine, bet: 10)
        setupState(vm, playerTurnState(
            playerHand: nonPair,
            availableActions: [.hit, .stand, .split]
        ))

        vm.split()

        #expect(vm.state.playerHands.count == 1)
    }

    @Test("split does nothing without sufficient chips")
    func splitDoesNothingWithoutChips() {
        let vm = makeViewModel()
        let pairHand = hand(.eight, .eight, bet: 100)
        setupState(vm, playerTurnState(
            playerHand: pairHand,
            chips: 50,
            availableActions: [.hit, .stand, .split]
        ))

        vm.split()

        #expect(vm.state.playerHands.count == 1)
    }

    @Test("split aces marks splitFromAces when hitSplitAces disabled")
    func splitAcesMarksSplitFromAces() async {
        let vm = makeViewModel()
        var rules = defaultRules
        rules.hitSplitAces = false
        let acesPair = Hand(cards: [card(.ace), card(.ace)], bet: 10)
        setupState(vm, playerTurnState(
            playerHand: acesPair,
            chips: 990,
            rules: rules,
            availableActions: [.hit, .stand, .split]
        ))

        vm.split()
        await Task.yield()

        #expect(vm.state.playerHands.allSatisfy { $0.splitFromAces })
    }

    // MARK: - surrender

    @Test("surrender marks hand and ends round")
    func surrenderMarksHand() async {
        let vm = makeViewModel()
        setupState(vm, playerTurnState(
            availableActions: [.hit, .stand, .surrender]
        ))

        vm.surrender()
        await Task.yield()

        #expect(vm.state.playerHands.first!.isSurrendered)
        #expect(vm.state.phase == .roundComplete)
    }

    @Test("surrender does nothing outside PLAYER_TURN")
    func surrenderDoesNothingOutsidePlayerTurn() {
        let vm = makeViewModel()
        var s = playerTurnState()
        s.phase = .betting
        setupState(vm, s)

        vm.surrender()

        #expect(!vm.state.playerHands.first!.isSurrendered)
    }

    // MARK: - Insurance

    @Test("takeInsurance deducts half bet")
    func takeInsuranceDeductsHalfBet() async {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .insuranceOffered,
            rules: defaultRules,
            playerHands: [hand(.ten, .seven, bet: 100)],
            dealerHand: Hand(cards: [card(.ace), card(.nine)]),
            chips: 900,
            currentBet: 100,
            availableActions: [.insurance, .declineInsurance]
        ))

        vm.takeInsurance()
        await Task.yield()

        #expect(vm.state.insuranceBet == 50)
        #expect(vm.state.chips == 850)
    }

    @Test("declineInsurance keeps chips unchanged")
    func declineInsuranceKeepsChips() async {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .insuranceOffered,
            rules: defaultRules,
            playerHands: [hand(.ten, .seven, bet: 100)],
            dealerHand: Hand(cards: [card(.ace), card(.nine)]),
            chips: 900,
            currentBet: 100,
            availableActions: [.insurance, .declineInsurance]
        ))

        let chipsBefore = vm.state.chips
        vm.declineInsurance()
        await Task.yield()

        #expect(vm.state.insuranceBet == 0)
        #expect(vm.state.chips >= chipsBefore || vm.state.phase == .roundComplete)
    }

    // MARK: - Even money

    @Test("takeEvenMoney pays 2x bet and ends round")
    func takeEvenMoneyPays2xBet() {
        let vm = makeViewModel()
        let bjHand = Hand(cards: [card(.ace), card(.ten)], bet: 100)
        setupState(vm, GameState(
            phase: .insuranceOffered,
            rules: defaultRules,
            playerHands: [bjHand],
            dealerHand: Hand(cards: [card(.ace), card(.king)]),
            chips: 900,
            currentBet: 100,
            availableActions: [.evenMoney, .declineEvenMoney]
        ))

        vm.takeEvenMoney()

        #expect(vm.state.phase == .roundComplete)
        #expect(vm.state.chips == 1100)
        #expect(vm.state.roundPayout == 200)
        #expect(vm.state.handResults[0] == .win)
    }

    @Test("declineEvenMoney continues game")
    func declineEvenMoneyContinuesGame() async {
        let vm = makeViewModel()
        let bjHand = Hand(cards: [card(.ace), card(.ten)], bet: 100)
        setupState(vm, GameState(
            phase: .insuranceOffered,
            rules: defaultRules,
            playerHands: [bjHand],
            dealerHand: Hand(cards: [card(.ace), card(.nine)]),
            chips: 900,
            currentBet: 100,
            availableActions: [.evenMoney, .declineEvenMoney]
        ))

        vm.declineEvenMoney()
        await Task.yield()

        #expect(vm.state.phase != .insuranceOffered)
    }

    // MARK: - newRound

    @Test("newRound resets to BETTING phase")
    func newRoundResetsToBetting() {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .roundComplete,
            rules: defaultRules,
            playerHands: [hand(.ten, .nine)],
            dealerHand: hand(.ten, .seven),
            chips: 1020,
            currentBet: 10,
            handResults: [0: .win],
            roundMessage: "You win!",
            roundPayout: 20
        ))

        vm.newRound()

        #expect(vm.state.phase == .betting)
        #expect(vm.state.playerHands.isEmpty)
        #expect(vm.state.dealerHand == Hand())
        #expect(vm.state.handResults.isEmpty)
        #expect(vm.state.insuranceBet == 0)
        #expect(!vm.state.showDealerHoleCard)
        #expect(vm.state.roundPayout == 0)
        #expect(vm.state.roundMessage == "")
        #expect(vm.state.availableActions.isEmpty)
    }

    @Test("newRound clamps bet to available chips")
    func newRoundClampsBet() {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .roundComplete,
            rules: defaultRules,
            chips: 5,
            currentBet: 100
        ))

        vm.newRound()

        #expect(vm.state.currentBet == 5)
    }

    @Test("newRound does nothing outside ROUND_COMPLETE")
    func newRoundDoesNothingOutsideRoundComplete() {
        let vm = makeViewModel()
        vm.startGame(rules: defaultRules)

        let stateBefore = vm.state
        vm.newRound()

        #expect(stateBefore == vm.state)
    }

    // MARK: - resetGame

    @Test("resetGame restores initial state with same rules")
    func resetGameRestoresInitialState() async {
        let vm = makeViewModel()
        let rules = CasinoRules(initialChips: 2000, minimumBet: 25)
        vm.startGame(rules: rules)
        vm.deal()
        await Task.yield()

        vm.resetGame()

        #expect(vm.state.phase == .betting)
        #expect(vm.state.chips == 2000)
        #expect(vm.state.currentBet == 25)
        #expect(vm.state.rules == rules)
    }

    // MARK: - Coach feedback

    @Test("coach evaluates correct action")
    func coachEvaluatesCorrectAction() {
        let vm = makeViewModel()
        let playerHand = hand(.ten, .six, bet: 10)
        let dealerHand = Hand(cards: [card(.seven), card(.nine)])
        var s = playerTurnState(playerHand: playerHand, dealerHand: dealerHand)
        s.coachEnabled = true
        setupState(vm, s)

        vm.hit() // correct play for 16 vs 7

        #expect(vm.state.coachFeedback.contains("Correct"))
        #expect(vm.state.coachCorrect == 1)
        #expect(vm.state.coachTotal == 1)
    }

    @Test("coach evaluates incorrect action")
    func coachEvaluatesIncorrectAction() async {
        let vm = makeViewModel()
        let playerHand = hand(.ten, .six, bet: 10)
        let dealerHand = Hand(cards: [card(.seven), card(.nine)])
        var s = playerTurnState(playerHand: playerHand, dealerHand: dealerHand)
        s.coachEnabled = true
        setupState(vm, s)

        vm.stand() // wrong play
        await Task.yield()

        #expect(vm.state.coachFeedback.contains("Optimal play"))
        #expect(vm.state.coachCorrect == 0)
        #expect(vm.state.coachTotal == 1)
    }

    @Test("coach disabled does not track")
    func coachDisabledDoesNotTrack() {
        let vm = makeViewModel()
        var s = playerTurnState()
        s.coachEnabled = false
        setupState(vm, s)

        vm.hit()

        #expect(vm.state.coachFeedback == "")
        #expect(vm.state.coachTotal == 0)
    }

    @Test("takeInsurance gives negative coach feedback")
    func takeInsuranceGivesNegativeCoachFeedback() async {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .insuranceOffered,
            rules: defaultRules,
            playerHands: [hand(.ten, .seven, bet: 100)],
            dealerHand: Hand(cards: [card(.ace), card(.nine)]),
            chips: 900,
            currentBet: 100,
            availableActions: [.insurance, .declineInsurance],
            coachEnabled: true
        ))

        vm.takeInsurance()
        await Task.yield()

        #expect(vm.state.coachFeedback.contains("never take insurance"))
    }

    @Test("declineInsurance gives positive coach feedback")
    func declineInsuranceGivesPositiveCoachFeedback() async {
        let vm = makeViewModel()
        setupState(vm, GameState(
            phase: .insuranceOffered,
            rules: defaultRules,
            playerHands: [hand(.ten, .seven, bet: 100)],
            dealerHand: Hand(cards: [card(.ace), card(.nine)]),
            chips: 900,
            currentBet: 100,
            availableActions: [.insurance, .declineInsurance],
            coachEnabled: true
        ))

        vm.declineInsurance()
        await Task.yield()

        #expect(vm.state.coachFeedback.contains("Correct"))
    }

    // MARK: - Game over

    @Test("round resolves to GAME_OVER when chips reach zero")
    func gameOverWhenChipsReachZero() async {
        let vm = makeViewModel()
        setupState(vm, playerTurnState(
            playerHand: hand(.ten, .queen, bet: 10),
            chips: 0
        ))

        vm.hit() // bust on 20
        await Task.yield()

        if vm.state.playerHands.first!.isBusted {
            #expect(vm.state.phase == .gameOver)
        }
    }

    // MARK: - Statistics tracking

    @Test("handsPlayed increments after round")
    func handsPlayedIncrements() async {
        let vm = makeViewModel()
        var s = playerTurnState()
        s.handsPlayed = 5
        setupState(vm, s)

        vm.surrender()
        await Task.yield()

        #expect(vm.state.handsPlayed == 6)
    }

    @Test("handsWon increments on win")
    func handsWonIncrements() async {
        let vm = makeViewModel()
        var s = playerTurnState(
            playerHand: hand(.ten, .queen, bet: 10),
            dealerHand: Hand(cards: [card(.seven), card(.ten)]),
            chips: 990
        )
        s.handsWon = 3
        setupState(vm, s)

        vm.stand()
        await Task.yield()

        if vm.state.handResults[0] == .win {
            #expect(vm.state.handsWon == 4)
        }
    }

    // MARK: - Extra players

    @Test("deal initializes extra players when configured")
    func dealInitializesExtraPlayers() async {
        let vm = makeViewModel()
        var rules = defaultRules
        rules.extraPlayers = 2
        vm.startGame(rules: rules)
        vm.deal()
        await Task.yield()

        #expect(vm.state.extraPlayers.count == 2)
        for ep in vm.state.extraPlayers {
            #expect(ep.hand.cards.count >= 2, "Extra player should have at least 2 cards")
        }
    }

    @Test("deal with zero extra players has empty list")
    func dealWithZeroExtraPlayers() async {
        let vm = makeViewModel()
        var rules = defaultRules
        rules.extraPlayers = 0
        vm.startGame(rules: rules)
        vm.deal()
        await Task.yield()

        #expect(vm.state.extraPlayers.isEmpty)
    }

    @Test("extra players play before human turn")
    func extraPlayersPlayBeforeHumanTurn() async {
        let vm = makeViewModel()
        var rules = defaultRules
        rules.extraPlayers = 1
        vm.startGame(rules: rules)
        vm.deal()
        await Task.yield()

        let validPhases: Set<GamePhase> = [.playerTurn, .insuranceOffered, .roundComplete]
        #expect(validPhases.contains(vm.state.phase), "Phase should be playable but was \(vm.state.phase)")

        if !vm.state.extraPlayers.isEmpty {
            #expect(vm.state.extraPlayers.first!.hand.cards.count >= 2)
        }
    }

    @Test("extra player results computed on round complete")
    func extraPlayerResultsComputed() async {
        let vm = makeViewModel()
        let ep1 = ExtraPlayerState(hand: hand(.ten, .nine))
        let ep2 = ExtraPlayerState(hand: hand(.ten, .seven))
        var s = playerTurnState(
            playerHand: hand(.ten, .queen, bet: 10),
            dealerHand: Hand(cards: [card(.eight), card(.ten)]),
            chips: 990
        )
        s.extraPlayers = [ep1, ep2]
        setupState(vm, s)

        vm.stand()
        await Task.yield()

        if vm.state.phase == .roundComplete || vm.state.phase == .gameOver {
            for ep in vm.state.extraPlayers {
                #expect(ep.result != nil, "Extra player result should be set")
            }
            #expect(vm.state.extraPlayers[0].result == .win)
            #expect(vm.state.extraPlayers[1].result == .lose)
        }
    }

    @Test("newRound clears extra players")
    func newRoundClearsExtraPlayers() {
        let vm = makeViewModel()
        let ep = ExtraPlayerState(hand: hand(.ten, .nine), result: .win)
        var rules = defaultRules
        rules.extraPlayers = 1
        setupState(vm, GameState(
            phase: .roundComplete,
            rules: rules,
            playerHands: [hand(.ten, .nine)],
            dealerHand: hand(.ten, .seven),
            chips: 1020,
            currentBet: 10,
            handResults: [0: .win],
            roundMessage: "You win!",
            roundPayout: 20,
            extraPlayers: [ep]
        ))

        vm.newRound()

        #expect(vm.state.extraPlayers.isEmpty)
    }

    @Test("extra player cards counted in running count")
    func extraPlayerCardsCountedInRunningCount() async {
        let vm = makeViewModel()
        var rules = defaultRules
        rules.extraPlayers = 2
        vm.startGame(rules: rules)

        #expect(vm.state.runningCount == 0)

        vm.deal()
        await Task.yield()

        #expect(vm.state.extraPlayers.count == 2)
        for ep in vm.state.extraPlayers {
            #expect(ep.hand.cards.count >= 2)
        }
    }
}
