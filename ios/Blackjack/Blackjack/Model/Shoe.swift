class Shoe {
    private var cards: [Card] = []
    private var cutCardPosition: Int = 0
    let totalCards: Int
    var cardsRemaining: Int { cards.count }
    var penetration: Float { 1.0 - (Float(cards.count) / Float(totalCards)) }

    private let numberOfDecks: Int

    init(numberOfDecks: Int) {
        self.numberOfDecks = numberOfDecks
        self.totalCards = numberOfDecks * 52
        shuffle()
    }

    func shuffle() {
        cards.removeAll()
        for _ in 0..<numberOfDecks {
            for suit in Suit.allCases {
                for rank in Rank.allCases {
                    cards.append(Card(rank: rank, suit: suit))
                }
            }
        }
        cards.shuffle()
        cutCardPosition = Int(Double(totalCards) * 0.25)
    }

    func draw() -> Card {
        precondition(!cards.isEmpty, "Shoe is empty")
        return cards.removeFirst()
    }

    func drawMatching(_ predicate: (Card) -> Bool) -> Card {
        guard let index = cards.firstIndex(where: predicate) else {
            preconditionFailure("No matching card in shoe")
        }
        let result = cards.remove(at: index)
        cards.shuffle()
        return result
    }

    func needsReshuffle() -> Bool {
        cards.count <= cutCardPosition
    }
}
