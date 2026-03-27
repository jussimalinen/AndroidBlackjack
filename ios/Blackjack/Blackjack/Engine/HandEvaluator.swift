enum HandEvaluator {

    static func bestScore(_ cards: [Card]) -> Int {
        if cards.isEmpty { return 0 }
        let aceCount = cards.filter { $0.rank.isAce }.count
        var sum = cards.filter { !$0.rank.isAce }.reduce(0) { $0 + $1.rank.baseValue }
        sum += aceCount // all aces as 1
        if aceCount > 0 && sum + 10 <= 21 {
            sum += 10 // promote one ace to 11
        }
        return sum
    }

    static func isSoft(_ cards: [Card]) -> Bool {
        guard cards.contains(where: { $0.rank.isAce }) else { return false }
        let aceCount = cards.filter { $0.rank.isAce }.count
        let nonAceSum = cards.filter { !$0.rank.isAce }.reduce(0) { $0 + $1.rank.baseValue }
        return (nonAceSum + 11 + (aceCount - 1)) <= 21
    }
}
