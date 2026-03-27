struct Hand: Equatable {
    var cards: [Card] = []
    var bet: Int = 0
    var isSplitHand: Bool = false
    var isDoubledDown: Bool = false
    var isSurrendered: Bool = false
    var isStanding: Bool = false
    var splitFromAces: Bool = false

    var score: Int { HandEvaluator.bestScore(cards) }

    var isSoft: Bool { HandEvaluator.isSoft(cards) }

    var isBusted: Bool { score > 21 }

    var isBlackjack: Bool {
        cards.count == 2 && score == 21 && !isSplitHand
    }

    var isPair: Bool {
        cards.count == 2 && cards[0].rank.baseValue == cards[1].rank.baseValue
    }

    var isFinished: Bool {
        isBusted || isStanding || isDoubledDown || isSurrendered || isBlackjack ||
            (splitFromAces && cards.count >= 2)
    }

    func addCard(_ card: Card) -> Hand {
        var copy = self
        copy.cards = cards + [card]
        return copy
    }
}
