import SwiftUI

extension Color {
    init(hex: UInt, opacity: Double = 1.0) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: opacity
        )
    }

    // Felt greens
    static let feltGreen = Color(hex: 0x1B5E20)
    static let feltGreenLight = Color(hex: 0x2E7D32)
    static let feltGreenDark = Color(hex: 0x0D3B0E)

    // Accents
    static let goldAccent = Color(hex: 0xFFD700)
    static let goldDark = Color(hex: 0xC5A600)

    // Card colors
    static let cardWhite = Color(hex: 0xFFFFF0)
    static let cardRed = Color(hex: 0xD32F2F)
    static let cardBlack = Color(hex: 0x212121)
    static let cardBack = Color(hex: 0x1565C0)
    static let cardBackPattern = Color(hex: 0x0D47A1)

    // Chip colors
    static let chipRed = Color(hex: 0xE53935)
    static let chipBlue = Color(hex: 0x1E88E5)
    static let chipGreen = Color(hex: 0x43A047)
    static let chipBlack = Color(hex: 0x424242)
    static let chipGold = Color(hex: 0xFFB300)
}
