package com.jmalinen.blackjack.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmalinen.blackjack.model.ChartCell
import com.jmalinen.blackjack.model.ChartRow
import com.jmalinen.blackjack.model.StrategyChartData
import com.jmalinen.blackjack.ui.theme.FeltGreenDark

// Chart cell colors
private val HitColor = Color(0xFFEF5350)
private val StandColor = Color(0xFF66BB6A)
private val DoubleColor = Color(0xFFFFEE58)
private val SplitColor = Color(0xFF42A5F5)
private val SurrenderColor = Color(0xFFCE93D8)

private val CellWidth = 34.dp
private val CellHeight = 30.dp
private val LabelWidth = 50.dp
private val CellFontSize = 11.sp
private val HeaderFontSize = 12.sp

private val DealerColumns = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "A")

private fun ChartCell.color(): Color = when (this) {
    ChartCell.HIT -> HitColor
    ChartCell.STAND -> StandColor
    ChartCell.DOUBLE_HIT, ChartCell.DOUBLE_STAND -> DoubleColor
    ChartCell.SPLIT, ChartCell.SPLIT_HIT -> SplitColor
    ChartCell.SURRENDER_HIT, ChartCell.SURRENDER_STAND, ChartCell.SURRENDER_SPLIT -> SurrenderColor
}

private fun ChartCell.textColor(): Color = when (this) {
    ChartCell.DOUBLE_HIT, ChartCell.DOUBLE_STAND -> Color.Black
    else -> Color.White
}

@Composable
fun StrategyChartOverlay(
    visible: Boolean,
    chartData: StrategyChartData,
    hasSurrender: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) { onDismiss() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltGreenDark)
                .padding(top = statusBarPadding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Title bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Basic Strategy",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "X",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // -- Hard Totals --
                    SectionLabel("HARD TOTALS")
                    DealerHeaderRow()
                    MergedRow("8 or less", "ALWAYS HIT", HitColor)
                    // Individual rows for hard 9-17
                    chartData.hardRows
                        .filter { it.label.toInt() in 9..17 }
                        .forEach { ChartDataRow(it) }
                    MergedRow("18+", "ALWAYS STAY", StandColor)

                    Spacer(modifier = Modifier.height(8.dp))

                    // -- Soft Totals --
                    SectionLabel("SOFT TOTALS")
                    DealerHeaderRow()
                    chartData.softRows
                        .filter { it.label != "A,9" }
                        .forEach { ChartDataRow(it) }
                    MergedRow("A,9", "ALWAYS STAY", StandColor)

                    Spacer(modifier = Modifier.height(8.dp))

                    // -- Pairs --
                    SectionLabel("PAIRS")
                    DealerHeaderRow()
                    chartData.pairRows.forEach { row ->
                        if (row.label == "10,10") {
                            MergedRow("10,10", "ALWAYS STAY", StandColor)
                        } else {
                            ChartDataRow(row)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                ChartLegend(hasSurrender)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun DealerHeaderRow() {
    Row {
        // Empty label cell
        Box(
            modifier = Modifier
                .width(LabelWidth)
                .height(CellHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hand",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
        DealerColumns.forEach { col ->
            Box(
                modifier = Modifier
                    .width(CellWidth)
                    .height(CellHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = col,
                    color = Color.White,
                    fontSize = HeaderFontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ChartDataRow(row: ChartRow) {
    Row {
        // Row label
        Box(
            modifier = Modifier
                .width(LabelWidth)
                .height(CellHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = row.label,
                color = Color.White,
                fontSize = CellFontSize,
                fontWeight = FontWeight.Bold
            )
        }
        // Data cells
        row.cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .width(CellWidth)
                    .height(CellHeight)
                    .padding(0.5.dp)
                    .background(cell.color(), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell.symbol,
                    color = cell.textColor(),
                    fontSize = CellFontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MergedRow(label: String, message: String, color: Color) {
    Row {
        Box(
            modifier = Modifier
                .width(LabelWidth)
                .height(CellHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = CellFontSize,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .width(CellWidth * 10)
                .height(CellHeight)
                .padding(0.5.dp)
                .background(color, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = if (color == DoubleColor) Color.Black else Color.White,
                fontSize = CellFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChartLegend(hasSurrender: Boolean) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = "LEGEND",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem("H = Hit", HitColor)
            LegendItem("- = Stand", StandColor)
            LegendItem("D = Double", DoubleColor, textColor = Color.Black)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem("D/S = Double or Stand", DoubleColor, textColor = Color.Black)
            LegendItem("Y = Split", SplitColor)
        }
        if (hasSurrender) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem("Y/H = Split or Hit", SplitColor)
                LegendItem("Rh = Surrender or Hit", SurrenderColor)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem("Rs = Surrender or Stand", SurrenderColor)
                LegendItem("Ry = Surrender or Split", SurrenderColor)
            }
        } else {
            Spacer(modifier = Modifier.height(2.dp))
            LegendItem("Y/H = Split or Hit", SplitColor)
        }
    }
}

@Composable
private fun LegendItem(text: String, color: Color, textColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(14.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}
