import SwiftUI

struct SettingsScreen: View {
    let settingsViewModel: SettingsViewModel
    let onStartGame: (CasinoRules) -> Void

    var body: some View {
        let rules = settingsViewModel.rules

        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Blackjack")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.goldAccent)
                Text("Configure Casino Rules")
                    .font(.system(size: 16))
                    .foregroundColor(.white.opacity(0.7))

                Spacer().frame(height: 16)

                // Presets
                SectionHeader("Presets")
                HStack(spacing: 8) {
                    PresetButton("Vegas") { settingsViewModel.applyPreset("Vegas") }
                    PresetButton("European") { settingsViewModel.applyPreset("European") }
                    PresetButton("Favorable") { settingsViewModel.applyPreset("Favorable") }
                }
                HStack(spacing: 8) {
                    PresetButton("Helsinki") { settingsViewModel.applyPreset("Helsinki") }
                }
                .padding(.top, 8)

                Spacer().frame(height: 16)
                Divider().background(Color.white.opacity(0.2))
                Spacer().frame(height: 12)

                // Deck count
                SectionHeader("Number of Decks: \(rules.numberOfDecks)")
                Slider(value: Binding(
                    get: { Double(rules.numberOfDecks) },
                    set: { settingsViewModel.updateNumberOfDecks(Int($0)) }
                ), in: 1...8, step: 1)
                .tint(.goldAccent)

                Spacer().frame(height: 8)

                // Dealer rules
                SectionHeader("Dealer Rules")
                SwitchRow(label: "Dealer stands on soft 17", checked: rules.dealerStandsOnSoft17) {
                    settingsViewModel.toggleDealerStandsOnSoft17($0)
                }
                SwitchRow(label: "Dealer peeks for blackjack", checked: rules.dealerPeeks) {
                    settingsViewModel.toggleDealerPeeks($0)
                }

                Spacer().frame(height: 8)

                // Payout
                SectionHeader("Blackjack Payout")
                HStack(spacing: 8) {
                    PayoutButton("3:2", selected: rules.blackjackPayout == .threeToTwo) {
                        settingsViewModel.setBlackjackPayout(.threeToTwo)
                    }
                    PayoutButton("6:5", selected: rules.blackjackPayout == .sixToFive) {
                        settingsViewModel.setBlackjackPayout(.sixToFive)
                    }
                }

                Spacer().frame(height: 8)

                // Surrender
                SectionHeader("Surrender Policy")
                HStack(spacing: 8) {
                    PayoutButton("None", selected: rules.surrenderPolicy == .none) {
                        settingsViewModel.setSurrenderPolicy(.none)
                    }
                    PayoutButton("Late", selected: rules.surrenderPolicy == .late) {
                        settingsViewModel.setSurrenderPolicy(.late)
                    }
                    PayoutButton("Early", selected: rules.surrenderPolicy == .early) {
                        settingsViewModel.setSurrenderPolicy(.early)
                    }
                }

                Spacer().frame(height: 8)

                // Split rules
                SectionHeader("Split Rules")
                SwitchRow(label: "Double after split", checked: rules.doubleAfterSplit) {
                    settingsViewModel.toggleDoubleAfterSplit($0)
                }
                SwitchRow(label: "Re-split aces", checked: rules.resplitAces) {
                    settingsViewModel.toggleResplitAces($0)
                }
                SwitchRow(label: "Hit split aces", checked: rules.hitSplitAces) {
                    settingsViewModel.toggleHitSplitAces($0)
                }

                SectionHeader("Max Split Hands: \(rules.maxSplitHands)")
                Slider(value: Binding(
                    get: { Double(rules.maxSplitHands) },
                    set: { settingsViewModel.setMaxSplitHands(Int($0)) }
                ), in: 2...4, step: 1)
                .tint(.goldAccent)

                Spacer().frame(height: 8)

                // Other
                SectionHeader("Other")
                SwitchRow(label: "Insurance available", checked: rules.insuranceAvailable) {
                    settingsViewModel.toggleInsurance($0)
                }
                SwitchRow(label: "Three 7s pays 3:1", checked: rules.threeSevensPays3to1) {
                    settingsViewModel.toggleThreeSevensBonus($0)
                }

                Spacer().frame(height: 8)

                // Training
                SectionHeader("Training")
                SwitchRow(label: "Soft hands only", checked: rules.trainSoftHands) {
                    settingsViewModel.toggleTrainSoftHands($0)
                }
                SwitchRow(label: "Paired hands only", checked: rules.trainPairedHands) {
                    settingsViewModel.toggleTrainPairedHands($0)
                }

                Spacer().frame(height: 8)

                // Extra players
                SectionHeader("Extra Players: \(rules.extraPlayers)")
                Text("Computer-controlled players for card counting practice")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.5))
                    .padding(.bottom, 4)
                Slider(value: Binding(
                    get: { Double(rules.extraPlayers) },
                    set: { settingsViewModel.setExtraPlayers(Int($0)) }
                ), in: 0...2, step: 1)
                .tint(.goldAccent)

                Spacer().frame(height: 24)

                // Start button
                Button {
                    onStartGame(rules)
                } label: {
                    Text("START GAME")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.goldAccent)
                        .cornerRadius(12)
                }

                Spacer().frame(height: 16)
            }
            .padding(16)
        }
        .background(Color.feltGreen)
        .navigationBarHidden(true)
    }
}

private struct SectionHeader: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text)
            .font(.system(size: 15, weight: .bold))
            .foregroundColor(.white)
            .padding(.vertical, 4)
    }
}

private struct SwitchRow: View {
    let label: String
    let checked: Bool
    let onCheckedChange: (Bool) -> Void

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.white.opacity(0.9))
            Spacer()
            Toggle("", isOn: Binding(
                get: { checked },
                set: { onCheckedChange($0) }
            ))
            .tint(.goldAccent)
            .labelsHidden()
        }
        .padding(.vertical, 4)
    }
}

private struct PresetButton: View {
    let text: String
    let action: () -> Void
    init(_ text: String, action: @escaping () -> Void) {
        self.text = text
        self.action = action
    }
    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.white.opacity(0.5), lineWidth: 1)
                )
        }
    }
}

private struct PayoutButton: View {
    let text: String
    let selected: Bool
    let action: () -> Void
    init(_ text: String, selected: Bool, action: @escaping () -> Void) {
        self.text = text
        self.selected = selected
        self.action = action
    }
    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(selected ? .black : .white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(selected ? Color.goldAccent : Color.gray.opacity(0.3))
                .cornerRadius(8)
        }
    }
}
