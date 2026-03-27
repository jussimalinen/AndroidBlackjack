import SwiftUI

struct GameScreen: View {
    let viewModel: GameViewModel
    let onNavigateToSettings: () -> Void

    @State private var showChart = false

    var body: some View {
        let state = viewModel.state
        let chartData = BasicStrategyAdvisor.getChartData(rules: state.rules)

        ZStack {
            GameScreenContent(
                state: state,
                onBetChanged: viewModel.adjustBet,
                onDeal: viewModel.deal,
                onHit: viewModel.hit,
                onStand: viewModel.stand,
                onDouble: viewModel.doubleDown,
                onSplit: viewModel.split,
                onSurrender: viewModel.surrender,
                onInsurance: viewModel.takeInsurance,
                onDeclineInsurance: viewModel.declineInsurance,
                onEvenMoney: viewModel.takeEvenMoney,
                onDeclineEvenMoney: viewModel.declineEvenMoney,
                onNewRound: viewModel.newRound,
                onReset: viewModel.resetGame,
                onToggleCoach: viewModel.toggleCoach,
                onToggleDeviations: viewModel.toggleDeviations,
                onToggleCount: viewModel.toggleCount,
                onShowChart: { showChart = true },
                onEnd: onNavigateToSettings
            )

            StrategyChartOverlay(
                visible: showChart,
                chartData: chartData,
                hasSurrender: state.rules.surrenderPolicy != .none,
                onDismiss: { showChart = false }
            )
        }
        .navigationBarHidden(true)
    }
}

private struct UiScales {
    let dealerScale: CGFloat
    let playerScale: CGFloat
    let extraPlayerScale: CGFloat
    let actionBoxHeight: CGFloat
    let compactActions: Bool

    init(dealerScale: CGFloat, playerScale: CGFloat, extraPlayerScale: CGFloat, actionBoxHeight: CGFloat, compactActions: Bool = false) {
        self.dealerScale = dealerScale
        self.playerScale = playerScale
        self.extraPlayerScale = extraPlayerScale
        self.actionBoxHeight = actionBoxHeight
        self.compactActions = compactActions
    }
}

private func computeScales(width: CGFloat, hasExtraPlayers: Bool) -> UiScales {
    if width < 380 {
        return UiScales(
            dealerScale: 0.65,
            playerScale: hasExtraPlayers ? 0.50 : 0.65,
            extraPlayerScale: 0.50,
            actionBoxHeight: 120,
            compactActions: true
        )
    } else if width < 600 {
        return UiScales(
            dealerScale: 1.0,
            playerScale: hasExtraPlayers ? 0.75 : 1.0,
            extraPlayerScale: 0.65,
            actionBoxHeight: 160
        )
    } else {
        return UiScales(
            dealerScale: 1.4,
            playerScale: hasExtraPlayers ? 1.19 : 1.4,
            extraPlayerScale: 1.05,
            actionBoxHeight: 200
        )
    }
}

private struct GameScreenContent: View {
    let state: GameState
    var onBetChanged: (Int) -> Void = { _ in }
    var onDeal: () -> Void = {}
    var onHit: () -> Void = {}
    var onStand: () -> Void = {}
    var onDouble: () -> Void = {}
    var onSplit: () -> Void = {}
    var onSurrender: () -> Void = {}
    var onInsurance: () -> Void = {}
    var onDeclineInsurance: () -> Void = {}
    var onEvenMoney: () -> Void = {}
    var onDeclineEvenMoney: () -> Void = {}
    var onNewRound: () -> Void = {}
    var onReset: () -> Void = {}
    var onToggleCoach: () -> Void = {}
    var onToggleDeviations: () -> Void = {}
    var onToggleCount: () -> Void = {}
    var onShowChart: () -> Void = {}
    var onEnd: () -> Void = {}

    var body: some View {
        GeometryReader { geometry in
            let hasExtraPlayers = !state.extraPlayers.isEmpty
            let scales = computeScales(width: geometry.size.width, hasExtraPlayers: hasExtraPlayers)

            VStack(spacing: 0) {
                GameInfoBar(
                    chips: state.chips,
                    shoePenetration: state.shoePenetration,
                    handsPlayed: state.handsPlayed,
                    handsWon: state.handsWon,
                    coachEnabled: state.coachEnabled,
                    onToggleCoach: onToggleCoach,
                    deviationsEnabled: state.deviationsEnabled,
                    onToggleDeviations: onToggleDeviations,
                    showCount: state.showCount,
                    runningCount: state.runningCount,
                    trueCount: state.trueCount,
                    onToggleCount: onToggleCount,
                    onShowChart: onShowChart,
                    onEnd: onEnd,
                    coachFeedback: state.coachFeedback,
                    coachCorrect: state.coachCorrect,
                    coachTotal: state.coachTotal
                )

                DealerArea(
                    hand: state.dealerHand,
                    showHoleCard: state.showDealerHoleCard,
                    compact: hasExtraPlayers,
                    cardScale: scales.dealerScale
                )

                if hasExtraPlayers {
                    ExtraPlayersArea(
                        extraPlayers: state.extraPlayers,
                        cardScale: scales.extraPlayerScale
                    )
                }

                PlayerArea(
                    hands: state.playerHands,
                    activeHandIndex: state.activeHandIndex,
                    handResults: state.handResults,
                    phase: state.phase,
                    currentBet: state.currentBet,
                    cardScale: scales.playerScale
                )

                // Bottom action area — fixed height
                ZStack {
                    switch state.phase {
                    case .betting:
                        BetSelector(
                            currentBet: state.currentBet,
                            chips: state.chips,
                            rules: state.rules,
                            onBetChanged: onBetChanged,
                            onDeal: onDeal
                        )

                    case .insuranceOffered:
                        VStack(spacing: 4) {
                            Text("Insurance?")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                            InsuranceBar(
                                availableActions: state.availableActions,
                                onInsurance: onInsurance,
                                onDecline: onDeclineInsurance,
                                onEvenMoney: onEvenMoney,
                                onDeclineEvenMoney: onDeclineEvenMoney
                            )
                        }
                        .padding(8)

                    case .playerTurn:
                        ActionBar(
                            availableActions: state.availableActions,
                            onHit: onHit,
                            onStand: onStand,
                            onDouble: onDouble,
                            onSplit: onSplit,
                            onSurrender: onSurrender
                        )

                    case .dealing, .dealerPeek, .extraPlayersTurn, .dealerTurn:
                        Text(phaseMessage(state.phase))
                            .font(.system(size: 16))
                            .foregroundColor(.white.opacity(0.7))

                    case .roundComplete:
                        ResultArea(
                            message: state.roundMessage,
                            payout: state.roundPayout,
                            onNewRound: onNewRound,
                            compact: scales.compactActions
                        )

                    case .gameOver:
                        GameOverArea(
                            handsPlayed: state.handsPlayed,
                            handsWon: state.handsWon,
                            onReset: onReset,
                            compact: scales.compactActions
                        )
                    }
                }
                .frame(height: scales.actionBoxHeight)
                .frame(maxWidth: .infinity)
            }
            .background(Color.feltGreen)
        }
    }

    private func phaseMessage(_ phase: GamePhase) -> String {
        switch phase {
        case .dealerTurn: "Dealer's turn..."
        case .extraPlayersTurn: "Other players..."
        default: "Dealing..."
        }
    }
}

private struct ResultArea: View {
    let message: String
    let payout: Int
    let onNewRound: () -> Void
    var compact: Bool = false

    var body: some View {
        VStack(spacing: compact ? 4 : 12) {
            Text(message)
                .font(.system(size: compact ? 16 : 20, weight: .bold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)

            Button(action: onNewRound) {
                Text("Next Hand")
                    .font(.system(size: compact ? 14 : 16, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Color.goldAccent)
                    .clipShape(Capsule())
            }
        }
        .padding(compact ? 8 : 16)
    }
}

private struct GameOverArea: View {
    let handsPlayed: Int
    let handsWon: Int
    let onReset: () -> Void
    var compact: Bool = false

    var body: some View {
        VStack(spacing: compact ? 2 : 12) {
            Text("Game Over!")
                .font(.system(size: compact ? 18 : 24, weight: .bold))
                .foregroundColor(Color(hex: 0xEF5350))

            if !compact {
                Text("You're out of chips")
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.7))
            }

            Text(compact
                ? "Out of chips · Played: \(handsPlayed)  Won: \(handsWon)"
                : "Played: \(handsPlayed)  Won: \(handsWon)")
                .font(.system(size: compact ? 11 : 13))
                .foregroundColor(.white.opacity(0.7))

            Button(action: onReset) {
                Text("Play Again")
                    .font(.system(size: compact ? 13 : 14, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .background(Color.goldAccent)
                    .cornerRadius(8)
            }
        }
        .padding(compact ? 4 : 16)
    }
}
