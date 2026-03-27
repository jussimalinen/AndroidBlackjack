# Native iOS Rewrite Plan — Blackjack

## Context

Rewrite the Android Blackjack game as a native iOS app in Swift/SwiftUI, maintaining the same MVVM architecture and feature parity. The clean layer separation (model → engine → viewmodel → UI) allows a bottom-up, test-driven port where each phase is independently verifiable.

---

## Phase 1: Project Setup + Model Layer (~249 lines)

**Create Xcode project** targeting iOS 17+, SwiftUI lifecycle, with folder structure mirroring the Kotlin packages (`Model/`, `Engine/`, `ViewModel/`, `Views/Screens/`, `Views/Components/`, `Views/Theme/`). Two targets: app + unit tests (Swift Testing).

**Port 11 model files** — direct 1:1 translations:

| Kotlin | Swift | Mapping |
|--------|-------|---------|
| `data class` | `struct` (Equatable) | Value semantics preserved naturally |
| `enum class` | `enum: CaseIterable` | Add raw values where needed |
| `Shoe` (mutable) | `class Shoe` | Only mutable reference type in model |
| `List<Card>` | `[Card]` | Immutable by virtue of struct containment |
| `copy()` | Struct mutation | `var s = state; s.field = x; state = s` |

Key files: `Rank.swift`, `Suit.swift`, `Card.swift`, `Hand.swift`, `GameState.swift` (24 fields), `GamePhase.swift`, `PlayerAction.swift`, `HandResult.swift`, `CasinoRules.swift`, `Shoe.swift`, `ChartCell.swift`

**Milestone:** All model types compile. Basic model tests pass (card creation, shoe draw, rank properties).

---

## Phase 2: Engine Layer (~617 lines) + Tests (~1,672 lines)

**Port 6 engine files** — Kotlin `object` singletons become Swift `enum` with `static` methods:

1. `HandEvaluator.swift` — ace-counting score logic
2. `DealerStrategy.swift` — dealer hit/stand rules
3. `BlackjackEngine.swift` — available actions validation
4. `PayoutCalculator.swift` — hand outcomes + payout math
5. `BasicStrategyAdvisor.swift` — **largest file (287 lines)**, hardcoded strategy lookup tables. Port tables exactly as `[Int: [Cell]]` dictionaries
6. `DeviationAdvisor.swift` — card counting deviation rules

**Port 5 test files** simultaneously — these are the correctness guarantee:
- `BasicStrategyAdvisorTests.swift` (545 lines) — strategy table correctness
- `BasicStrategyAdvisorChartTests.swift` (243 lines) — chart data generation
- `BlackjackEngineTests.swift` (339 lines) — action validation
- `PayoutCalculatorTests.swift` (323 lines) — payout calculations
- `DeviationAdvisorTests.swift` (222 lines) — counting deviations

Test translation: JUnit `@Test` → Swift Testing `@Test`, `assertEquals` → `#expect(x == y)`, backtick names → `@Test("descriptive name")`, helper functions `card(rank:)` / `hand(ranks:)` port directly.

**Milestone:** All ~100+ engine tests pass, verifying strategy tables, payouts, and game rules are identical.

---

## Phase 3: ViewModel Layer (~864 lines) + Tests (~363 lines)

### SettingsViewModel (127 lines)
Straightforward port. `@Observable class SettingsViewModel` with `var rules: CasinoRules` and mutator methods + preset application.

### GameViewModel (738 lines) — the complex one

| Kotlin pattern | Swift pattern |
|----------------|---------------|
| `ViewModel()` | `@Observable @MainActor class` |
| `MutableStateFlow<GameState>` | `var state: GameState` (tracked by @Observable) |
| `_state.update { it.copy(...) }` | Direct struct mutation |
| `viewModelScope.launch { }` | `Task { }` stored as `Task<Void, Never>?` |
| `delay(300L)` | `try await Task.sleep(for: .milliseconds(300))` |
| `dealJob?.cancel()` | `dealTask?.cancel()` |
| Companion constants | `private static let` |

The entire class must be `@MainActor` since all state mutations drive UI updates.

**Critical state machine methods to preserve exactly:**
- `deal()` → animated dealing sequence via async Task
- `afterDeal()` → insurance/peek/blackjack checks
- `playExtraPlayers()` → AI player loop
- `hit()` / `stand()` / `doubleDown()` / `split()` / `surrender()`
- `advanceToNextHand()` → split hand progression
- `playDealerHand()` → async dealer draw loop
- `resolveRound()` → payout + result message
- `evaluateCoach()` → strategy feedback with deviation awareness

**Testing approach:** Add `internal var animationSpeedMultiplier: Double = 1.0` so tests can set it to 0, making `Task.sleep` calls instant. Tests use `@MainActor` and `await Task.yield()` for async settling.

**Milestone:** GameViewModel drives a complete game loop. ViewModel tests pass.

---

## Phase 4: UI Layer (~2,500 lines)

### 4.1 Theme (~82 lines)
`Theme.swift` — port all color constants as `Color` extensions (feltGreen, goldAccent, chip colors, etc.). Add hex color initializer.

### 4.2 Components (build bottom-up, ~1,279 lines)

| Compose → SwiftUI mapping |
|---------------------------|
| `Column` → `VStack`, `Row` → `HStack`, `Box` → `ZStack` |
| `Modifier.fillMaxSize()` → `.frame(maxWidth: .infinity, ...)` |
| `BoxWithConstraints` → `GeometryReader` |
| `AnimatedVisibility` → `if` + `withAnimation` + `.transition()` |
| `LaunchedEffect` → `.task` or `.onAppear` |

Port order (dependency chain):
1. **CardView.swift** (130 lines) — 70x100pt rounded rect with rank/suit, face-down back, flip animation
2. **HandView.swift** (159 lines) — ZStack with overlapping card offsets, score badge
3. **DealerArea.swift** (89 lines) — dealer hand display
4. **PlayerArea.swift** (83 lines) — player hands with active highlighting
5. **ExtraPlayersArea.swift** (57 lines) — AI player hands
6. **ActionBar.swift** (101 lines) — action buttons filtered by availableActions
7. **BetSelector.swift** (125 lines) — chip buttons + deal button
8. **GameInfoBar.swift** (193 lines) — top bar with stats, coach toggle, count display
9. **StrategyChart.swift** (342 lines) — scrollable color-coded strategy grid overlay

### 4.3 Screens (~700 lines, excluding previews)
- **GameScreen.swift** (368 lines) — VStack of info bar, dealer area, player area, phase-based action area. Uses `GeometryReader` for responsive card scaling
- **SettingsScreen.swift** (330 lines) — SwiftUI `Form` with `Section`, toggles, pickers, steppers, preset buttons

### 4.4 Navigation
`NavigationStack` with `navigationDestination`. Settings → Game flow. ViewModels owned at app root as `@State`.

**Milestone:** Complete visual app running on iOS simulator. All screens navigable, all game phases playable.

---

## Phase 5: Polish + Platform Integration

- **Haptics** — `UIImpactFeedbackGenerator` on deal, win/loss, button taps
- **Orientation lock** — portrait only (matching Android)
- **Safe areas** — verify on notched devices
- **Accessibility** — `accessibilityLabel` on cards, scores, buttons; Dynamic Type support
- **App icon + launch screen**
- **Optional: `@AppStorage`** for persisting settings preferences between launches
- **SwiftUI `#Preview` blocks** — port key preview states from `GameScreenPreviews.kt` (519 lines)

### End-to-end verification
- All game phases: bet → deal → hit/stand → dealer → result
- Blackjack, insurance, even money, surrender
- Split hands (re-split, split aces)
- Double down
- Game over + reset
- All 4 casino presets (Vegas, European, Favorable, Helsinki)
- Coach mode with/without deviations
- Card counting display
- Strategy chart overlay
- Training modes (soft hands, paired hands)
- Extra players (1 and 2)

---

## Summary

| Phase | What | Approx. lines | Depends on |
|-------|------|---------------|------------|
| 1 | Model types | 250 | — |
| 2 | Engine + tests | 2,300 | Phase 1 |
| 3 | ViewModels + tests | 1,230 | Phase 2 |
| 4 | SwiftUI views | 2,500 | Phase 3 |
| 5 | Polish | varies | Phase 4 |

Total: ~35 Swift source files + 7 test files, mirroring the Android structure.

**Highest-risk areas:**
1. `BasicStrategyAdvisor` table porting — mitigated by 788 lines of existing tests
2. `GameViewModel` async state machine — mitigated by making animation delays testable
3. `GameState` 24-field struct ergonomics — use `var copy = state` mutation pattern
