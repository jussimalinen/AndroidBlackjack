/// Standard basic strategy advisor for 4-8 deck blackjack.
/// Each table cell encodes the ideal action with fallback:
///   H = Hit, S = Stand, D = Double (else Hit), Ds = Double (else Stand),
///   P = Split, Ph = Split (else Hit), Pd = Split (with DAS, else Hit),
///   Rh = Surrender (else Hit), Rs = Surrender (else Stand), Rp = Surrender (else Split)
enum BasicStrategyAdvisor {

    static func optimalAction(
        hand: Hand,
        dealerUpCard: Card,
        availableActions: Set<PlayerAction>,
        rules: CasinoRules
    ) -> PlayerAction {
        if availableActions.isEmpty { return .stand }

        let dealerIndex = dealerColumnIndex(dealerUpCard.rank)

        // Pairs (only if split is theoretically possible)
        if hand.isPair && hand.cards.count == 2 {
            if let pairAction = pairStrategy(rank: hand.cards.first!.rank, dealerIdx: dealerIndex, rules: rules) {
                if let resolved = resolveAction(pairAction, available: availableActions) {
                    return resolved
                }
            }
        }

        // Soft totals (ace counted as 11)
        if hand.isSoft && (13...20).contains(hand.score) {
            let cell = softStrategy(score: hand.score, dealerIdx: dealerIndex, rules: rules)
            if let resolved = resolveAction(cell, available: availableActions) {
                return resolved
            }
        }

        // Hard totals
        let cell = hardStrategy(score: hand.score, dealerIdx: dealerIndex, cardCount: hand.cards.count, rules: rules)
        return resolveAction(cell, available: availableActions) ?? .stand
    }

    // MARK: - Column mapping: dealer upcard rank -> index 0-9 (2,3,4,5,6,7,8,9,T,A)

    private static func dealerColumnIndex(_ rank: Rank) -> Int {
        switch rank {
        case .two: 0
        case .three: 1
        case .four: 2
        case .five: 3
        case .six: 4
        case .seven: 5
        case .eight: 6
        case .nine: 7
        case .ten, .jack, .queen, .king: 8
        case .ace: 9
        }
    }

    // MARK: - Action codes

    private enum Cell {
        case H    // Hit
        case S    // Stand
        case D    // Double else Hit
        case Ds   // Double else Stand
        case P    // Split
        case Ph   // Split else Hit
        case Pd   // Split if DAS else Hit
        case Rh   // Surrender else Hit
        case Rs   // Surrender else Stand
        case Rp   // Surrender else Split
    }

    private static func resolveAction(_ cell: Cell, available: Set<PlayerAction>) -> PlayerAction? {
        switch cell {
        case .H:
            return pickIfAvailable(.hit, available)
        case .S:
            return pickIfAvailable(.stand, available)
        case .D:
            if available.contains(.doubleDown) { return .doubleDown }
            return pickIfAvailable(.hit, available)
        case .Ds:
            if available.contains(.doubleDown) { return .doubleDown }
            return pickIfAvailable(.stand, available)
        case .P:
            if let split = pickIfAvailable(.split, available) { return split }
            return pickIfAvailable(.hit, available)
        case .Ph:
            if available.contains(.split) { return .split }
            return pickIfAvailable(.hit, available)
        case .Pd:
            if available.contains(.split) { return .split }
            return pickIfAvailable(.hit, available)
        case .Rh:
            if available.contains(.surrender) { return .surrender }
            return pickIfAvailable(.hit, available)
        case .Rs:
            if available.contains(.surrender) { return .surrender }
            return pickIfAvailable(.stand, available)
        case .Rp:
            if available.contains(.surrender) { return .surrender }
            if available.contains(.split) { return .split }
            return pickIfAvailable(.hit, available)
        }
    }

    private static func pickIfAvailable(_ action: PlayerAction, _ available: Set<PlayerAction>) -> PlayerAction? {
        available.contains(action) ? action : nil
    }

    // MARK: - Hard totals strategy (rows: score 5-20, cols: dealer 2-A)

    //                                      2     3     4     5     6     7     8     9     T     A
    private static let hardTable: [Int: [Cell]] = [
        5:  [.H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H],
        6:  [.H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H],
        7:  [.H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H],
        8:  [.H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H,  .H],
        9:  [.H,  .D,  .D,  .D,  .D,  .H,  .H,  .H,  .H,  .H],
        10: [.D,  .D,  .D,  .D,  .D,  .D,  .D,  .D,  .H,  .H],
        11: [.D,  .D,  .D,  .D,  .D,  .D,  .D,  .D,  .D,  .H],
        12: [.H,  .H,  .S,  .S,  .S,  .H,  .H,  .H,  .H,  .H],
        13: [.S,  .S,  .S,  .S,  .S,  .H,  .H,  .H,  .H,  .H],
        14: [.S,  .S,  .S,  .S,  .S,  .H,  .H,  .H,  .H,  .H],
        15: [.S,  .S,  .S,  .S,  .S,  .H,  .H,  .H,  .Rh, .H],
        16: [.S,  .S,  .S,  .S,  .S,  .H,  .H,  .Rh, .Rh, .Rh],
        17: [.S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S],
        18: [.S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S],
        19: [.S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S],
        20: [.S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S],
    ]

    private static func hardStrategy(score: Int, dealerIdx: Int, cardCount: Int = 2, rules: CasinoRules) -> Cell {
        // H17 deviation: double 11 vs A
        if score == 11 && dealerIdx == 9 && !rules.dealerStandsOnSoft17 {
            return .D
        }
        // H17 surrender deviations
        if !rules.dealerStandsOnSoft17 {
            if score == 17 && dealerIdx == 9 { return .Rs }
            if score == 15 && dealerIdx == 9 { return .Rh }
        }
        // ENHC (no hole card) deviations
        if !rules.dealerPeeks {
            if score == 11 && dealerIdx == 8 { return .H }     // Don't double 11 vs 10
            if score == 14 && dealerIdx == 8 { return .Rh }    // Surrender 14 vs 10
            if score == 16 && dealerIdx == 8 && cardCount >= 3 { return .S }  // Multi-card 16 vs 10: Stand
            if score == 16 && dealerIdx == 9 { return .H }     // Don't surrender 16 vs A
        }
        if score <= 4 { return .H }
        if score >= 21 { return .S }
        let clamped = max(5, min(score, 20))
        return hardTable[clamped]?[dealerIdx] ?? .H
    }

    // MARK: - Soft totals (rows: total 13=A2 through 20=A9, cols: dealer 2-A)

    //                                       2      3      4      5      6      7      8      9      T      A
    private static let softTable: [Int: [Cell]] = [
        13: [.H,  .H,  .H,  .D,  .D,  .H,  .H,  .H,  .H,  .H],
        14: [.H,  .H,  .H,  .D,  .D,  .H,  .H,  .H,  .H,  .H],
        15: [.H,  .H,  .D,  .D,  .D,  .H,  .H,  .H,  .H,  .H],
        16: [.H,  .H,  .D,  .D,  .D,  .H,  .H,  .H,  .H,  .H],
        17: [.H,  .D,  .D,  .D,  .D,  .H,  .H,  .H,  .H,  .H],
        18: [.Ds, .Ds, .Ds, .Ds, .Ds, .S,  .S,  .H,  .H,  .H],
        19: [.S,  .S,  .S,  .S,  .Ds, .S,  .S,  .S,  .S,  .S],
        20: [.S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S,  .S],
    ]

    private static func softStrategy(score: Int, dealerIdx: Int, rules: CasinoRules) -> Cell {
        // ENHC (no hole card) deviations
        if !rules.dealerPeeks {
            if score == 18 && dealerIdx == 0 { return .S }    // Soft 18 vs 2: Stand
            if score == 19 && dealerIdx == 4 { return .S }    // Soft 19 vs 6: Stand
        }
        let clamped = max(13, min(score, 20))
        return softTable[clamped]?[dealerIdx] ?? .H
    }

    // MARK: - Pairs strategy (by pair rank, cols: dealer 2-A)

    private static func pairStrategy(rank: Rank, dealerIdx: Int, rules: CasinoRules) -> Cell? {
        let das = rules.doubleAfterSplit
        switch rank {
        case .ace:
            return (!rules.dealerPeeks && dealerIdx == 9) ? .H : .P
        case .ten, .jack, .queen, .king:
            return .S  // Never split tens
        case .five:
            return nil  // Treat as hard 10
        case .nine:
            return pairNines(dealerIdx)
        case .eight:
            return pairEights(dealerIdx, rules: rules)
        case .seven:
            return pairSevens(dealerIdx)
        case .six:
            return pairSixes(dealerIdx, das: das)
        case .four:
            return pairFours(dealerIdx, das: das)
        case .three:
            return pairThreeTwos(dealerIdx, das: das)
        case .two:
            return pairThreeTwos(dealerIdx, das: das)
        }
    }

    //                          2   3   4   5   6   7   8   9   T   A
    private static func pairNines(_ d: Int) -> Cell {
        switch d {
        case 5: .S   // vs 7: Stand
        case 8: .S   // vs T: Stand
        case 9: .S   // vs A: Stand
        default: .P  // vs 2-6,8,9: Split
        }
    }

    private static func pairEights(_ d: Int, rules: CasinoRules) -> Cell {
        // ENHC (no hole card) deviations
        if !rules.dealerPeeks {
            if d == 8 { return .Rh }  // Surrender 8,8 vs 10
            if d == 9 { return .H }   // Hit 8,8 vs A
        }
        // H17 deviation: surrender 8s vs A
        if d == 9 && !rules.dealerStandsOnSoft17 { return .Rp }
        return .P // Always split 8s
    }

    private static func pairSevens(_ d: Int) -> Cell {
        d <= 5 ? .P : .H   // vs 2-7: Split, vs 8+: Hit
    }

    private static func pairSixes(_ d: Int, das: Bool) -> Cell {
        if das && d <= 4 { return .P }       // DAS: split vs 2-6
        if !das && (1...4).contains(d) { return .P }  // No DAS: split vs 3-6
        if !das && d == 0 { return .H }
        if d <= 4 { return .P }
        return .H
    }

    private static func pairFours(_ d: Int, das: Bool) -> Cell {
        if das && (3...4).contains(d) { return .P }  // DAS: split vs 5-6
        return .H
    }

    private static func pairThreeTwos(_ d: Int, das: Bool) -> Cell {
        if das && d <= 5 { return .P }            // DAS: split vs 2-7
        if !das && (2...5).contains(d) { return .P }  // No DAS: split vs 4-7
        return .H
    }

    // MARK: - Chart data generation

    static func getChartData(rules: CasinoRules) -> StrategyChartData {
        let hardRows = (5...20).map { score in
            ChartRow(
                label: "\(score)",
                cells: (0...9).map { d in cellToChartCell(hardStrategy(score: score, dealerIdx: d, cardCount: 2, rules: rules)) }
            )
        }

        let softLabels = ["A,2", "A,3", "A,4", "A,5", "A,6", "A,7", "A,8", "A,9"]
        let softRows = (13...20).enumerated().map { (i, score) in
            ChartRow(
                label: softLabels[i],
                cells: (0...9).map { d in cellToChartCell(softStrategy(score: score, dealerIdx: d, rules: rules)) }
            )
        }

        let pairRanks: [Rank] = [.two, .three, .four, .five, .six, .seven, .eight, .nine, .ten, .ace]
        let pairLabels = ["2,2", "3,3", "4,4", "5,5", "6,6", "7,7", "8,8", "9,9", "10,10", "A,A"]
        let pairRows = pairRanks.enumerated().map { (i, rank) in
            ChartRow(
                label: pairLabels[i],
                cells: (0...9).map { d in
                    if let cell = pairStrategy(rank: rank, dealerIdx: d, rules: rules) {
                        cellToChartCell(cell)
                    } else {
                        cellToChartCell(hardStrategy(score: rank.baseValue * 2, dealerIdx: d, cardCount: 2, rules: rules))
                    }
                }
            )
        }

        return StrategyChartData(hardRows: hardRows, softRows: softRows, pairRows: pairRows)
    }

    private static func cellToChartCell(_ cell: Cell) -> ChartCell {
        switch cell {
        case .H:  .hit
        case .S:  .stand
        case .D:  .doubleHit
        case .Ds: .doubleStand
        case .P:  .split
        case .Ph: .splitHit
        case .Pd: .splitHit
        case .Rh: .surrenderHit
        case .Rs: .surrenderStand
        case .Rp: .surrenderSplit
        }
    }
}
