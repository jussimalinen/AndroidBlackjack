import SwiftUI

struct PlayerArea: View {
    let hands: [Hand]
    let activeHandIndex: Int
    let handResults: [Int: HandResult]
    let phase: GamePhase
    let currentBet: Int
    var cardScale: CGFloat = 1.0

    var body: some View {
        let verticalPadding = min(max(16 * cardScale, 4), 24)

        ScrollView {
            VStack(spacing: 2) {
                if hands.count > 1 {
                    Text("Hand \(activeHandIndex + 1) of \(hands.count)")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.6))
                }

                Text("YOUR HAND\(hands.count > 1 ? "S" : "")")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white.opacity(0.6))

                // FlowRow equivalent — wrapping layout
                FlowLayout(spacing: 0) {
                    ForEach(Array(hands.enumerated()), id: \.offset) { index, hand in
                        HandView(
                            hand: hand,
                            isActive: index == activeHandIndex && phase == .playerTurn,
                            showScore: true,
                            result: handResults[index],
                            cardScale: cardScale
                        )
                    }
                }
                .frame(maxWidth: .infinity)

                let totalBet = hands.isEmpty ? currentBet : hands.reduce(0) { $0 + $1.bet }
                if totalBet > 0 {
                    Text("Bet: $\(totalBet)")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.goldAccent)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.vertical, verticalPadding)
        }
    }
}

// Simple wrapping layout for split hands
struct FlowLayout: Layout {
    var spacing: CGFloat = 0

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified
            )
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            totalWidth = max(totalWidth, x)
        }

        return (CGSize(width: totalWidth, height: y + rowHeight), positions)
    }
}
