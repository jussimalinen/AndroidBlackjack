import SwiftUI

struct HandView: View {
    let hand: Hand
    let isActive: Bool
    let showScore: Bool
    let result: HandResult?
    var cardScale: CGFloat = 1.0

    private var scaledPadding: CGFloat {
        min(max(4 * cardScale, 2), 6)
    }

    var body: some View {
        let cardOverlap: CGFloat = 28
        let cardWidth: CGFloat = 70

        VStack(spacing: 2) {
            if showScore && !hand.cards.isEmpty {
                let scoreText = "\(hand.score)" + (hand.isSoft ? " (soft)" : "")
                Text(scoreText)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
            }

            let handWidth = hand.cards.count > 1
                ? cardWidth + CGFloat(hand.cards.count - 1) * cardOverlap
                : cardWidth

            ZStack(alignment: .topLeading) {
                if hand.cards.isEmpty {
                    EmptyCardSlot()
                } else {
                    ForEach(Array(hand.cards.enumerated()), id: \.offset) { index, card in
                        AnimatedCardView(card: card)
                            .offset(x: CGFloat(index) * cardOverlap)
                            .zIndex(Double(index))
                    }
                }
            }
            .frame(minWidth: handWidth, minHeight: 100, alignment: .topLeading)
            .padding(isActive ? 2 : 0)
            .overlay(
                isActive
                    ? RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.goldAccent, lineWidth: 2)
                    : nil
            )

            if hand.bet > 0 {
                Text("$\(hand.bet)")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.goldAccent)
            }

            if let result = result {
                Text(result.displayName)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(resultColor(result))
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(4)
            }
        }
        .padding(scaledPadding)
        .scaleEffect(cardScale, anchor: .top)
    }

    private func resultColor(_ result: HandResult) -> Color {
        switch result {
        case .threeSevens: Color(hex: 0xFFD700)
        case .blackjack, .win: Color(hex: 0x4CAF50)
        case .lose, .bust: Color(hex: 0xEF5350)
        case .push: Color(hex: 0xFFEB3B)
        case .surrender: Color(hex: 0xFF9800)
        }
    }
}
