import Observation
import Foundation

@Observable
@MainActor
class GameViewModel {

    // internal(set) so tests can set state directly for deterministic scenarios
    internal(set) var state = GameState()

    private var shoe = Shoe(numberOfDecks: 6)
    private var dealTask: Task<Void, Never>?
    private var runningCount = 0
    private var pendingHoleCardValue = 0

    // Animation delays — internal so tests can override to 0
    var animationSpeedMultiplier: Double = 1.0

    private static let dealCardDelay: UInt64 = 300
    private static let dealerDrawDelay: UInt64 = 500
    private static let extraPlayerDelay: UInt64 = 400
    private static let holeCardRevealDelay: UInt64 = 400

    private func delay(_ ms: UInt64) async throws {
        let adjusted = UInt64(Double(ms) * animationSpeedMultiplier)
        if adjusted > 0 {
            try await Task.sleep(for: .milliseconds(adjusted))
        }
    }

    private func updateCountState() {
        let decksRemaining = max(Float(shoe.cardsRemaining) / 52.0, 0.5)
        let trueCount = Float(runningCount) / decksRemaining
        state.shoePenetration = shoe.penetration
        state.runningCount = runningCount
        state.trueCount = trueCount
    }

    private func drawAndCount() -> Card {
        let card = shoe.draw()
        runningCount += card.rank.hiLoValue
        updateCountState()
        return card
    }

    private func countHoleCard() {
        runningCount += pendingHoleCardValue
        pendingHoleCardValue = 0
        updateCountState()
    }

    func startGame(rules: CasinoRules) {
        dealTask?.cancel()
        shoe = Shoe(numberOfDecks: rules.numberOfDecks)
        runningCount = 0
        state = GameState(
            phase: .betting,
            rules: rules,
            chips: rules.initialChips,
            currentBet: rules.minimumBet
        )
    }

    func adjustBet(_ newBet: Int) {
        let clamped = max(state.rules.minimumBet, min(newBet, min(state.rules.maximumBet, state.chips)))
        state.currentBet = clamped
    }

    func toggleCoach() {
        state.coachEnabled = !state.coachEnabled
        state.coachFeedback = ""
        state.coachCorrect = 0
        state.coachTotal = 0
    }

    func toggleCount() {
        state.showCount = !state.showCount
    }

    func toggleDeviations() {
        state.deviationsEnabled = !state.deviationsEnabled
    }

    private func evaluateCoach(_ chosenAction: PlayerAction) {
        guard state.coachEnabled else { return }
        guard let hand = state.activeHand else { return }
        guard let dealerUpCard = state.dealerHand.cards.first else { return }

        let feedback: String
        let optimal: PlayerAction

        if state.deviationsEnabled {
            let result = DeviationAdvisor.optimalAction(
                hand: hand,
                dealerUpCard: dealerUpCard,
                availableActions: state.availableActions,
                rules: state.rules,
                runningCount: runningCount,
                trueCount: state.trueCount
            )
            optimal = result.action
            let correct = chosenAction == optimal
            if result.isDeviation {
                feedback = correct
                    ? "Correct! \(result.description ?? "")"
                    : "\(result.description ?? "") (you chose \(chosenAction.displayName))"
            } else {
                feedback = correct
                    ? "Correct! \(chosenAction.displayName) was optimal."
                    : "Optimal play: \(optimal.displayName) (you chose \(chosenAction.displayName))"
            }
        } else {
            optimal = BasicStrategyAdvisor.optimalAction(
                hand: hand,
                dealerUpCard: dealerUpCard,
                availableActions: state.availableActions,
                rules: state.rules
            )
            let correct = chosenAction == optimal
            feedback = correct
                ? "Correct! \(chosenAction.displayName) was optimal."
                : "Optimal play: \(optimal.displayName) (you chose \(chosenAction.displayName))"
        }

        let correct = chosenAction == optimal
        state.coachFeedback = feedback
        state.coachCorrect += correct ? 1 : 0
        state.coachTotal += 1
    }

    func deal() {
        guard state.phase == .betting else { return }
        guard state.currentBet <= state.chips else { return }

        if state.rules.isTrainingMode || shoe.needsReshuffle() {
            shoe.shuffle()
            runningCount = 0
            state.shoePenetration = 0
            state.runningCount = 0
            state.trueCount = 0
        }

        // Draw player cards — forced hand type in training mode
        let playerCard1: Card
        let playerCard2: Card
        if state.rules.isTrainingMode {
            let dealSoft: Bool
            if state.rules.trainSoftHands && state.rules.trainPairedHands {
                dealSoft = Double.random(in: 0..<1) < 0.5
            } else {
                dealSoft = state.rules.trainSoftHands
            }
            if dealSoft {
                playerCard1 = shoe.drawMatching { $0.rank.isAce }
                playerCard2 = shoe.drawMatching { !$0.rank.isAce && !$0.rank.isTenValue }
            } else {
                let rank = Rank.allCases.randomElement()!
                playerCard1 = shoe.drawMatching { $0.rank == rank }
                playerCard2 = shoe.drawMatching { $0.rank == rank }
            }
        } else {
            playerCard1 = drawAndCount()
            playerCard2 = drawAndCount()
        }

        // Pre-draw extra player cards
        let extraCount = state.rules.extraPlayers
        let extraCards = (0..<extraCount).map { _ in (drawAndCount(), drawAndCount()) }

        let dealerCard1 = drawAndCount()
        let dealerCard2 = shoe.draw() // hole card — counted when revealed
        pendingHoleCardValue = dealerCard2.rank.hiLoValue

        let currentBet = state.currentBet

        // Start with empty hands, transition to DEALING
        state.phase = .dealing
        state.playerHands = [Hand(bet: currentBet)]
        state.activeHandIndex = 0
        state.dealerHand = Hand()
        state.chips -= currentBet
        state.insuranceBet = 0
        state.handResults = [:]
        state.showDealerHoleCard = false
        state.roundPayout = 0
        state.roundMessage = ""
        state.extraPlayers = Array(repeating: ExtraPlayerState(), count: extraCount)

        // Animate cards one at a time in casino order
        dealTask?.cancel()
        dealTask = Task {
            do {
                // Round 1: extra players card 1, then player card 1, then dealer card 1
                for i in 0..<extraCount {
                    try await delay(Self.dealCardDelay)
                    state.extraPlayers[i].hand = state.extraPlayers[i].hand.addCard(extraCards[i].0)
                }

                // Player card 1
                try await delay(Self.dealCardDelay)
                state.playerHands[0] = state.playerHands[0].addCard(playerCard1)

                // Dealer card 1 (face up)
                try await delay(Self.dealCardDelay)
                state.dealerHand = state.dealerHand.addCard(dealerCard1)

                // Round 2: extra players card 2, then player card 2, then dealer card 2
                for i in 0..<extraCount {
                    try await delay(Self.dealCardDelay)
                    state.extraPlayers[i].hand = state.extraPlayers[i].hand.addCard(extraCards[i].1)
                }

                // Player card 2
                try await delay(Self.dealCardDelay)
                state.playerHands[0] = state.playerHands[0].addCard(playerCard2)

                // Dealer card 2 (face down — hidden by showDealerHoleCard=false)
                try await delay(Self.dealCardDelay)
                state.dealerHand = state.dealerHand.addCard(dealerCard2)

                afterDeal()
            } catch {
                // Task was cancelled
            }
        }
    }

    private func afterDeal() {
        guard let dealerUpCard = state.dealerHand.cards.first else { return }

        // Check for insurance opportunity
        if dealerUpCard.rank.isAce && state.rules.insuranceAvailable {
            let actions = BlackjackEngine.insuranceActions(hand: state.playerHands.first!, chips: state.chips)
            state.phase = .insuranceOffered
            state.availableActions = actions
            return
        }

        // Dealer peek for 10-value cards
        if state.rules.dealerPeeks && dealerUpCard.rank.isTenValue {
            if state.dealerHand.isBlackjack {
                resolveRound()
                return
            }
        }

        // Check player blackjack
        if state.playerHands.first!.isBlackjack {
            resolveRound()
            return
        }

        playExtraPlayers()
    }

    func takeInsurance() {
        if state.coachEnabled {
            let devResult = state.deviationsEnabled ? DeviationAdvisor.insuranceDeviation(trueCount: state.trueCount) : nil
            let feedback = devResult != nil
                ? "Correct! \(devResult!.description ?? "")"
                : "Basic strategy: never take insurance"
            state.coachFeedback = feedback
        }
        let cost = state.currentBet / 2
        state.insuranceBet = cost
        state.chips -= cost
        afterInsurance()
    }

    func declineInsurance() {
        if state.coachEnabled {
            let devResult = state.deviationsEnabled ? DeviationAdvisor.insuranceDeviation(trueCount: state.trueCount) : nil
            let feedback = devResult != nil
                ? "\(devResult!.description ?? "") (you declined)"
                : "Correct! Never take insurance."
            state.coachFeedback = feedback
        }
        afterInsurance()
    }

    func takeEvenMoney() {
        if state.coachEnabled {
            let devResult = state.deviationsEnabled ? DeviationAdvisor.insuranceDeviation(trueCount: state.trueCount) : nil
            let feedback = devResult != nil
                ? "Correct! \(devResult!.description ?? "")"
                : "Basic strategy: decline even money"
            state.coachFeedback = feedback
        }
        countHoleCard()
        let payout = state.currentBet * 2
        state.phase = .roundComplete
        state.chips += payout
        state.showDealerHoleCard = true
        state.roundPayout = payout
        state.roundMessage = "Even money paid!"
        state.handResults = [0: .win]
        state.handsPlayed += 1
        state.handsWon += 1
    }

    func declineEvenMoney() {
        if state.coachEnabled {
            let devResult = state.deviationsEnabled ? DeviationAdvisor.insuranceDeviation(trueCount: state.trueCount) : nil
            let feedback = devResult != nil
                ? "\(devResult!.description ?? "") (you declined)"
                : "Correct! Decline even money."
            state.coachFeedback = feedback
        }
        afterInsurance()
    }

    private func afterInsurance() {
        if state.rules.dealerPeeks {
            if state.dealerHand.isBlackjack {
                resolveRound()
                return
            }
        }

        if state.playerHands.first!.isBlackjack {
            resolveRound()
            return
        }

        playExtraPlayers()
    }

    private func playExtraPlayers() {
        if state.extraPlayers.isEmpty {
            startPlayerTurn()
            return
        }

        state.phase = .extraPlayersTurn
        state.availableActions = []

        dealTask = Task {
            do {
                guard let dealerUpCard = state.dealerHand.cards.first else { return }

                for epIndex in state.extraPlayers.indices {
                    var hand = state.extraPlayers[epIndex].hand

                    while !hand.isBusted && hand.score < 21 {
                        let actions: Set<PlayerAction> = [.hit, .stand, .doubleDown]
                        let action = BasicStrategyAdvisor.optimalAction(
                            hand: hand,
                            dealerUpCard: dealerUpCard,
                            availableActions: actions,
                            rules: state.rules
                        )

                        if action == .stand { break }

                        try await delay(Self.extraPlayerDelay)
                        let card = drawAndCount()
                        hand = hand.addCard(card)

                        state.extraPlayers[epIndex].hand = hand

                        // Double down means only one card
                        if action == .doubleDown { break }
                    }
                }

                startPlayerTurn()
            } catch {
                // Task was cancelled
            }
        }
    }

    private func startPlayerTurn() {
        let hand = state.playerHands[state.activeHandIndex]
        let dealerUpCard = state.dealerHand.cards.first

        let actions = BlackjackEngine.availableActions(
            hand: hand,
            playerHands: state.playerHands,
            dealerUpCard: dealerUpCard,
            chips: state.chips,
            rules: state.rules
        )

        state.phase = .playerTurn
        state.availableActions = actions

        if hand.isFinished {
            advanceToNextHand()
        }
    }

    func hit() {
        guard state.phase == .playerTurn else { return }
        evaluateCoach(.hit)

        let card = drawAndCount()
        guard let activeHand = state.activeHand else { return }
        let updatedHand = activeHand.addCard(card)
        state.playerHands[state.activeHandIndex] = updatedHand

        if updatedHand.isBusted || updatedHand.score == 21 {
            advanceToNextHand()
        } else {
            updateAvailableActions()
        }
    }

    func stand() {
        guard state.phase == .playerTurn else { return }
        evaluateCoach(.stand)

        guard var activeHand = state.activeHand else { return }
        activeHand.isStanding = true
        state.playerHands[state.activeHandIndex] = activeHand

        advanceToNextHand()
    }

    func doubleDown() {
        guard state.phase == .playerTurn else { return }
        evaluateCoach(.doubleDown)

        guard let hand = state.activeHand else { return }
        guard state.chips >= hand.bet else { return }

        let card = drawAndCount()
        var updatedHand = hand
        updatedHand.cards = hand.cards + [card]
        updatedHand.bet = hand.bet * 2
        updatedHand.isDoubledDown = true
        state.playerHands[state.activeHandIndex] = updatedHand
        state.chips -= hand.bet

        advanceToNextHand()
    }

    func split() {
        guard state.phase == .playerTurn else { return }
        evaluateCoach(.split)

        guard let hand = state.activeHand else { return }
        guard hand.isPair && state.chips >= hand.bet else { return }

        let isAceSplit = hand.cards.first!.rank.isAce

        let hand1 = Hand(
            cards: [hand.cards[0], drawAndCount()],
            bet: hand.bet,
            isSplitHand: true,
            splitFromAces: isAceSplit && !state.rules.hitSplitAces
        )
        let hand2 = Hand(
            cards: [hand.cards[1], drawAndCount()],
            bet: hand.bet,
            isSplitHand: true,
            splitFromAces: isAceSplit && !state.rules.hitSplitAces
        )

        var updatedHands = state.playerHands
        updatedHands[state.activeHandIndex] = hand1
        updatedHands.insert(hand2, at: state.activeHandIndex + 1)

        state.playerHands = updatedHands
        state.chips -= hand.bet

        if hand1.isFinished {
            advanceToNextHand()
        } else {
            updateAvailableActions()
        }
    }

    func surrender() {
        guard state.phase == .playerTurn else { return }
        evaluateCoach(.surrender)

        guard var hand = state.activeHand else { return }
        hand.isSurrendered = true
        state.playerHands[state.activeHandIndex] = hand

        advanceToNextHand()
    }

    private func advanceToNextHand() {
        let nextIndex = state.activeHandIndex + 1

        if nextIndex < state.playerHands.count {
            state.activeHandIndex = nextIndex
            let nextHand = state.playerHands[nextIndex]
            if nextHand.isFinished {
                advanceToNextHand()
            } else {
                updateAvailableActions()
            }
        } else {
            let allBustedOrSurrendered = state.playerHands.allSatisfy {
                $0.isBusted || $0.isSurrendered
            }
            if allBustedOrSurrendered {
                resolveRound()
            } else {
                playDealerHand()
            }
        }
    }

    private func updateAvailableActions() {
        guard let hand = state.playerHands.indices.contains(state.activeHandIndex)
                ? state.playerHands[state.activeHandIndex] : nil else { return }
        let dealerUpCard = state.dealerHand.cards.first

        let actions = BlackjackEngine.availableActions(
            hand: hand,
            playerHands: state.playerHands,
            dealerUpCard: dealerUpCard,
            chips: state.chips,
            rules: state.rules
        )

        state.availableActions = actions
    }

    private func playDealerHand() {
        countHoleCard()
        state.phase = .dealerTurn
        state.showDealerHoleCard = true
        state.availableActions = []

        dealTask = Task {
            do {
                // Pause to let player see the hole card reveal
                try await delay(Self.holeCardRevealDelay)

                while DealerStrategy.shouldHit(hand: state.dealerHand, rules: state.rules) {
                    try await delay(Self.dealerDrawDelay)
                    let card = drawAndCount()
                    state.dealerHand = state.dealerHand.addCard(card)
                }

                // Short pause before showing results
                try await delay(Self.dealCardDelay)
                resolveRound()
            } catch {
                // Task was cancelled
            }
        }
    }

    private func resolveRound() {
        if !state.showDealerHoleCard {
            countHoleCard()
        }

        let result = PayoutCalculator.calculateResults(
            playerHands: state.playerHands,
            dealerHand: state.dealerHand,
            insuranceBet: state.insuranceBet,
            rules: state.rules
        )

        let newChips = state.chips + result.totalPayout

        let winsThisRound = result.handResults.values.filter {
            $0 == .win || $0 == .blackjack || $0 == .threeSevens
        }.count

        let message = buildResultMessage(result: result)

        // Compute extra player results
        let updatedExtraPlayers = state.extraPlayers.map { ep -> ExtraPlayerState in
            let epResult: HandResult
            if ep.hand.isBusted {
                epResult = .bust
            } else if ep.hand.isBlackjack && !state.dealerHand.isBlackjack {
                epResult = .blackjack
            } else if ep.hand.isBlackjack && state.dealerHand.isBlackjack {
                epResult = .push
            } else if !ep.hand.isBlackjack && state.dealerHand.isBlackjack {
                epResult = .lose
            } else if state.dealerHand.isBusted {
                epResult = .win
            } else if ep.hand.score > state.dealerHand.score {
                epResult = .win
            } else if ep.hand.score == state.dealerHand.score {
                epResult = .push
            } else {
                epResult = .lose
            }
            return ExtraPlayerState(hand: ep.hand, result: epResult)
        }

        state.phase = newChips <= 0 ? .gameOver : .roundComplete
        state.handResults = result.handResults
        state.chips = newChips
        state.showDealerHoleCard = true
        state.roundPayout = result.totalPayout
        state.roundMessage = message
        state.availableActions = []
        state.handsPlayed += 1
        state.handsWon += winsThisRound
        state.extraPlayers = updatedExtraPlayers
    }

    private func buildResultMessage(result: PayoutCalculator.RoundResult) -> String {
        let totalBet = state.playerHands.reduce(0) { $0 + $1.bet } + state.insuranceBet
        let net = result.totalPayout - totalBet

        if result.handResults.values.contains(.threeSevens) {
            return "Three 7s! You win $\(net)!"
        } else if result.handResults.values.allSatisfy({ $0 == .blackjack }) {
            return "Blackjack!"
        } else if net > 0 {
            return "You win $\(net)!"
        } else if net < 0 {
            return "You lose $\(-net)"
        } else {
            return "Push"
        }
    }

    func newRound() {
        guard state.phase == .roundComplete else { return }

        dealTask?.cancel()
        state.phase = .betting
        state.playerHands = []
        state.dealerHand = Hand()
        state.activeHandIndex = 0
        state.handResults = [:]
        state.insuranceBet = 0
        state.showDealerHoleCard = false
        state.roundPayout = 0
        state.roundMessage = ""
        state.availableActions = []
        state.currentBet = min(state.currentBet, state.chips)
        state.coachFeedback = ""
        state.extraPlayers = []
    }

    func resetGame() {
        dealTask?.cancel()
        let rules = state.rules
        startGame(rules: rules)
    }
}
