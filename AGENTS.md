# Agents Guide

## Project Overview

Android Blackjack game built with Jetpack Compose. Players configure casino rules on a settings screen, then play blackjack with hit/stand/double/split/surrender. Includes a coach mode that evaluates plays against basic strategy.

## Build & Run

```bash
./gradlew assembleDebug          # compile
./gradlew installDebug           # install to connected device/emulator
./gradlew test                   # unit tests (no tests written yet)
```

Min SDK 28, Target SDK 35, Kotlin 2.1.0, Compose BOM 2024.12.01.

## Architecture

MVVM with Kotlin StateFlow. No dependency injection. No persistence — all state is session-only.

### Source Layout

All source under `app/src/main/java/com/jmalinen/blackjack/`:

```
model/          Immutable data classes and enums (Card, Hand, GameState, CasinoRules, etc.)
engine/         Pure stateless game logic objects (HandEvaluator, BlackjackEngine, PayoutCalculator, BasicStrategyAdvisor)
viewmodel/      GameViewModel (game orchestration), SettingsViewModel (rules presets)
ui/screens/     GameScreen, SettingsScreen
ui/components/  Composable building blocks (CardView, HandView, ActionBar, BetSelector, DealerArea, PlayerArea, GameInfoBar)
ui/navigation/  NavGraph — settings -> game
ui/theme/       Colors, typography, Material theme
```

### Key Design Decisions

- **GameState** (`model/GameState.kt`) is the single source of truth. It is an immutable data class exposed as `StateFlow<GameState>` from GameViewModel. All UI reads from this one flow.
- **Engine objects** are stateless Kotlin `object` singletons with pure functions. They never hold state. The only mutable state outside GameState is `Shoe` (the card deck), held privately in GameViewModel.
- **GamePhase** enum drives the state machine: `BETTING -> DEALING -> INSURANCE_OFFERED -> DEALER_PEEK -> PLAYER_TURN -> DEALER_TURN -> ROUND_COMPLETE -> GAME_OVER`. The bottom action area in GameScreen switches UI based on phase.
- **Card dealing** is animated via coroutines in GameViewModel (`viewModelScope.launch` with `delay()`). Cards are drawn upfront but added to state one at a time. The dealing coroutine is stored as `dealJob: Job?` and cancelled on new round/reset.
- **Split hands** use a flat `List<Hand>` with `activeHandIndex` tracking which hand the player is acting on. Hands advance sequentially.
- **CasinoRules** is fully configurable (deck count, S17/H17, surrender policy, DAS, payout ratios, Helsinki three-7s bonus, etc.). Rule presets live in SettingsViewModel.
- **Coach mode** uses `BasicStrategyAdvisor` which has hardcoded standard basic strategy tables. It evaluates after each player action and sets `coachFeedback` on GameState.

### Navigation

Two screens with shared ViewModels scoped to the NavHost in `NavGraph.kt`:
- `"settings"` — start destination, rule configuration
- `"game"` — the blackjack table

### UI Layout Constraints

- The bottom action area in GameScreen is a fixed 160dp `Box` to prevent card areas from shifting when buttons appear/disappear.
- Cards are 70x100dp with 28dp overlap offset.
- `GameInfoBar` uses `WindowInsets.statusBars` for safe area padding.

## Conventions

- State updates use `_state.update { it.copy(...) }` pattern
- UI components are stateless `@Composable` functions that receive data and callbacks
- Colors are defined in `ui/theme/Color.kt` (FeltGreen, GoldAccent, chip colors, etc.)
- No string resources — all strings are hardcoded in Kotlin
- No tests exist yet; verify changes with `./gradlew assembleDebug`
