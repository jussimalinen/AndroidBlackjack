import SwiftUI

struct GameInfoBar: View {
    let chips: Int
    let shoePenetration: Float
    let handsPlayed: Int
    let handsWon: Int
    let coachEnabled: Bool
    let onToggleCoach: () -> Void
    let deviationsEnabled: Bool
    let onToggleDeviations: () -> Void
    let showCount: Bool
    let runningCount: Int
    let trueCount: Float
    let onToggleCount: () -> Void
    let onShowChart: () -> Void
    let onEnd: () -> Void
    var coachFeedback: String = ""
    var coachCorrect: Int = 0
    var coachTotal: Int = 0

    var body: some View {
        VStack(spacing: 4) {
            // Top row: chips, win ratio, toggles
            HStack {
                HStack(spacing: 4) {
                    Text("Chips:")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.7))
                    Text("$\(chips)")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.goldAccent)
                }

                Spacer()

                Text("W: \(handsWon) / \(handsPlayed)")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.7))

                Spacer()

                HStack(spacing: 6) {
                    TogglePill(
                        label: "Coach",
                        isOn: coachEnabled,
                        activeColor: Color(hex: 0x4CAF50),
                        action: onToggleCoach
                    )

                    if coachEnabled {
                        TogglePill(
                            label: "Dev",
                            isOn: deviationsEnabled,
                            activeColor: Color(hex: 0xFF9800),
                            action: onToggleDeviations
                        )
                    }

                    let trueCountStr = trueCount >= 0 ? String(format: "+%.1f", trueCount) : String(format: "%.1f", trueCount)
                    let rcStr = runningCount >= 0 ? "+\(runningCount)" : "\(runningCount)"
                    TogglePill(
                        label: showCount ? "RC:\(rcStr) TC:\(trueCountStr)" : "Count",
                        isOn: showCount,
                        activeColor: Color(hex: 0x42A5F5),
                        action: onToggleCount
                    )
                }
            }

            // Shoe progress
            HStack(spacing: 8) {
                Text("Shoe")
                    .font(.system(size: 11))
                    .foregroundColor(.white.opacity(0.5))
                ProgressView(value: Double(1 - shoePenetration))
                    .tint(shoePenetration > 0.7 ? Color(hex: 0xFF9800) : .goldAccent)
                    .frame(height: 6)
            }

            // Bottom row: buttons + coach feedback
            HStack(spacing: 8) {
                Button("End") { onEnd() }
                    .font(.system(size: 14))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.white.opacity(0.5), lineWidth: 1)
                    )

                Button("Chart") { onShowChart() }
                    .font(.system(size: 14))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.white.opacity(0.5), lineWidth: 1)
                    )

                if coachEnabled && !coachFeedback.isEmpty {
                    let isCorrect = coachFeedback.hasPrefix("Correct")
                    let suffix = coachTotal > 0 ? " (\(coachCorrect)/\(coachTotal))" : ""
                    Text(coachFeedback + suffix)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(isCorrect ? Color(hex: 0x4CAF50) : Color(hex: 0xFF9800))
                        .lineLimit(2)
                }

                Spacer()
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.feltGreenDark)
    }
}

private struct TogglePill: View {
    let label: String
    let isOn: Bool
    let activeColor: Color
    let action: () -> Void

    var body: some View {
        Text(label)
            .font(.system(size: 12, weight: isOn ? .bold : .regular))
            .foregroundColor(isOn ? activeColor : .white.opacity(0.4))
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(
                isOn
                    ? activeColor.opacity(0.2)
                    : Color.clear
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isOn ? Color.clear : Color.white.opacity(0.3), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .onTapGesture { action() }
    }
}
