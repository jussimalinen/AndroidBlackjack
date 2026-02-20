package com.jmalinen.blackjack.engine

import com.jmalinen.blackjack.model.CasinoRules
import com.jmalinen.blackjack.model.ChartCell.*
import org.junit.Assert.assertEquals
import org.junit.Test

class BasicStrategyAdvisorChartTest {

    private val standardRules = CasinoRules()  // S17, peek, DAS
    private val h17Rules = CasinoRules(dealerStandsOnSoft17 = false)
    private val enhcRules = CasinoRules(dealerPeeks = false)
    private val noDasRules = CasinoRules(doubleAfterSplit = false)

    // Dealer column indices: 0=2, 1=3, 2=4, 3=5, 4=6, 5=7, 6=8, 7=9, 8=10, 9=A

    // -- Dimensions --

    @Test
    fun `chart data has correct dimensions`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        assertEquals("hard rows", 16, data.hardRows.size)         // scores 5-20
        assertEquals("soft rows", 8, data.softRows.size)          // scores 13-20
        assertEquals("pair rows", 10, data.pairRows.size)         // A,10,9,8,7,6,5,4,3,2
        data.hardRows.forEach { assertEquals("hard row ${it.label} has 10 cells", 10, it.cells.size) }
        data.softRows.forEach { assertEquals("soft row ${it.label} has 10 cells", 10, it.cells.size) }
        data.pairRows.forEach { assertEquals("pair row ${it.label} has 10 cells", 10, it.cells.size) }
    }

    // -- Hard totals spot checks (standard rules: S17, peek, DAS) --

    @Test
    fun `hard 5 through 8 always hit`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        for (score in 5..8) {
            val row = data.hardRows.first { it.label == "$score" }
            row.cells.forEachIndexed { d, cell ->
                assertEquals("hard $score vs dealer col $d", HIT, cell)
            }
        }
    }

    @Test
    fun `hard 9 doubles vs 3-6 hits otherwise`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.hardRows.first { it.label == "9" }
        assertEquals("9 vs 2", HIT, row.cells[0])
        for (d in 1..4) assertEquals("9 vs dealer col $d", DOUBLE_HIT, row.cells[d])
        for (d in 5..9) assertEquals("9 vs dealer col $d", HIT, row.cells[d])
    }

    @Test
    fun `hard 16 surrenders vs 9 10 A under standard rules`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.hardRows.first { it.label == "16" }
        assertEquals("16 vs 9", SURRENDER_HIT, row.cells[7])
        assertEquals("16 vs 10", SURRENDER_HIT, row.cells[8])
        assertEquals("16 vs A", SURRENDER_HIT, row.cells[9])
    }

    @Test
    fun `hard 12 stands vs 4 5 6 hits otherwise`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.hardRows.first { it.label == "12" }
        assertEquals("12 vs 2", HIT, row.cells[0])
        assertEquals("12 vs 3", HIT, row.cells[1])
        assertEquals("12 vs 4", STAND, row.cells[2])
        assertEquals("12 vs 5", STAND, row.cells[3])
        assertEquals("12 vs 6", STAND, row.cells[4])
        for (d in 5..9) assertEquals("12 vs dealer col $d", HIT, row.cells[d])
    }

    @Test
    fun `hard 17 through 20 always stand under standard rules`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        for (score in 17..20) {
            val row = data.hardRows.first { it.label == "$score" }
            row.cells.forEachIndexed { d, cell ->
                assertEquals("hard $score vs dealer col $d", STAND, cell)
            }
        }
    }

    // -- Soft totals spot checks --

    @Test
    fun `soft 18 double-stand vs 2-6 stand vs 7-8 hit vs 9-A`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.softRows.first { it.label == "A,7" }
        for (d in 0..4) assertEquals("A,7 vs dealer col $d", DOUBLE_STAND, row.cells[d])
        assertEquals("A,7 vs 7", STAND, row.cells[5])
        assertEquals("A,7 vs 8", STAND, row.cells[6])
        assertEquals("A,7 vs 9", HIT, row.cells[7])
        assertEquals("A,7 vs 10", HIT, row.cells[8])
        assertEquals("A,7 vs A", HIT, row.cells[9])
    }

    @Test
    fun `soft 20 always stands`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.softRows.first { it.label == "A,9" }
        row.cells.forEachIndexed { d, cell ->
            assertEquals("A,9 vs dealer col $d", STAND, cell)
        }
    }

    // -- Pairs spot checks --

    @Test
    fun `pair tens always stand`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.pairRows.first { it.label == "10,10" }
        row.cells.forEachIndexed { d, cell ->
            assertEquals("10,10 vs dealer col $d", STAND, cell)
        }
    }

    @Test
    fun `pair fives treated as hard 10`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val pairFivesRow = data.pairRows.first { it.label == "5,5" }
        val hardTenRow = data.hardRows.first { it.label == "10" }
        pairFivesRow.cells.forEachIndexed { d, cell ->
            assertEquals("5,5 matches hard 10 vs dealer col $d", hardTenRow.cells[d], cell)
        }
    }

    @Test
    fun `pair aces always split under standard rules`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.pairRows.first { it.label == "A,A" }
        row.cells.forEachIndexed { d, cell ->
            assertEquals("A,A vs dealer col $d", SPLIT, cell)
        }
    }

    @Test
    fun `pair 8s always split under standard rules`() {
        val data = BasicStrategyAdvisor.getChartData(standardRules)
        val row = data.pairRows.first { it.label == "8,8" }
        row.cells.forEachIndexed { d, cell ->
            assertEquals("8,8 vs dealer col $d", SPLIT, cell)
        }
    }

    // -- H17 rule deviations --

    @Test
    fun `H17 hard 11 vs A is double`() {
        val data = BasicStrategyAdvisor.getChartData(h17Rules)
        val row = data.hardRows.first { it.label == "11" }
        assertEquals("11 vs A under H17", DOUBLE_HIT, row.cells[9])
    }

    @Test
    fun `H17 hard 17 vs A is surrender-stand`() {
        val data = BasicStrategyAdvisor.getChartData(h17Rules)
        val row = data.hardRows.first { it.label == "17" }
        assertEquals("17 vs A under H17", SURRENDER_STAND, row.cells[9])
    }

    @Test
    fun `H17 hard 15 vs A is surrender-hit`() {
        val data = BasicStrategyAdvisor.getChartData(h17Rules)
        val row = data.hardRows.first { it.label == "15" }
        assertEquals("15 vs A under H17", SURRENDER_HIT, row.cells[9])
    }

    @Test
    fun `H17 pair 8s vs A is surrender-split`() {
        val data = BasicStrategyAdvisor.getChartData(h17Rules)
        val row = data.pairRows.first { it.label == "8,8" }
        assertEquals("8,8 vs A under H17", SURRENDER_SPLIT, row.cells[9])
    }

    // -- ENHC rule deviations --

    @Test
    fun `ENHC hard 11 vs 10 is hit not double`() {
        val data = BasicStrategyAdvisor.getChartData(enhcRules)
        val row = data.hardRows.first { it.label == "11" }
        assertEquals("11 vs 10 under ENHC", HIT, row.cells[8])
    }

    @Test
    fun `ENHC pair aces vs A is hit not split`() {
        val data = BasicStrategyAdvisor.getChartData(enhcRules)
        val row = data.pairRows.first { it.label == "A,A" }
        assertEquals("A,A vs A under ENHC", HIT, row.cells[9])
    }

    @Test
    fun `ENHC pair 8s vs 10 is surrender-hit`() {
        val data = BasicStrategyAdvisor.getChartData(enhcRules)
        val row = data.pairRows.first { it.label == "8,8" }
        assertEquals("8,8 vs 10 under ENHC", SURRENDER_HIT, row.cells[8])
    }

    @Test
    fun `ENHC soft 18 vs 2 is stand not double-stand`() {
        val data = BasicStrategyAdvisor.getChartData(enhcRules)
        val row = data.softRows.first { it.label == "A,7" }
        assertEquals("A,7 vs 2 under ENHC", STAND, row.cells[0])
    }

    // -- DAS rule effects on pairs --

    @Test
    fun `DAS pair 6s vs 2 is split, no-DAS is hit`() {
        val dasData = BasicStrategyAdvisor.getChartData(standardRules)
        val noDasData = BasicStrategyAdvisor.getChartData(noDasRules)
        val dasRow = dasData.pairRows.first { it.label == "6,6" }
        val noDasRow = noDasData.pairRows.first { it.label == "6,6" }
        assertEquals("6,6 vs 2 with DAS", SPLIT, dasRow.cells[0])
        assertEquals("6,6 vs 2 without DAS", HIT, noDasRow.cells[0])
    }

    @Test
    fun `DAS pair 4s vs 5-6 splits, no-DAS hits`() {
        val dasData = BasicStrategyAdvisor.getChartData(standardRules)
        val noDasData = BasicStrategyAdvisor.getChartData(noDasRules)
        val dasRow = dasData.pairRows.first { it.label == "4,4" }
        val noDasRow = noDasData.pairRows.first { it.label == "4,4" }
        assertEquals("4,4 vs 5 with DAS", SPLIT, dasRow.cells[3])
        assertEquals("4,4 vs 6 with DAS", SPLIT, dasRow.cells[4])
        assertEquals("4,4 vs 5 without DAS", HIT, noDasRow.cells[3])
        assertEquals("4,4 vs 6 without DAS", HIT, noDasRow.cells[4])
    }

    @Test
    fun `DAS pair 2s vs 2-7 splits, no-DAS only vs 4-7`() {
        val dasData = BasicStrategyAdvisor.getChartData(standardRules)
        val noDasData = BasicStrategyAdvisor.getChartData(noDasRules)
        val dasRow = dasData.pairRows.first { it.label == "2,2" }
        val noDasRow = noDasData.pairRows.first { it.label == "2,2" }
        // DAS: split vs 2-7
        for (d in 0..5) assertEquals("2,2 vs dealer col $d with DAS", SPLIT, dasRow.cells[d])
        // No-DAS: hit vs 2-3, split vs 4-7
        assertEquals("2,2 vs 2 without DAS", HIT, noDasRow.cells[0])
        assertEquals("2,2 vs 3 without DAS", HIT, noDasRow.cells[1])
        for (d in 2..5) assertEquals("2,2 vs dealer col $d without DAS", SPLIT, noDasRow.cells[d])
    }
}
