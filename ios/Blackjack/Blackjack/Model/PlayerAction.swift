enum PlayerAction: Hashable {
    case hit
    case stand
    case doubleDown
    case split
    case surrender
    case insurance
    case declineInsurance
    case evenMoney
    case declineEvenMoney

    var displayName: String {
        switch self {
        case .hit: "Hit"
        case .stand: "Stand"
        case .doubleDown: "Double"
        case .split: "Split"
        case .surrender: "Surrender"
        case .insurance: "Insurance"
        case .declineInsurance: "No Insurance"
        case .evenMoney: "Even Money"
        case .declineEvenMoney: "No Even Money"
        }
    }
}
