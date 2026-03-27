import SwiftUI

struct ActionBar: View {
    let availableActions: Set<PlayerAction>
    let onHit: () -> Void
    let onStand: () -> Void
    let onDouble: () -> Void
    let onSplit: () -> Void
    let onSurrender: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            ActionButton(text: "Hit", enabled: availableActions.contains(.hit), action: onHit)
            ActionButton(text: "Stand", enabled: availableActions.contains(.stand), action: onStand)
            ActionButton(text: "Double", enabled: availableActions.contains(.doubleDown), action: onDouble)
            ActionButton(text: "Split", enabled: availableActions.contains(.split), action: onSplit)
            ActionButton(text: "Surr.", enabled: availableActions.contains(.surrender), action: onSurrender)
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 12)
    }
}

struct InsuranceBar: View {
    let availableActions: Set<PlayerAction>
    let onInsurance: () -> Void
    let onDecline: () -> Void
    let onEvenMoney: () -> Void
    let onDeclineEvenMoney: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            if availableActions.contains(.evenMoney) {
                ActionButton(text: "Even Money", enabled: true, action: onEvenMoney)
                ActionButton(text: "Decline", enabled: true, action: onDeclineEvenMoney)
            } else {
                ActionButton(text: "Insurance", enabled: availableActions.contains(.insurance), action: onInsurance)
                ActionButton(text: "No Thanks", enabled: availableActions.contains(.declineInsurance), action: onDecline)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private struct ActionButton: View {
    let text: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 13, weight: .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 4)
                .padding(.vertical, 8)
        }
        .disabled(!enabled)
        .background(enabled ? Color.goldAccent : Color.gray.opacity(0.3))
        .foregroundColor(enabled ? .black : .white.opacity(0.4))
        .cornerRadius(8)
    }
}
