import SwiftUI

struct DealerArea: View {
    let hand: Hand
    let showHoleCard: Bool
    var compact: Bool = false
    var cardScale: CGFloat = 1.0

    var body: some View {
        let padding: CGFloat = compact ? 4 : 16

        VStack(spacing: 2) {
            Text("DEALER")
                .font(.system(size: 15 * cardScale, weight: .bold))
                .foregroundColor(.white.opacity(0.6))

            if !hand.cards.isEmpty {
                let scoreText = showHoleCard
                    ? "\(hand.score)" + (hand.isSoft ? " (soft)" : "")
                    : ""
                Text(scoreText)
                    .font(.system(size: 16 * cardScale, weight: .bold))
                    .foregroundColor(.white.opacity(showHoleCard ? 1 : 0))
                    .animation(.easeIn(duration: 0.3), value: showHoleCard)
            }

            let displayHand: Hand = {
                if showHoleCard {
                    return hand
                } else {
                    var h = hand
                    h.cards = hand.cards.enumerated().map { index, card in
                        index == 1 ? card.flip() : card
                    }
                    return h
                }
            }()

            HandView(
                hand: displayHand,
                isActive: false,
                showScore: false,
                result: nil,
                cardScale: cardScale
            )
        }
        .frame(maxWidth: .infinity)
        .padding(padding)
    }
}
