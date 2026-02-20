package com.jmalinen.blackjack.model

enum class ChartCell(val symbol: String) {
    HIT("H"),
    STAND("-"),
    DOUBLE_HIT("D"),
    DOUBLE_STAND("D/S"),
    SPLIT("Y"),
    SPLIT_HIT("Y/H"),
    SURRENDER_HIT("Rh"),
    SURRENDER_STAND("Rs"),
    SURRENDER_SPLIT("Ry"),
}

data class ChartRow(
    val label: String,
    val cells: List<ChartCell>
)

data class StrategyChartData(
    val hardRows: List<ChartRow>,
    val softRows: List<ChartRow>,
    val pairRows: List<ChartRow>,
)
