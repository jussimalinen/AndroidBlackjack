# ADB Automated Testing Plan

Plan for an agent to autonomously test the Blackjack app on an Android emulator using ADB.

## Prerequisites

```bash
# Start emulator (headless for CI, or with display for debugging)
emulator -avd Pixel_8_API_35 -no-audio -no-window &
adb wait-for-device
adb shell getprop sys.boot_completed  # poll until "1"

# Build and install
./gradlew installDebug

# Launch app
adb shell am start -n com.jmalinen.blackjack/.MainActivity
sleep 2
```

## Core Tools

### Reading Screen State

```bash
# Dump UI hierarchy (XML with text, bounds, class, enabled, clickable)
adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml

# Screenshot for visual verification
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png
```

The UI dump returns XML with nodes like:
```xml
<node text="DEAL" class="..." bounds="[540,1800][900,1900]" clickable="true" enabled="true" />
```

An agent should parse the XML to locate elements by `text` attribute and extract `bounds` for tap coordinates.

### Interacting with the App

```bash
# Tap at coordinates (center of element bounds)
adb shell input tap <x> <y>

# Swipe (for scrolling settings screen)
adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>

# Wait for animations to settle
sleep 1
```

### Helper: Find and Tap by Text

The agent should implement this pattern for each interaction:

1. Dump UI tree: `adb shell uiautomator dump`
2. Parse XML, find node where `text="<target>"`
3. Extract bounds `[left,top][right,bottom]`
4. Calculate center: `x = (left+right)/2`, `y = (top+bottom)/2`
5. Check `enabled="true"` before tapping
6. Tap: `adb shell input tap x y`
7. Wait for UI to settle: `sleep 0.5` to `sleep 1`

## Element Identification

The app has **no testTag or contentDescription annotations**. All elements must be found by text content in the UI tree.

### Settings Screen Elements

| Element | Text to match | Type |
|---------|--------------|------|
| Presets | `"Vegas"`, `"European"`, `"Favorable"`, `"Helsinki"` | Button |
| Dealer S17 | `"Dealer stands on soft 17"` | Switch |
| Dealer peek | `"Dealer peeks for blackjack"` | Switch |
| Payout | `"3:2"`, `"6:5"` | Button |
| Surrender | `"None"`, `"Late"`, `"Early"` | Button |
| DAS | `"Double after split"` | Switch |
| Start | `"START GAME"` | Button |

**Note:** The settings screen scrolls. Elements below the fold require `adb shell input swipe 540 1500 540 500 300` before they become visible and tappable.

### Betting Phase Elements

| Element | Text to match |
|---------|--------------|
| Chip buttons | `"5"`, `"10"`, `"25"`, `"50"`, `"100"` |
| Clear bet | `"Clear"` |
| Deal | `"DEAL"` |

### Player Turn Elements

| Element | Text to match |
|---------|--------------|
| Hit | `"Hit"` |
| Stand | `"Stand"` |
| Double | `"Double"` |
| Split | `"Split"` |
| Surrender | `"Surr."` |

### Insurance Phase Elements

| Element | Text to match |
|---------|--------------|
| Take insurance | `"Insurance"` |
| Decline insurance | `"No Thanks"` |
| Take even money | `"Even Money"` |
| Decline even money | `"Decline"` |

### Round Complete Elements

| Element | Text to match |
|---------|--------------|
| Next hand | `"Next Hand"` |
| Back to settings | `"Settings"` |

### Game Over Elements

| Element | Text to match |
|---------|--------------|
| Restart | `"Play Again"` |
| Back to settings | `"Settings"` |

### Header Toggles

| Element | Text to match |
|---------|--------------|
| Coach mode | `"Coach"` |
| Card count | `"Count"` (when off) or `"RC:"` prefix (when on) |

## Test Scenarios

### 1. Smoke Test — Settings to Deal to Round Complete

**Goal:** Verify the basic game loop works end to end.

```
1. App launches → verify "Blackjack" title visible in UI dump
2. Tap "Vegas" preset
3. Tap "START GAME"
4. Verify phase: "DEAL" button visible
5. Tap "DEAL"
6. Wait 2s for deal animation (4 cards × 300ms)
7. Dump UI → verify action buttons visible ("Hit", "Stand", etc.)
   - OR if "Insurance"/"Even Money" visible → tap "No Thanks"/"Decline"
   - OR if neither → round resolved early (blackjack), tap "Next Hand"
8. Tap "Stand"
9. Wait 2s for dealer play
10. Verify "Next Hand" visible → round completed
11. Tap "Next Hand"
12. Verify "DEAL" visible → back to betting
```

**Verification at each step:** Dump UI, check expected text nodes exist.

### 2. Full Action Coverage

**Goal:** Exercise every player action at least once across multiple rounds.

```
For each round:
  1. Tap "DEAL", wait for animation
  2. Read UI dump to determine game state:
     a. Insurance offered? → tap "No Thanks"
     b. Round complete? → tap "Next Hand", continue to next round
     c. Player turn? → continue
  3. Read available actions (check which buttons are enabled="true")
  4. Choose action by priority:
     - If "Split" enabled and not yet tested → tap "Split"
     - If "Double" enabled and not yet tested → tap "Double"
     - If "Surr." enabled and not yet tested → tap "Surr."
     - Else alternate "Hit" and "Stand"
  5. If still in player turn after action, tap "Stand"
  6. Wait for round resolution, verify "Next Hand" or "Game Over"
  7. Record which actions were exercised
  8. Repeat until all 5 actions tested (typically 5-15 rounds)
```

**Exit condition:** All of Hit, Stand, Double, Split, Surrender exercised, or 20 rounds played.

### 3. Bet Adjustment

**Goal:** Verify chip buttons and bet clamping.

```
1. Start game with Vegas rules
2. On betting screen, read current bet from UI (text matching "Bet: $XX")
3. Tap "25" chip → verify bet increases
4. Tap "100" chip → verify bet increases
5. Tap "Clear" → verify bet resets to minimum
6. Tap "5" chip 3 times → verify bet increments
7. Tap "DEAL" → verify chips deducted (read "Chips: $XXX")
```

### 4. Settings Presets

**Goal:** Verify each preset configures rules correctly.

```
For each preset in ["Vegas", "European", "Favorable", "Helsinki"]:
  1. Tap preset button
  2. Dump UI → verify switch states changed:
     - Vegas: S17 on, peek on
     - European: S17 on, peek off
     - Favorable: S17 on, peek on, DAS on
     - Helsinki: Three 7s on, peek off
  3. Tap "START GAME"
  4. Play one round (deal → stand)
  5. Tap "Settings" to return
```

### 5. Coach Mode

**Goal:** Verify coach feedback appears after actions.

```
1. Start game, tap "DEAL", wait for deal
2. Handle insurance if offered
3. In player turn, tap "Coach" in header
4. Take an action (e.g., "Hit" or "Stand")
5. Dump UI → verify feedback text present:
   - Match "Correct!" or "Optimal play:" in text nodes
6. Complete round, verify accuracy shown (e.g., "1/1" or "0/1")
```

### 6. Insurance Flow

**Goal:** Exercise insurance and even money paths.

This requires dealer showing an Ace, which is random. Strategy:

```
Loop (max 30 rounds):
  1. Deal
  2. Check if "Insurance" or "Even Money" visible
  3. If insurance offered:
     a. Tap "Insurance" → verify chips deducted
     b. Complete round, record result
     c. Next round: when insurance offered again, tap "No Thanks"
  4. If even money offered:
     a. Tap "Even Money" → verify round ends with "Even money paid!"
     b. Next round: tap "Decline"
  5. Else: stand and continue
```

### 7. Split Hands

**Goal:** Verify split creates two hands and both are playable.

This requires being dealt a pair, which is random. Strategy:

```
Loop (max 30 rounds):
  1. Deal, handle insurance
  2. Check if "Split" button is enabled
  3. If yes:
     a. Tap "Split"
     b. Dump UI → verify "Hand 1 of 2" or similar text
     c. Take actions on first hand (hit/stand)
     d. Take actions on second hand (hit/stand)
     e. Verify round completes
     f. Break — split tested
  4. Else: stand and continue
```

### 8. Game Over and Recovery

**Goal:** Verify game over screen and restart.

```
1. Start game with rules: initialChips=50, minimumBet=50
   (Requires manual settings adjustment or a preset)
2. Deal and lose (hit until bust, or stand and hope dealer wins)
3. If chips reach 0 → verify "Game Over!" text visible
4. Verify "Play Again" button visible
5. Tap "Play Again" → verify betting screen returns with initial chips
```

### 9. Navigation

**Goal:** Verify settings ↔ game navigation works.

```
1. On settings screen, tap "START GAME"
2. Verify game screen ("DEAL" visible)
3. Deal → stand → complete round
4. Tap "Settings" → verify settings screen ("Blackjack" title)
5. Tap "START GAME" again → verify game screen
```

### 10. Surrender Payout

**Goal:** Verify surrender returns half the bet.

```
1. Start with Late Surrender enabled, $100 bet
2. Read chips before round
3. Deal, wait for player turn
4. If "Surr." enabled:
   a. Tap "Surr."
   b. Read chips after round
   c. Verify chips = before - 100 + 50 (half returned)
```

## Agent Loop Structure

The agent should follow this main loop for each test scenario:

```
function runTest(scenario):
    for each step in scenario:
        # 1. Act
        performAction(step.action)
        sleep(step.waitTime)

        # 2. Observe
        uiTree = dumpUI()
        screenshot = captureScreen()

        # 3. Verify
        for assertion in step.assertions:
            if not assertion.check(uiTree):
                report FAIL with screenshot
                return

        # 4. Adapt (handle randomness)
        if unexpectedState(uiTree):
            handleState(uiTree)  # e.g., insurance prompt, early blackjack

    report PASS

function dumpUI():
    run: adb shell uiautomator dump /sdcard/ui.xml
    run: adb shell cat /sdcard/ui.xml
    return parseXML(output)

function tapByText(text):
    tree = dumpUI()
    node = findNodeByText(tree, text)
    assert node.enabled == "true"
    bounds = parseBounds(node.bounds)  # "[left,top][right,bottom]"
    x = (bounds.left + bounds.right) / 2
    y = (bounds.top + bounds.bottom) / 2
    run: adb shell input tap x y
    sleep 0.5

function findNodeByText(tree, text):
    # Search XML for node with matching text attribute
    # Handle partial matches for dynamic content (e.g., "Chips: $")
    return matchingNode or null

function handleState(tree):
    # Adaptive state handler for random game outcomes
    if hasText(tree, "Insurance"):   tapByText("No Thanks")
    if hasText(tree, "Even Money"):  tapByText("Decline")
    if hasText(tree, "Next Hand"):   tapByText("Next Hand")
    if hasText(tree, "Play Again"):  tapByText("Play Again")
    if hasText(tree, "Game Over"):   tapByText("Play Again")
```

## Dealing with Randomness

Card dealing is random, so the agent must adapt:

1. **Can't predict the game phase after deal** — always check the UI tree after dealing to determine if insurance is offered, player has blackjack, or normal play begins.
2. **Can't guarantee specific hands** — tests for split/double/surrender may need multiple rounds. Set a max retry count (20-30 rounds).
3. **Can't predict round outcome** — verify state transitions rather than specific results. Check that chips changed (not by how much), that the phase advanced, that buttons appeared/disappeared.
4. **Training mode as escape hatch** — enable `trainPairedHands` in settings to force pairs for split testing, or `trainSoftHands` to force soft hands for double testing. These are toggleable via switches on the settings screen.

## Suggested Improvements for Better Testability

To make ADB testing more reliable, consider adding to the app:

1. **`testTag` on key elements** — would allow `uiautomator` to find elements by resource-id instead of text
2. **`contentDescription` on action buttons** — accessibility improvement that also aids testing
3. **A debug text overlay** showing `GamePhase` and `hand.score` — easier to parse than inferring state from button visibility
4. **Deterministic seed option** — a debug setting to seed the shoe's shuffle for reproducible test runs
