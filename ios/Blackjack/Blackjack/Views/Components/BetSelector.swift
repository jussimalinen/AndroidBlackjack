import SwiftUI

private struct ChipDenom {
    let value: Int
    let color: Color
}

private let chipDenoms = [
    ChipDenom(value: 5, color: .chipRed),
    ChipDenom(value: 10, color: .chipBlue),
    ChipDenom(value: 25, color: .chipGreen),
    ChipDenom(value: 50, color: .chipBlack),
    ChipDenom(value: 100, color: .chipGold),
]

struct BetSelector: View {
    let currentBet: Int
    let chips: Int
    let rules: CasinoRules
    let onBetChanged: (Int) -> Void
    let onDeal: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Text("Place Your Bet")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)

            HStack(spacing: 0) {
                ForEach(chipDenoms, id: \.value) { chip in
                    let canAdd = currentBet + chip.value <= min(rules.maximumBet, chips)
                    Button {
                        onBetChanged(currentBet + chip.value)
                    } label: {
                        Text("\(chip.value)")
                            .font(.system(size: chip.value >= 100 ? 11 : 12, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(canAdd ? chip.color : chip.color.opacity(0.3))
                            .clipShape(Circle())
                    }
                    .disabled(!canAdd)
                    .frame(maxWidth: .infinity)
                }
            }

            HStack {
                Button("Clear") {
                    onBetChanged(rules.minimumBet)
                }
                .disabled(currentBet <= rules.minimumBet)
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.white.opacity(0.5), lineWidth: 1)
                )

                Spacer()

                Button {
                    onDeal()
                } label: {
                    Text("DEAL")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.black)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 10)
                        .background(Color.goldAccent)
                        .clipShape(Capsule())
                }
                .disabled(!(rules.minimumBet...min(rules.maximumBet, chips)).contains(currentBet))
            }
            .padding(.horizontal, 16)
        }
        .padding(12)
    }
}
