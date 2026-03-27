import SwiftUI

struct ExtraPlayersArea: View {
    let extraPlayers: [ExtraPlayerState]
    var cardScale: CGFloat = 0.75

    var body: some View {
        if extraPlayers.isEmpty { return AnyView(EmptyView()) }

        return AnyView(
            VStack {
                HStack(spacing: 0) {
                    ForEach(Array(extraPlayers.enumerated()), id: \.offset) { index, ep in
                        VStack(spacing: 0) {
                            Text("P\(index + 1)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white.opacity(0.5))
                            HandView(
                                hand: ep.hand,
                                isActive: false,
                                showScore: !ep.hand.cards.isEmpty,
                                result: ep.result,
                                cardScale: cardScale
                            )
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 16)
        )
    }
}
