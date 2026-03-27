struct Card: Equatable, Hashable {
    let rank: Rank
    let suit: Suit
    var faceUp: Bool = true

    var display: String { "\(rank.symbol)\(suit.symbol)" }

    func flip() -> Card {
        Card(rank: rank, suit: suit, faceUp: !faceUp)
    }
}
