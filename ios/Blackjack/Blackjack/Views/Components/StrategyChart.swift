import SwiftUI

private let hitColor = Color(hex: 0xEF5350)
private let standColor = Color(hex: 0x66BB6A)
private let doubleColor = Color(hex: 0xFFEE58)
private let splitColor = Color(hex: 0x42A5F5)
private let surrenderColor = Color(hex: 0xCE93D8)

private let cellWidth: CGFloat = 34
private let cellHeight: CGFloat = 30
private let labelWidth: CGFloat = 50
private let cellFontSize: CGFloat = 11
private let headerFontSize: CGFloat = 12

private let dealerColumns = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "A"]

private func cellColor(_ cell: ChartCell) -> Color {
    switch cell {
    case .hit: hitColor
    case .stand: standColor
    case .doubleHit, .doubleStand: doubleColor
    case .split, .splitHit: splitColor
    case .surrenderHit, .surrenderStand, .surrenderSplit: surrenderColor
    }
}

private func cellTextColor(_ cell: ChartCell) -> Color {
    switch cell {
    case .doubleHit, .doubleStand: .black
    default: .white
    }
}

struct StrategyChartOverlay: View {
    let visible: Bool
    let chartData: StrategyChartData
    let hasSurrender: Bool
    let onDismiss: () -> Void

    var body: some View {
        if visible {
            ZStack {
                Color.feltGreenDark.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        // Title bar
                        HStack {
                            Text("Basic Strategy")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.white)
                            Spacer()
                            Button("X") { onDismiss() }
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white.opacity(0.7))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 4)
                        }

                        Spacer().frame(height: 8)

                        ScrollView(.horizontal) {
                            VStack(alignment: .leading, spacing: 0) {
                                // Hard totals
                                SectionLabel(text: "HARD TOTALS")
                                DealerHeaderRow()
                                MergedRow(label: "8 or less", message: "ALWAYS HIT", color: hitColor)
                                ForEach(chartData.hardRows.filter {
                                    if let v = Int($0.label) { return (9...17).contains(v) }
                                    return false
                                }, id: \.label) { row in
                                    ChartDataRow(row: row)
                                }
                                MergedRow(label: "18+", message: "ALWAYS STAY", color: standColor)

                                Spacer().frame(height: 8)

                                // Soft totals
                                SectionLabel(text: "SOFT TOTALS")
                                DealerHeaderRow()
                                ForEach(chartData.softRows.filter { $0.label != "A,9" }, id: \.label) { row in
                                    ChartDataRow(row: row)
                                }
                                MergedRow(label: "A,9", message: "ALWAYS STAY", color: standColor)

                                Spacer().frame(height: 8)

                                // Pairs
                                SectionLabel(text: "PAIRS")
                                DealerHeaderRow()
                                ForEach(chartData.pairRows, id: \.label) { row in
                                    if row.label == "10,10" {
                                        MergedRow(label: "10,10", message: "ALWAYS STAY", color: standColor)
                                    } else {
                                        ChartDataRow(row: row)
                                    }
                                }
                            }
                        }

                        Spacer().frame(height: 12)
                        ChartLegend(hasSurrender: hasSurrender)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 8)
                }
            }
            .transition(.opacity)
        }
    }
}

private struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(.white.opacity(0.6))
            .padding(.vertical, 2)
    }
}

private struct DealerHeaderRow: View {
    var body: some View {
        HStack(spacing: 0) {
            Text("Hand")
                .font(.system(size: 10))
                .foregroundColor(.white.opacity(0.5))
                .frame(width: labelWidth, height: cellHeight)
            ForEach(dealerColumns, id: \.self) { col in
                Text(col)
                    .font(.system(size: headerFontSize, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: cellWidth, height: cellHeight)
            }
        }
    }
}

private struct ChartDataRow: View {
    let row: ChartRow
    var body: some View {
        HStack(spacing: 0) {
            Text(row.label)
                .font(.system(size: cellFontSize, weight: .bold))
                .foregroundColor(.white)
                .frame(width: labelWidth, height: cellHeight, alignment: .leading)
            ForEach(Array(row.cells.enumerated()), id: \.offset) { _, cell in
                Text(cell.symbol)
                    .font(.system(size: cellFontSize, weight: .bold))
                    .foregroundColor(cellTextColor(cell))
                    .frame(width: cellWidth, height: cellHeight)
                    .background(cellColor(cell))
                    .cornerRadius(2)
                    .padding(0.5)
            }
        }
    }
}

private struct MergedRow: View {
    let label: String
    let message: String
    let color: Color
    var body: some View {
        HStack(spacing: 0) {
            Text(label)
                .font(.system(size: cellFontSize, weight: .bold))
                .foregroundColor(.white)
                .frame(width: labelWidth, height: cellHeight, alignment: .leading)
            Text(message)
                .font(.system(size: cellFontSize, weight: .bold))
                .foregroundColor(color == doubleColor ? .black : .white)
                .frame(width: cellWidth * 10, height: cellHeight)
                .background(color)
                .cornerRadius(2)
                .padding(0.5)
        }
    }
}

private struct ChartLegend: View {
    let hasSurrender: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("LEGEND")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(.white.opacity(0.6))
            Spacer().frame(height: 4)
            HStack(spacing: 12) {
                LegendItem(text: "H = Hit", color: hitColor)
                LegendItem(text: "- = Stand", color: standColor)
                LegendItem(text: "D = Double", color: doubleColor)
            }
            HStack(spacing: 12) {
                LegendItem(text: "D/S = Double or Stand", color: doubleColor)
                LegendItem(text: "Y = Split", color: splitColor)
            }
            if hasSurrender {
                HStack(spacing: 12) {
                    LegendItem(text: "Y/H = Split or Hit", color: splitColor)
                    LegendItem(text: "Rh = Surrender or Hit", color: surrenderColor)
                }
                HStack(spacing: 12) {
                    LegendItem(text: "Rs = Surrender or Stand", color: surrenderColor)
                    LegendItem(text: "Ry = Surrender or Split", color: surrenderColor)
                }
            } else {
                LegendItem(text: "Y/H = Split or Hit", color: splitColor)
            }
        }
        .padding(.top, 4)
    }
}

private struct LegendItem: View {
    let text: String
    let color: Color
    var body: some View {
        HStack(spacing: 4) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color)
                .frame(width: 14, height: 14)
            Text(text)
                .font(.system(size: 10))
                .foregroundColor(.white.opacity(0.8))
        }
    }
}
