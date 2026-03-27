# CLAUDE.md

## Project

Android Blackjack game — Jetpack Compose, Kotlin, MVVM with StateFlow. Configurable casino rules, basic strategy coach, Hi-Lo card counting. No external dependencies beyond AndroidX/Compose.

## Build & Test

```bash
./gradlew assembleDebug                    # compile
./gradlew :app:testDebugUnitTest           # all unit tests
./gradlew :app:testDebugUnitTest --tests "com.jmalinen.blackjack.engine.BasicStrategyAdvisorTest"  # single class
```

Min SDK 28, Target SDK 35, Kotlin 2.1.0, Compose BOM 2024.12.01, JVM target 17.

**Always run `./gradlew :app:testDebugUnitTest` after changing engine, model, or ViewModel code.**

## Architecture

MVVM with Kotlin StateFlow. No DI, no persistence, no network — all state is session-only.

### Source layout

All source under `app/src/main/java/com/jmalinen/blackjack/`:

- `model/` — Immutable data classes and enums (GameState, Hand, Card, Rank, Suit, CasinoRules, Shoe, etc.)
- `engine/` — Stateless game logic as Kotlin `object` singletons (BasicStrategyAdvisor, BlackjackEngine, DealerStrategy, PayoutCalculator, HandEvaluator, DeviationAdvisor)
- `viewmodel/` — GameViewModel (state machine), SettingsViewModel (rule presets)
- `ui/screens/` — GameScreen, SettingsScreen
- `ui/components/` — CardView, HandView, ActionBar, BetSelector, DealerArea, PlayerArea, GameInfoBar
- `ui/navigation/` — NavGraph (settings → game)
- `ui/theme/` — Color, Theme (dark Material 3), Type

Tests under `app/src/test/java/com/jmalinen/blackjack/`:

- `engine/` — BasicStrategyAdvisorTest, BlackjackEngineTest, PayoutCalculatorTest, DeviationAdvisorTest
- `viewmodel/` — GameViewModelTest

## Key Patterns

### State management
- `GameState` is the **single source of truth** — an immutable data class exposed as `StateFlow<GameState>` from GameViewModel.
- State updates use `_state.update { it.copy(...) }` pattern. Never mutate state directly.
- UI collects state with `collectAsStateWithLifecycle()`.

### Engine objects
- All engine objects are stateless Kotlin `object` singletons with pure functions. They never hold state.
- The only mutable state outside GameState is `Shoe` (card deck), held privately in GameViewModel.
- Call engine methods directly: `BasicStrategyAdvisor.optimalAction(...)`, `PayoutCalculator.calculateResults(...)`.

### GamePhase state machine
`BETTING → DEALING → INSURANCE_OFFERED → PLAYER_TURN → DEALER_TURN → ROUND_COMPLETE → GAME_OVER`

### ViewModels
- Created at NavGraph level and passed explicitly to screens as parameters.
- Both GameViewModel and SettingsViewModel are shared across screens via the NavHost scope.

### UI composables
- Stateless `@Composable` functions that receive data and callbacks as parameters.
- Bottom action area is a fixed 160dp Box to prevent layout shifts.
- Cards are 70x100dp with 28dp overlap offset.

## Code Conventions

- No string resources — all strings are hardcoded in Kotlin.
- No external libraries beyond AndroidX/Compose — no DI, no Room, no network.
- Colors defined in `ui/theme/Color.kt` (FeltGreen, GoldAccent, chip colors).
- Companion object constants use UPPER_SNAKE_CASE (e.g., `DEAL_CARD_DELAY`).
- Private backing fields prefixed with `_` (e.g., `_state`).
- Wildcard imports for enums (`Rank.*`, `PlayerAction.*`) in tests.

## Test Conventions

- **JUnit 4** with `org.junit.Assert`.
- Test names use **backtick-enclosed descriptive behavior** statements: `` `hard 5 through 8 always hit` ``.
- Helper functions: `card(rank)`, `hand(vararg ranks)` for test data creation.
- Engine tests are pure function calls — no mocking, no coroutine setup.
- ViewModel tests use `UnconfinedTestDispatcher`, `runTest`, `advanceUntilIdle()`.
- ViewModel tests use **reflection** to set `_state` for deterministic scenarios (`setupState` helper).
- Loop-based parametrized testing for strategy tables with descriptive assertion messages.
- Don't call `advanceUntilIdle()` if asserting intermediate state before round resolution.

## When Writing New Code

- New game logic goes in `engine/` as methods on existing `object` singletons or new `object` singletons.
- New state fields go in `GameState` data class with a default value.
- New UI sections go in `ui/components/` as stateless composables; wire them in the relevant screen.
- New rules go in `CasinoRules` data class; update SettingsViewModel presets if needed.
- New tests follow existing patterns: factory helpers, backtick names, section comments.
- Card dealing animations use coroutines with `delay()` in GameViewModel — store as `Job?` and cancel on new round/reset.
