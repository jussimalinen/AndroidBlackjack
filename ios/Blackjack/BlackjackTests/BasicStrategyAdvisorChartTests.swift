import Testing
@testable import Blackjack

@Suite("BasicStrategyAdvisor Chart Tests")
struct BasicStrategyAdvisorChartTests {

    private let standardRules = CasinoRules()
    private let h17Rules = CasinoRules(dealerStandsOnSoft17: false)
    private let enhcRules = CasinoRules(dealerPeeks: false)
    private let noDasRules = CasinoRules(doubleAfterSplit: false)

    // Dealer column indices: 0=2, 1=3, 2=4, 3=5, 4=6, 5=7, 6=8, 7=9, 8=10, 9=A

    // MARK: - Dimensions

    @Test("chart data has correct dimensions")
    func chartDataHasCorrectDimensions() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        #expect(data.hardRows.count == 16, "hard rows")
        #expect(data.softRows.count == 8, "soft rows")
        #expect(data.pairRows.count == 10, "pair rows")
        for row in data.hardRows { #expect(row.cells.count == 10, "hard row \(row.label) has 10 cells") }
        for row in data.softRows { #expect(row.cells.count == 10, "soft row \(row.label) has 10 cells") }
        for row in data.pairRows { #expect(row.cells.count == 10, "pair row \(row.label) has 10 cells") }
    }

    // MARK: - Hard totals spot checks (standard rules: S17, peek, DAS)

    @Test("hard 5 through 8 always hit")
    func hard5Through8AlwaysHit() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        for score in 5...8 {
            let row = data.hardRows.first { $0.label == "\(score)" }!
            for (d, cell) in row.cells.enumerated() {
                #expect(cell == .hit, "hard \(score) vs dealer col \(d)")
            }
        }
    }

    @Test("hard 9 doubles vs 3-6 hits otherwise")
    func hard9DoublesVs3to6() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.hardRows.first { $0.label == "9" }!
        #expect(row.cells[0] == .hit, "9 vs 2")
        for d in 1...4 { #expect(row.cells[d] == .doubleHit, "9 vs dealer col \(d)") }
        for d in 5...9 { #expect(row.cells[d] == .hit, "9 vs dealer col \(d)") }
    }

    @Test("hard 16 surrenders vs 9 10 A under standard rules")
    func hard16SurrendersVs9_10_A() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.hardRows.first { $0.label == "16" }!
        #expect(row.cells[7] == .surrenderHit, "16 vs 9")
        #expect(row.cells[8] == .surrenderHit, "16 vs 10")
        #expect(row.cells[9] == .surrenderHit, "16 vs A")
    }

    @Test("hard 12 stands vs 4 5 6 hits otherwise")
    func hard12StandsVs4_5_6() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.hardRows.first { $0.label == "12" }!
        #expect(row.cells[0] == .hit, "12 vs 2")
        #expect(row.cells[1] == .hit, "12 vs 3")
        #expect(row.cells[2] == .stand, "12 vs 4")
        #expect(row.cells[3] == .stand, "12 vs 5")
        #expect(row.cells[4] == .stand, "12 vs 6")
        for d in 5...9 { #expect(row.cells[d] == .hit, "12 vs dealer col \(d)") }
    }

    @Test("hard 17 through 20 always stand under standard rules")
    func hard17Through20AlwaysStand() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        for score in 17...20 {
            let row = data.hardRows.first { $0.label == "\(score)" }!
            for (d, cell) in row.cells.enumerated() {
                #expect(cell == .stand, "hard \(score) vs dealer col \(d)")
            }
        }
    }

    // MARK: - Soft totals spot checks

    @Test("soft 18 double-stand vs 2-6 stand vs 7-8 hit vs 9-A")
    func soft18Strategy() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.softRows.first { $0.label == "A,7" }!
        for d in 0...4 { #expect(row.cells[d] == .doubleStand, "A,7 vs dealer col \(d)") }
        #expect(row.cells[5] == .stand, "A,7 vs 7")
        #expect(row.cells[6] == .stand, "A,7 vs 8")
        #expect(row.cells[7] == .hit, "A,7 vs 9")
        #expect(row.cells[8] == .hit, "A,7 vs 10")
        #expect(row.cells[9] == .hit, "A,7 vs A")
    }

    @Test("soft 20 always stands")
    func soft20AlwaysStands() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.softRows.first { $0.label == "A,9" }!
        for (d, cell) in row.cells.enumerated() {
            #expect(cell == .stand, "A,9 vs dealer col \(d)")
        }
    }

    // MARK: - Pairs spot checks

    @Test("pair tens always stand")
    func pairTensAlwaysStand() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.pairRows.first { $0.label == "10,10" }!
        for (d, cell) in row.cells.enumerated() {
            #expect(cell == .stand, "10,10 vs dealer col \(d)")
        }
    }

    @Test("pair fives treated as hard 10")
    func pairFivesTreatedAsHard10() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let pairFivesRow = data.pairRows.first { $0.label == "5,5" }!
        let hardTenRow = data.hardRows.first { $0.label == "10" }!
        for (d, cell) in pairFivesRow.cells.enumerated() {
            #expect(cell == hardTenRow.cells[d], "5,5 matches hard 10 vs dealer col \(d)")
        }
    }

    @Test("pair aces always split under standard rules")
    func pairAcesAlwaysSplit() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.pairRows.first { $0.label == "A,A" }!
        for (d, cell) in row.cells.enumerated() {
            #expect(cell == .split, "A,A vs dealer col \(d)")
        }
    }

    @Test("pair 8s always split under standard rules")
    func pair8sAlwaysSplit() {
        let data = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let row = data.pairRows.first { $0.label == "8,8" }!
        for (d, cell) in row.cells.enumerated() {
            #expect(cell == .split, "8,8 vs dealer col \(d)")
        }
    }

    // MARK: - H17 rule deviations

    @Test("H17 hard 11 vs A is double")
    func h17Hard11VsAIsDouble() {
        let data = BasicStrategyAdvisor.getChartData(rules: h17Rules)
        let row = data.hardRows.first { $0.label == "11" }!
        #expect(row.cells[9] == .doubleHit, "11 vs A under H17")
    }

    @Test("H17 hard 17 vs A is surrender-stand")
    func h17Hard17VsAIsSurrenderStand() {
        let data = BasicStrategyAdvisor.getChartData(rules: h17Rules)
        let row = data.hardRows.first { $0.label == "17" }!
        #expect(row.cells[9] == .surrenderStand, "17 vs A under H17")
    }

    @Test("H17 hard 15 vs A is surrender-hit")
    func h17Hard15VsAIsSurrenderHit() {
        let data = BasicStrategyAdvisor.getChartData(rules: h17Rules)
        let row = data.hardRows.first { $0.label == "15" }!
        #expect(row.cells[9] == .surrenderHit, "15 vs A under H17")
    }

    @Test("H17 pair 8s vs A is surrender-split")
    func h17Pair8sVsAIsSurrenderSplit() {
        let data = BasicStrategyAdvisor.getChartData(rules: h17Rules)
        let row = data.pairRows.first { $0.label == "8,8" }!
        #expect(row.cells[9] == .surrenderSplit, "8,8 vs A under H17")
    }

    // MARK: - ENHC rule deviations

    @Test("ENHC hard 11 vs 10 is hit not double")
    func enhcHard11Vs10IsHit() {
        let data = BasicStrategyAdvisor.getChartData(rules: enhcRules)
        let row = data.hardRows.first { $0.label == "11" }!
        #expect(row.cells[8] == .hit, "11 vs 10 under ENHC")
    }

    @Test("ENHC pair aces vs A is hit not split")
    func enhcPairAcesVsAIsHit() {
        let data = BasicStrategyAdvisor.getChartData(rules: enhcRules)
        let row = data.pairRows.first { $0.label == "A,A" }!
        #expect(row.cells[9] == .hit, "A,A vs A under ENHC")
    }

    @Test("ENHC pair 8s vs 10 is surrender-hit")
    func enhcPair8sVs10IsSurrenderHit() {
        let data = BasicStrategyAdvisor.getChartData(rules: enhcRules)
        let row = data.pairRows.first { $0.label == "8,8" }!
        #expect(row.cells[8] == .surrenderHit, "8,8 vs 10 under ENHC")
    }

    @Test("ENHC soft 18 vs 2 is stand not double-stand")
    func enhcSoft18Vs2IsStand() {
        let data = BasicStrategyAdvisor.getChartData(rules: enhcRules)
        let row = data.softRows.first { $0.label == "A,7" }!
        #expect(row.cells[0] == .stand, "A,7 vs 2 under ENHC")
    }

    // MARK: - DAS rule effects on pairs

    @Test("DAS pair 6s vs 2 is split, no-DAS is hit")
    func dasPair6sVs2() {
        let dasData = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let noDasData = BasicStrategyAdvisor.getChartData(rules: noDasRules)
        let dasRow = dasData.pairRows.first { $0.label == "6,6" }!
        let noDasRow = noDasData.pairRows.first { $0.label == "6,6" }!
        #expect(dasRow.cells[0] == .split, "6,6 vs 2 with DAS")
        #expect(noDasRow.cells[0] == .hit, "6,6 vs 2 without DAS")
    }

    @Test("DAS pair 4s vs 5-6 splits, no-DAS hits")
    func dasPair4sVs5to6() {
        let dasData = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let noDasData = BasicStrategyAdvisor.getChartData(rules: noDasRules)
        let dasRow = dasData.pairRows.first { $0.label == "4,4" }!
        let noDasRow = noDasData.pairRows.first { $0.label == "4,4" }!
        #expect(dasRow.cells[3] == .split, "4,4 vs 5 with DAS")
        #expect(dasRow.cells[4] == .split, "4,4 vs 6 with DAS")
        #expect(noDasRow.cells[3] == .hit, "4,4 vs 5 without DAS")
        #expect(noDasRow.cells[4] == .hit, "4,4 vs 6 without DAS")
    }

    @Test("DAS pair 2s vs 2-7 splits, no-DAS only vs 4-7")
    func dasPair2sStrategy() {
        let dasData = BasicStrategyAdvisor.getChartData(rules: standardRules)
        let noDasData = BasicStrategyAdvisor.getChartData(rules: noDasRules)
        let dasRow = dasData.pairRows.first { $0.label == "2,2" }!
        let noDasRow = noDasData.pairRows.first { $0.label == "2,2" }!
        for d in 0...5 { #expect(dasRow.cells[d] == .split, "2,2 vs dealer col \(d) with DAS") }
        #expect(noDasRow.cells[0] == .hit, "2,2 vs 2 without DAS")
        #expect(noDasRow.cells[1] == .hit, "2,2 vs 3 without DAS")
        for d in 2...5 { #expect(noDasRow.cells[d] == .split, "2,2 vs dealer col \(d) without DAS") }
    }
}
