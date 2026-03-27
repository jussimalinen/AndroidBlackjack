enum ChartCell {
    case hit
    case stand
    case doubleHit
    case doubleStand
    case split
    case splitHit
    case surrenderHit
    case surrenderStand
    case surrenderSplit

    var symbol: String {
        switch self {
        case .hit: "H"
        case .stand: "-"
        case .doubleHit: "D"
        case .doubleStand: "D/S"
        case .split: "Y"
        case .splitHit: "Y/H"
        case .surrenderHit: "Rh"
        case .surrenderStand: "Rs"
        case .surrenderSplit: "Ry"
        }
    }
}

struct ChartRow: Equatable {
    let label: String
    let cells: [ChartCell]
}

struct StrategyChartData: Equatable {
    let hardRows: [ChartRow]
    let softRows: [ChartRow]
    let pairRows: [ChartRow]
}
