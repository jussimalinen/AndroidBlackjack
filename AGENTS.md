# Agents Guide

## Project Overview

Android Blackjack game built with Jetpack Compose. Players configure casino rules on a settings screen, then play blackjack with hit/stand/double/split/surrender. Includes a coach mode that evaluates plays against basic strategy, and a Hi-Lo card counting display.

## Build & Test

```bash
./gradlew assembleDebug                    # compile
./gradlew installDebug                     # install to connected device/emulator
./gradlew :app:testDebugUnitTest           # run all unit tests
./gradlew :app:testDebugUnitTest --tests "com.jmalinen.blackjack.engine.BasicStrategyAdvisorTest"  # single class
```

Min SDK 28, Target SDK 35, Kotlin 2.1.0, Compose BOM 2024.12.01, JVM target 17.

## Architecture

MVVM with Kotlin StateFlow. No dependency injection. No persistence — all state is session-only.

### Source Layout

All source under `app/src/main/java/com/jmalinen/blackjack/`:

```
model/          Immutable data classes and enums
  Card.kt         Card with rank, suit, faceUp
  Rank.kt         Enum with symbol, baseValue, isAce, isTenValue, hiLoValue
  Suit.kt         Enum with symbol and isRed
  Hand.kt         Player/dealer hand with computed score, isSoft, isPair, isBlackjack, isFinished
  GameState.kt    Single source of truth — all game state in one data class
  GamePhase.kt    State machine phases enum
  CasinoRules.kt  Full rule config + BlackjackPayout, SurrenderPolicy enums
  HandResult.kt   Outcome enum (WIN, LOSE, PUSH, BUST, BLACKJACK, SURRENDER, THREE_SEVENS)
  PlayerAction.kt Action enum (HIT, STAND, DOUBLE_DOWN, SPLIT, SURRENDER, INSURANCE, etc.)
  Shoe.kt         Card deck management — shuffle, draw, drawMatching, penetration

engine/         Pure stateless game logic (Kotlin object singletons)
  HandEvaluator.kt      bestScore(cards), isSoft(cards) — ace-aware scoring
  BlackjackEngine.kt    availableActions(), insuranceActions() — what the player can do
  DealerStrategy.kt     shouldHit(hand, rules) — respects S17/H17
  PayoutCalculator.kt   calculateResults() → RoundResult with per-hand results and payouts
  BasicStrategyAdvisor.kt  optimalAction() — hardcoded basic strategy tables with H17/ENHC/DAS deviations

viewmodel/      State orchestration
  GameViewModel.kt      Main game state machine — deal, hit, stand, double, split, surrender, insurance
  SettingsViewModel.kt  Rule presets (Vegas, European, Favorable, Helsinki) and individual toggles

ui/screens/     GameScreen.kt, SettingsScreen.kt
ui/components/  CardView, HandView, ActionBar, BetSelector, DealerArea, PlayerArea, GameInfoBar
ui/navigation/  NavGraph.kt — settings (start) -> game
ui/theme/       Color.kt, Theme.kt (dark Material 3), Type.kt
```

### Key Design Decisions

- **GameState** (`model/GameState.kt`) is the single source of truth. It is an immutable data class exposed as `StateFlow<GameState>` from GameViewModel. All UI reads from this one flow.
- **Engine objects** are stateless Kotlin `object` singletons with pure functions. They never hold state. The only mutable state outside GameState is `Shoe` (the card deck), held privately in GameViewModel.
- **GamePhase** enum drives the state machine: `BETTING -> DEALING -> INSURANCE_OFFERED -> PLAYER_TURN -> DEALER_TURN -> ROUND_COMPLETE -> GAME_OVER`. The bottom action area in GameScreen switches UI based on phase.
- **Card dealing** is animated via coroutines in GameViewModel (`viewModelScope.launch` with `delay()`). Cards are drawn upfront but added to state one at a time. The dealing coroutine is stored as `dealJob: Job?` and cancelled on new round/reset.
- **Split hands** use a flat `List<Hand>` with `activeHandIndex` tracking which hand the player is acting on. Hands advance sequentially.
- **CasinoRules** is fully configurable (deck count, S17/H17, dealer peek, surrender policy, DAS, payout ratios, Helsinki three-7s bonus, training mode, etc.). Rule presets live in SettingsViewModel.
- **Coach mode** uses `BasicStrategyAdvisor` which has hardcoded basic strategy tables with rule-specific deviations. It evaluates after each player action and sets `coachFeedback` on GameState.
- **Card counting** uses Hi-Lo system. Running count updated on each card draw; hole card counted when revealed. True count = running count / decks remaining.

### Navigation

Two screens with shared ViewModels scoped to the NavHost in `NavGraph.kt`:
- `"settings"` — start destination, rule configuration
- `"game"` — the blackjack table

### UI Layout Constraints

- The bottom action area in GameScreen is a fixed 160dp `Box` to prevent card areas from shifting when buttons appear/disappear.
- Cards are 70x100dp with 28dp overlap offset.
- `GameInfoBar` uses `WindowInsets.statusBars` for safe area padding.

## Engine Details

### BasicStrategyAdvisor

Lookup tables for hard totals (5-20), soft totals (13-20), and pairs. Each cell encodes an action with fallback:
- `H` Hit, `S` Stand, `D` Double→Hit, `Ds` Double→Stand, `P` Split, `Rh` Surrender→Hit, `Rs` Surrender→Stand, `Rp` Surrender→Split

Rule-specific deviations handled in strategy functions:
- **H17** (dealer hits soft 17): double 11 vs A, surrender 17/15 vs A, surrender 8-8 vs A
- **ENHC** (no hole card / no peek): don't double 11 vs 10, surrender 14 vs 10, multi-card 16 vs 10 stands, don't split A-A/8-8 vs A, soft 18 vs 2 stands, soft 19 vs 6 stands
- **DAS**: affects pair splitting for 2s, 3s, 4s, 6s

### PayoutCalculator

Priority order in `when` block: surrendered → busted → three-7s bonus → player BJ vs non-BJ → BJ push → dealer BJ wins → dealer bust → score comparison → push. Insurance pays 3x when dealer has blackjack.

### BlackjackEngine

`availableActions()` checks: hand finished → split-from-aces restriction → HIT+STAND always → double (2 cards, chips, DAS rule) → split (pair, max hands, resplit aces) → surrender (2 cards, not split, single hand, policy).

## Tests

Tests under `app/src/test/java/com/jmalinen/blackjack/`:

```
engine/
  BasicStrategyAdvisorTest.kt   Hard/soft/pair tables, H17/ENHC/DAS deviations, action fallbacks
  BlackjackEngineTest.kt        Available actions, split/double/surrender rules, insurance
  PayoutCalculatorTest.kt       Win/lose/push, blackjack payouts, surrender, three-7s, insurance

viewmodel/
  GameViewModelTest.kt          State transitions, player actions, coach feedback, phase guards
```

### Test conventions

- JUnit 4 with `org.junit.Assert`
- `kotlinx-coroutines-test` for ViewModel tests (`UnconfinedTestDispatcher`, `runTest`, `advanceUntilIdle`)
- Helper functions: `card(rank)`, `hand(vararg ranks)` for test data
- Engine tests use pure function calls — no mocking needed
- ViewModel tests use reflection to set `_state` for deterministic scenarios (the `Shoe` is not injectable)
- After calling ViewModel actions that trigger dealer play, use `advanceUntilIdle()` to complete the coroutine. But don't advance if you need to assert intermediate state (e.g., chip count before round resolution)

### Running tests

```bash
./gradlew :app:testDebugUnitTest                    # all tests
./gradlew :app:testDebugUnitTest --tests "*.PayoutCalculatorTest"  # single class
```

## Conventions

- State updates use `_state.update { it.copy(...) }` pattern
- UI components are stateless `@Composable` functions that receive data and callbacks
- Colors are defined in `ui/theme/Color.kt` (FeltGreen, GoldAccent, chip colors, etc.)
- No string resources — all strings are hardcoded in Kotlin
- No external libraries beyond AndroidX/Compose — no DI, no Room, no network
