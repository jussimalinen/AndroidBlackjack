import SwiftUI

struct CardView: View {
    let card: Card

    var body: some View {
        if !card.faceUp {
            ZStack {
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.cardBack)
                    .frame(width: 70, height: 100)
                RoundedRectangle(cornerRadius: 6)
                    .stroke(Color.white.opacity(0.3), lineWidth: 1)
                    .frame(width: 70, height: 100)
                Text("\u{2660}")
                    .font(.system(size: 30))
                    .foregroundColor(.white.opacity(0.3))
            }
        } else {
            let textColor: Color = card.suit.isRed ? .cardRed : .cardBlack

            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.cardWhite)
                    .frame(width: 70, height: 100)
                RoundedRectangle(cornerRadius: 6)
                    .stroke(Color.gray.opacity(0.5), lineWidth: 1)
                    .frame(width: 70, height: 100)

                VStack(alignment: .leading, spacing: 0) {
                    Text(card.rank.symbol)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(textColor)
                    Text(String(card.suit.symbol))
                        .font(.system(size: 14))
                        .foregroundColor(textColor)
                }
                .padding(5)

                Text(String(card.suit.symbol))
                    .font(.system(size: 34))
                    .foregroundColor(textColor)
                    .frame(width: 70, height: 100)
            }
        }
    }
}

struct AnimatedCardView: View {
    let card: Card
    @State private var visible = false

    var body: some View {
        CardView(card: card)
            .opacity(visible ? 1 : 0)
            .offset(x: visible ? 0 : -35)
            .onAppear {
                withAnimation(.easeOut(duration: 0.2)) {
                    visible = true
                }
            }
    }
}

struct EmptyCardSlot: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 6)
            .stroke(Color.white.opacity(0.2), lineWidth: 1)
            .frame(width: 70, height: 100)
    }
}
