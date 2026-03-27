enum Rank: Int, CaseIterable {
    case two = 2
    case three = 3
    case four = 4
    case five = 5
    case six = 6
    case seven = 7
    case eight = 8
    case nine = 9
    case ten = 10
    case jack = 11
    case queen = 12
    case king = 13
    case ace = 14

    var symbol: String {
        switch self {
        case .two: "2"
        case .three: "3"
        case .four: "4"
        case .five: "5"
        case .six: "6"
        case .seven: "7"
        case .eight: "8"
        case .nine: "9"
        case .ten: "10"
        case .jack: "J"
        case .queen: "Q"
        case .king: "K"
        case .ace: "A"
        }
    }

    var baseValue: Int {
        switch self {
        case .two, .three, .four, .five, .six, .seven, .eight, .nine:
            rawValue
        case .ten, .jack, .queen, .king:
            10
        case .ace:
            11
        }
    }

    var isAce: Bool { self == .ace }

    var isTenValue: Bool { baseValue == 10 && !isAce }

    var hiLoValue: Int {
        switch baseValue {
        case 2...6: +1
        case 7...9: 0
        default: -1
        }
    }
}
