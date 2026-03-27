enum Suit: CaseIterable {
    case hearts
    case diamonds
    case clubs
    case spades

    var symbol: Character {
        switch self {
        case .hearts: "\u{2665}"
        case .diamonds: "\u{2666}"
        case .clubs: "\u{2663}"
        case .spades: "\u{2660}"
        }
    }

    var isRed: Bool {
        switch self {
        case .hearts, .diamonds: true
        case .clubs, .spades: false
        }
    }
}
