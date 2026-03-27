enum HandResult: Hashable {
    case threeSevens
    case blackjack
    case win
    case lose
    case push
    case bust
    case surrender

    var displayName: String {
        switch self {
        case .threeSevens: "Three 7s!"
        case .blackjack: "Blackjack!"
        case .win: "Win"
        case .lose: "Lose"
        case .push: "Push"
        case .bust: "Bust"
        case .surrender: "Surrender"
        }
    }
}
