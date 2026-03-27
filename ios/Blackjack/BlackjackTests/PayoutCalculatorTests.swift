import Testing
@testable import Blackjack

@Suite("PayoutCalculator Tests")
struct PayoutCalculatorTests {

    // MARK: - Helpers

    private func card(_ rank: Rank, _ suit: Suit = .hearts) -> Card { Card(rank: rank, suit: suit) }

    private func hand(_ ranks: Rank..., bet: Int = 10) -> Hand {
        Hand(cards: ranks.map { card($0) }, bet: bet)
    }

    private func blackjackHand(bet: Int = 10) -> Hand { hand(.ace, .ten, bet: bet) }

    private func bustedHand(bet: Int = 10) -> Hand { hand(.ten, .eight, .five, bet: bet) }

    private func surrenderedHand(bet: Int = 10) -> Hand {
        Hand(cards: [card(.ten), card(.six)], bet: bet, isSurrendered: true)
    }

    private let defaultRules = CasinoRules()

    private func calculate(
        playerHands: [Hand],
        dealerHand: Hand,
        insuranceBet: Int = 0,
        rules: CasinoRules = CasinoRules()
    ) -> PayoutCalculator.RoundResult {
        PayoutCalculator.calculateResults(
            playerHands: playerHands, dealerHand: dealerHand,
            insuranceBet: insuranceBet, rules: rules
        )
    }

    // MARK: - Basic win/lose/push

    @Test("player wins when score is higher than dealer")
    func playerWins() {
        let result = calculate(
            playerHands: [hand(.ten, .nine, bet: 10)],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.handResults[0] == .win)
        #expect(result.totalPayout == 20)
    }

    @Test("player loses when score is lower than dealer")
    func playerLoses() {
        let result = calculate(
            playerHands: [hand(.ten, .seven, bet: 10)],
            dealerHand: hand(.ten, .nine)
        )
        #expect(result.handResults[0] == .lose)
        #expect(result.totalPayout == 0)
    }

    @Test("push when scores are equal")
    func push() {
        let result = calculate(
            playerHands: [hand(.ten, .eight, bet: 10)],
            dealerHand: hand(.nine, .nine)
        )
        #expect(result.handResults[0] == .push)
        #expect(result.totalPayout == 10)
    }

    @Test("player wins when dealer busts")
    func playerWinsWhenDealerBusts() {
        let result = calculate(
            playerHands: [hand(.ten, .seven, bet: 10)],
            dealerHand: hand(.ten, .six, .eight)
        )
        #expect(result.handResults[0] == .win)
        #expect(result.totalPayout == 20)
    }

    // MARK: - Bust

    @Test("player bust loses regardless of dealer")
    func playerBustLosesRegardless() {
        let result = calculate(
            playerHands: [bustedHand(bet: 10)],
            dealerHand: hand(.ten, .six, .eight)
        )
        #expect(result.handResults[0] == .bust)
        #expect(result.totalPayout == 0)
    }

    // MARK: - Blackjack

    @Test("blackjack pays 3 to 2 by default")
    func blackjackPays3To2() {
        let result = calculate(
            playerHands: [blackjackHand(bet: 10)],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.handResults[0] == .blackjack)
        #expect(result.totalPayout == 25)
    }

    @Test("blackjack pays 6 to 5 with rule")
    func blackjackPays6To5() {
        let rules = CasinoRules(blackjackPayout: .sixToFive)
        let result = calculate(
            playerHands: [blackjackHand(bet: 10)],
            dealerHand: hand(.ten, .seven),
            rules: rules
        )
        #expect(result.handResults[0] == .blackjack)
        #expect(result.totalPayout == 22)
    }

    @Test("blackjack vs blackjack is push")
    func blackjackVsBlackjackIsPush() {
        let result = calculate(
            playerHands: [blackjackHand(bet: 10)],
            dealerHand: blackjackHand()
        )
        #expect(result.handResults[0] == .push)
        #expect(result.totalPayout == 10)
    }

    @Test("non-blackjack 21 loses to dealer blackjack")
    func nonBlackjack21LosesToDealerBlackjack() {
        let threeCard21 = hand(.seven, .seven, .seven, bet: 10)
        let result = calculate(
            playerHands: [threeCard21],
            dealerHand: blackjackHand()
        )
        #expect(result.handResults[0] == .lose)
        #expect(result.totalPayout == 0)
    }

    // MARK: - Surrender

    @Test("surrender returns half the bet")
    func surrenderReturnsHalf() {
        let result = calculate(
            playerHands: [surrenderedHand(bet: 10)],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.handResults[0] == .surrender)
        #expect(result.totalPayout == 5)
    }

    @Test("surrender takes priority over other outcomes")
    func surrenderTakesPriority() {
        let result = calculate(
            playerHands: [surrenderedHand(bet: 20)],
            dealerHand: hand(.ten, .six, .eight)
        )
        #expect(result.handResults[0] == .surrender)
        #expect(result.totalPayout == 10)
    }

    // MARK: - Insurance

    @Test("insurance pays 3x when dealer has blackjack")
    func insurancePays3x() {
        let result = calculate(
            playerHands: [hand(.ten, .seven, bet: 10)],
            dealerHand: blackjackHand(),
            insuranceBet: 5
        )
        #expect(result.insurancePayout == 15)
    }

    @Test("insurance pays nothing when dealer has no blackjack")
    func insurancePaysNothing() {
        let result = calculate(
            playerHands: [hand(.ten, .seven, bet: 10)],
            dealerHand: hand(.ace, .six),
            insuranceBet: 5
        )
        #expect(result.insurancePayout == 0)
    }

    @Test("insurance payout included in total")
    func insurancePayoutIncludedInTotal() {
        let result = calculate(
            playerHands: [hand(.ten, .seven, bet: 10)],
            dealerHand: blackjackHand(),
            insuranceBet: 5
        )
        #expect(result.handResults[0] == .lose)
        #expect(result.totalPayout == 15)
    }

    @Test("no insurance bet means zero insurance payout")
    func noInsuranceBetMeansZeroPayout() {
        let result = calculate(
            playerHands: [hand(.ten, .seven, bet: 10)],
            dealerHand: blackjackHand(),
            insuranceBet: 0
        )
        #expect(result.insurancePayout == 0)
    }

    // MARK: - Three sevens bonus

    @Test("three sevens pays 3 to 1 when rule enabled")
    func threeSevensPays3To1() {
        let rules = CasinoRules(threeSevensPays3to1: true)
        let result = calculate(
            playerHands: [hand(.seven, .seven, .seven, bet: 10)],
            dealerHand: hand(.ten, .seven),
            rules: rules
        )
        #expect(result.handResults[0] == .threeSevens)
        #expect(result.totalPayout == 40)
    }

    @Test("three sevens treated as normal win when rule disabled")
    func threeSevensNormalWinWhenDisabled() {
        let result = calculate(
            playerHands: [hand(.seven, .seven, .seven, bet: 10)],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.handResults[0] == .win)
        #expect(result.totalPayout == 20)
    }

    @Test("three sevens takes priority over dealer blackjack")
    func threeSevensTakesPriority() {
        let rules = CasinoRules(threeSevensPays3to1: true)
        let result = calculate(
            playerHands: [hand(.seven, .seven, .seven, bet: 10)],
            dealerHand: blackjackHand(),
            rules: rules
        )
        #expect(result.handResults[0] == .threeSevens)
        #expect(result.totalPayout == 40)
    }

    // MARK: - Multiple hands (split scenarios)

    @Test("multiple hands each calculated independently")
    func multipleHandsIndependent() {
        let result = calculate(
            playerHands: [
                hand(.ten, .nine, bet: 10),
                hand(.ten, .five, bet: 10),
            ],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.handResults[0] == .win)
        #expect(result.handResults[1] == .lose)
        #expect(result.totalPayout == 20)
    }

    @Test("multiple hands with mixed results sum correctly")
    func multipleHandsMixedResults() {
        let result = calculate(
            playerHands: [
                hand(.ten, .nine, bet: 10),
                hand(.ten, .eight, bet: 10),
                bustedHand(bet: 10),
            ],
            dealerHand: hand(.ten, .eight)
        )
        #expect(result.handResults[0] == .win)
        #expect(result.handResults[1] == .push)
        #expect(result.handResults[2] == .bust)
        #expect(result.totalPayout == 30)
    }

    // MARK: - Bet amounts

    @Test("payout scales with bet size")
    func payoutScalesWithBetSize() {
        let result = calculate(
            playerHands: [hand(.ten, .nine, bet: 100)],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.totalPayout == 200)
    }

    @Test("blackjack payout scales with bet size")
    func blackjackPayoutScalesWithBetSize() {
        let result = calculate(
            playerHands: [blackjackHand(bet: 100)],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.totalPayout == 250)
    }

    @Test("doubled down hand pays double the doubled bet")
    func doubledDownHandPays() {
        let doubledHand = Hand(
            cards: [Card(rank: .five, suit: .hearts), Card(rank: .three, suit: .hearts), Card(rank: .ten, suit: .hearts)],
            bet: 20,
            isDoubledDown: true
        )
        let result = calculate(
            playerHands: [doubledHand],
            dealerHand: hand(.ten, .seven)
        )
        #expect(result.handResults[0] == .win)
        #expect(result.totalPayout == 40)
    }
}
