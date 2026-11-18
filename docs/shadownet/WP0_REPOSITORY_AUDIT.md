# ShadowNet WP0 — Comprehensive Repository Audit

**Status:** Final audit — read-only, no implementation performed  
**Date:** 2026-11-18  
**Repository:** `/home/zayne/Shadownet`  
**Application:** `com.shadownet.calculator`, version 1.0.0 (versionCode 1)  
**Scope:** repository, calculator implementation, accessibility, security/privacy, backend/infrastructure, ShadowNet requirements gap analysis, architecture, build/test verification.  
**Deliverable:** this report only. No application source code was modified, no alerts were sent, no survivor data was collected, and no credentials are reproduced in this document.

---

## Evidence and classification conventions

Every significant finding is tagged:

- **[V] Verified** — observed directly in source, build output, APK metadata, automated test output, or on-device UI/accessibility inspection.
- **[A] Assumption** — inferred from code or platform behavior but not directly tested.
- **[R] Recommendation** — proposed remediation or design direction.

Severity:

| Severity | Meaning |
|---|---|
| **CRITICAL** | Prevents safe use of the product, defeats the security/privacy model, or blocks release. |
| **HIGH** | Materially breaks a mandatory requirement or creates a realistic safety/privacy risk. |
| **MEDIUM** | Significant quality, accessibility, reliability or maintainability problem. |
| **LOW** | Minor defect, hygiene issue or technical debt. |
| **INFORMATIONAL** | Neutral observation or positive control. |

A finding marked “requires verification” is not a claim; it is a test that has not been performed.

---

# 1. Executive summary

## 1.1 What exists

The repository is a **standalone Android scientific calculator** written in Kotlin with Jetpack Compose: approximately **2,233 Kotlin lines** in **15 main source files** plus one unit-test file. It contains:

- A single-activity calculator UI with a Casio-style faceplate, fixed 210 dp LCD display, scientific keypad, calculation history overlay and light/dark/system theme dialog.
- A pure-Kotlin expression engine using the shunting-yard algorithm, RPN evaluation, trigonometric/logarithmic functions, constants, implicit multiplication, percent and factorial.
- A `TriggerEvaluator` extension hook and a `SampleSecurityTriggerEvaluator` that compares the current expression to a **hardcoded 4-digit numeric literal (redacted)**. The only ShadowNet-specific behaviour is this demo comparison.
- Plaintext `SharedPreferences` persistence for calculation history (maximum 20 items) and theme.
- **No** protected screen, onboarding, responder management, emergency interface, SOS, location, SMS, evidence capture, encryption, backend, network permission, analytics, crash reporting, CI/CD, version control metadata, README or architecture documentation.

## 1.2 Bottom line

| Question | Answer |
|---|---|
| Is this already ShadowNet? | **No.** It is a calculator with a demo interception hook and a visible “security” banner. No protected functionality is implemented. |
| Is the calculator engine reusable? | **Yes.** The parser is the strongest asset: pure Kotlin, separated from Android, covered by 11 passing unit tests. It needs correctness fixes and much broader tests. |
| Is the calculator UI ready to be the public interface? | **No.** It is visually distinctive but has a clipped bottom row, misleading/non-functional keys, no accessible name on any clickable control, and an equals key with no accessibility action. |
| Is a protected activation gate present? | **No.** A hardcoded literal is compared to the display; no authentication, credential storage, attempt limit, lifecycle protection or protected destination exists. |
| Is it secure enough for a survivor-facing product? | **No.** CRITICAL findings include a hardcoded credential and a visible interception banner. No encryption, no `FLAG_SECURE`, backup enabled, no lifecycle clearing. |
| Is accessibility adequate? | **No.** On-device inspection found 45 clickable nodes, all with empty accessible names; `=` is not clickable/focusable in the accessibility tree; the bottom row is clipped to ~23 dp; several colour pairs fail WCAG AA. |
| Does a backend exist? | **No.** No Firebase, API client, database, cloud functions, SMS integration, worker, offline queue or CI/CD. |
| Does the project build as-is? | **No, not with the checked-in wrapper.** `./gradlew` uses Gradle 8.7; AGP 8.7.2 requires Gradle 8.9+. The deploy script prefers Gradle 9.7.1 and failed at resource processing. Source builds successfully in an isolated copy with Gradle 8.13; 11 tests pass. |
| Can architecture design proceed? | **Yes.** The existing stack and parser should be retained. The security hook and calculator UI semantics must be reworked before protected features are added. |

## 1.3 Critical findings (headline)

1. **C-01 — Hardcoded credential.** A 4-digit numeric secret is a literal in `MainActivity.kt:25` and a default parameter in `TriggerEvaluator.kt:32`. It is compiled into the APK, has no rate limiting, and is not an authentication mechanism. **[V]**
2. **C-02 — No protected activation gate or destination.** `SampleSecurityTriggerEvaluator.onSecurityTriggered` defaults to an empty lambda (`TriggerEvaluator.kt:33`); the activity passes no callback. Triggering only sets `isIntercepted` and shows a banner. **[V]**
3. **C-03 — Visible disclosure of the hidden feature.** The UI displays `SECURITY HOOK TRIGGERED` and `INTERCEPTED` (`CalculatorViewModel.kt:204-207`, `DisplaySection.kt:163-190,220-231`), defeating plausible deniability and potentially escalating danger if an abuser sees it. **[V]**
4. **A-01 — Equals is inaccessible.** The `=` key is implemented with `pointerInput`/`detectTapGestures` on a non-clickable `Surface` (`KeypadSection.kt:384-395`). In the on-device accessibility tree the node is `clickable=false`, `focusable=false`, `long-clickable=false`. TalkBack and Switch Access cannot activate it. **[V]**
5. **A-02 — Every clickable key is unnamed.** UI Automator reported **45 clickable nodes, 0 with a non-empty text or content description**. Labels are separate `TextView` nodes. **[V]**
6. **A-03 — Bottom row is clipped.** On the test device the equals row was only **23.1 dp high** and partly off-screen; the visible `=` glyph was **4.4 × 5.6 dp**. **[V]**
7. **C-04/C-05/C-06 — Screen capture and backup exposure.** `allowBackup="true"` with no backup rules (`AndroidManifest.xml:5`); no `FLAG_SECURE`; `adb exec-out screencap` succeeded. History and theme are plaintext `SharedPreferences`. **[V]**
8. **Build blocker.** The checked-in wrapper cannot build the project, and the documented deploy path uses an unsupported Gradle version. This must be fixed before implementation work is credible. **[V]**

## 1.4 Readiness verdict

**Production readiness: RED. Architecture-design readiness: AMBER/GREEN.**

There is enough verified information to design ShadowNet without guessing. There is not enough implemented security or accessibility to put this in front of survivors. The next work package should be **WP1 — foundation, build reproducibility, accessibility design system and security architecture**, not emergency feature implementation.

---

# 2. Current technology stack

## 2.1 Verified stack

| Area | Technology / value | Evidence |
|---|---|---|
| Language | Kotlin 2.0.21 | `build.gradle.kts:3-4` |
| UI | Jetpack Compose, Material 3, Compose BOM `2024.10.00` (Compose UI 1.7.4 per lint resolution) | `app/build.gradle.kts:52-60` |
| App framework | Single `ComponentActivity` + Compose; `AndroidViewModel`; Kotlin `StateFlow` | `MainActivity.kt`, `CalculatorViewModel.kt` |
| Build system | Gradle wrapper **8.7**; Android Gradle Plugin **8.7.2** | `gradle/wrapper/gradle-wrapper.properties:3`, `build.gradle.kts:2` |
| Build JDK | Java/Kotlin target 17; build environment JDK 21.0.11 | `app/build.gradle.kts:33-39`; verified `java -version` |
| Android SDK | compileSdk 35, targetSdk 35, minSdk 24 | `app/build.gradle.kts:9,13-14` |
| Package / version | `com.shadownet.calculator`; 1.0.0 (code 1) | `app/build.gradle.kts:8,12,15-16` |
| Platforms | Android only, API 24+ (Android 7.0+) | manifest / Gradle |
| ABIs in debug APK | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (universal) | `aapt2 dump badging` |
| Local persistence | `SharedPreferences` (history JSON, theme string) | `HistoryPersistenceManager.kt:12-26`, `ThemePreferenceManager.kt:20-35` |
| Navigation | None (no NavHost, no fragments); overlays and one dialog | `CalculatorScreen.kt` |
| DI | None; `viewModels()` and global objects | `MainActivity.kt:17` |
| Async | `MutableStateFlow`/`StateFlow` in ViewModel; no WorkManager/coroutines beyond state flow | `CalculatorViewModel.kt:25-30` |
| Networking | None; no `INTERNET` permission, no HTTP client | merged manifest / source search |
| Backend / cloud | None | repository and manifest |
| Analytics / crash reporting | None | dependency list / merged manifest |
| Testing | JUnit 4.13.2; 11 parser unit tests; AndroidX test dependencies declared but no `androidTest` sources | `app/src/test/...`; dependency block |
| Static analysis | No lint/ktlint/detekt configuration; `lintDebug` attempted but blocked in this environment | build output |
| CI/CD | None | no workflow/config files |
| VCS | **Not a git repository**; no `.git`, no `.gitignore` | `git status` |
| Documentation | None (no README, docs, license, privacy policy) | repository scan |

## 2.2 Dependency inventory

Direct dependencies (`app/build.gradle.kts:46-68`):

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.13.1 | AndroidX core |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.6 | Lifecycle/coroutines |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.6 | ViewModel in Compose |
| `androidx.activity:activity-compose` | 1.9.3 | Compose activity integration |
| Compose BOM | 2024.10.00 | Compose version alignment |
| `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview` | via BOM | UI |
| `androidx.compose.material3:material3` | via BOM | Material 3 |
| `androidx.compose.material:material-icons-extended` | via BOM | Icons |
| `junit:junit` | 4.13.2 | Unit tests |
| `androidx.test.ext:junit` | 1.2.1 | Instrumentation test runner |
| `androidx.test.espresso:espresso-core` | 3.6.1 | Instrumentation tests |
| `androidx.compose.ui:ui-test-junit4` | via BOM | Compose tests |
| `androidx.compose.ui:ui-tooling`, `ui-test-manifest` | via BOM | Debug tooling |

No Firebase, Google Play Services, Retrofit/OkHttp, Room, WorkManager, SQLCipher, Tink, biometric, encryption, analytics, crash reporting or SMS dependency is present. This is a clean, small supply-chain surface, but it also means every backend/security capability is greenfield.

## 2.3 Version and toolchain observations

- **[V] BLOCKER B-01:** AGP 8.7.2 requires Gradle **8.9 or newer**; the checked-in wrapper is **8.7**. `./gradlew testDebugUnitTest` fails with: “Minimum supported Gradle version is 8.9. Current version is 8.7.”
- **[V] BLOCKER B-02:** `deploy_to_device.sh:32-37` prefers a hardcoded Gradle **9.7.1** installation. Running it produced `:app:processDebugResources FAILED` with “Cannot load processed manifest files, please file a bug” after 18m14s. AGP 8.7.2 is not validated against Gradle 9.x; the failure may also have been affected by concurrent builds, but the combination is unsupported either way.
- **[V] Workaround verified:** Gradle **8.13** with JDK 21 builds the same source successfully in an isolated copy (`BUILD SUCCESSFUL in 9m 42s`), including `testDebugUnitTest` and `assembleDebug`.
- **[V] Source is compatible with the declared target:** compilation produced only three Kotlin deprecation warnings (see §12).
- **[R]** Pin one supported Gradle version (8.9–8.13) in the wrapper, make the deploy script call `./gradlew`, and remove the hardcoded Gradle/JDK/SDK paths. Add a CI job that runs the wrapper on a clean checkout.

---

# 3. Repository structure

## 3.1 Tree (source only)

```text
Shadownet/
├── app/
│   ├── build.gradle.kts                  # app module, Compose, min/target SDK, dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # application + exported MainActivity
│       │   ├── java/com/shadownet/calculator/
│       │   │   ├── MainActivity.kt        # entry point; wires ViewModel + demo trigger
│       │   │   ├── engine/
│       │   │   │   └── ExpressionParser.kt # 426-line shunting-yard engine
│       │   │   ├── security/
│       │   │   │   └── TriggerEvaluator.kt # interface + demo hardcoded evaluator
│       │   │   └── ui/
│       │   │       ├── CalculatorScreen.kt
│       │   │       ├── CalculatorUiState.kt
│       │   │       ├── CalculatorViewModel.kt
│       │   │       ├── HistoryPersistenceManager.kt
│       │   │       ├── components/
│       │   │       │   ├── AppearanceSettingsDialog.kt
│       │   │       │   ├── DisplaySection.kt
│       │   │       │   ├── HistoryOverlay.kt
│       │   │       │   └── KeypadSection.kt
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           ├── ThemePreferenceManager.kt
│       │   │           └── Type.kt
│       │   └── res/
│       │       ├── mipmap-*/ic_launcher*.png
│       │       └── values/{strings,themes}.xml
│       └── test/java/com/shadownet/calculator/
│           └── ExpressionParserTest.kt    # 11 JUnit tests
├── gradle/wrapper/                         # wrapper 8.7 JAR + properties
├── build.gradle.kts                        # AGP 8.7.2, Kotlin 2.0.21
├── settings.gradle.kts                     # single :app module
├── gradle.properties
├── gradlew
├── deploy_to_device.sh                     # local ADB build/install helper
├── local.properties                        # local SDK path (not a secret)
├── statics/shadownet.png                   # 1254×1254 design asset (calculator)
├── build/                                  # generated
├── app/build/                              # generated; contains APK/test artifacts
├── .gradle/, .kotlin/, .idea/              # tool caches/IDE metadata
└── (no .git, no .gitignore, no README, no docs, no CI)
```

## 3.2 Module responsibilities

| Path | Responsibility | Quality note |
|---|---|---|
| `MainActivity.kt` | Lifecycle entry; creates ViewModel; installs demo trigger; hidden power-off callback. | 37 lines; hardcoded secret; no lifecycle/security handling. |
| `ExpressionParser.kt` | Tokenize, shunting-yard, RPN evaluation, formatting, errors. | Pure Kotlin, reusable, testable; 426 lines; correctness deviations. |
| `TriggerEvaluator.kt` | Synchronous interception interface + sample implementation. | Good seam concept; demo implementation is unsafe and not authentication. |
| `CalculatorViewModel.kt` | Input buffer, preview, equals, history, shift/alpha, interception state, theme angle mode. | UDF-style; Android-coupled; no saved state; security state in calculator state. |
| `CalculatorScreen.kt` | Compose layout, overlay/dialog visibility, theme selection. | Single screen; no navigation; fixed layout. |
| `DisplaySection.kt` | LCD, status indicators, expression/result, history/theme buttons, intercept banner. | Fixed 210 dp; tiny text; disclosure banner. |
| `KeypadSection.kt` | 9-row Casio keypad, key styling, equals pointer input. | Many labels not matched by actions; no semantics; equals inaccessible. |
| `HistoryOverlay.kt` | 60%-height history panel with clear/close and row selection. | Not modal; no focus trap; plaintext storage. |
| `AppearanceSettingsDialog.kt` | Theme radio dialog. | Standard AlertDialog; likely okay but radio row height not verified at large font. |
| `HistoryPersistenceManager.kt` | JSON `SharedPreferences` history. | No encryption, no retention policy, `printStackTrace`. |
| `ThemePreferenceManager.kt`, `Theme.kt`, `Color.kt`, `Type.kt` | Theme persistence, palettes, typography. | Attractive palette; contrast failures; deprecated window APIs. |
| `ExpressionParserTest.kt` | Parser unit tests. | 11 tests; no UI/security/lifecycle tests. |
| `deploy_to_device.sh` | Local build/install. | Hardcoded paths/toolchain; not CI; deploys debug build. |

## 3.3 Version control and documentation status

- **[V]** `git status` returns “fatal: not a git repository”. There is no `.git` directory, so commit history, branches, uncommitted changes and authorship cannot be assessed.
- **[V]** There is no `.gitignore`. `local.properties`, `build/`, `app/build/`, `.gradle/`, `.kotlin/` and `.idea/` are present in the working tree and would be candidates for accidental commit.
- **[V]** No README, `docs/`, license, privacy policy, architecture notes, threat model or contribution guide exists.
- **[V]** No GitHub Actions, GitLab CI, CircleCI, Jenkinsfile or other CI configuration exists.
- **[V]** No `CHANGELOG`, versioning policy or release checklist exists.
- **[R]** Initialise version control, add a `.gitignore` that excludes local SDK paths and build caches, and require signed commits/PR review before WP1. The audit did not create a repository or commit anything.

---

# 4. Existing architecture

## 4.1 Runtime flow

```text
Android launcher
  └─ MainActivity (ComponentActivity, exported, launcher intent)
       ├─ CalculatorViewModel by viewModels()
       │    ├─ MutableStateFlow<CalculatorUiState>
       │    ├─ ExpressionParser.evaluate(...)      [pure Kotlin]
       │    ├─ HistoryPersistenceManager          [SharedPreferences + JSON]
       │    └─ TriggerEvaluator                   [synchronous interception hook]
       │         └─ SampleSecurityTriggerEvaluator [hardcoded literal, no-op callback]
       ├─ setContent { CalculatorScreen(viewModel) }
       │    ├─ DisplaySection
       │    ├─ KeypadSection
       │    ├─ HistoryOverlay (conditional)
       │    └─ AppearanceSettingsDialog (conditional)
       └─ ThemePreferenceManager [SharedPreferences]
```

## 4.2 Architectural characteristics

**Strengths**

- The expression engine is a pure Kotlin `object` with no Android dependency, making it unit-testable and reusable.
- State is centralised in one immutable `CalculatorUiState` and exposed as a `StateFlow`; UI events call explicit ViewModel methods.
- The `TriggerEvaluator` `fun interface` is a sensible seam in principle: it decouples “equals” handling from a future activation mechanism.
- Compose + Material 3 + a small dependency set is a reasonable fit for a low-end Android device; there is no heavyweight framework to unwind.
- The theme system is centralised in `Color.kt`/`Theme.kt`, which makes contrast remediation tractable.

**Weaknesses**

- **No navigation architecture.** There is one screen, two booleans for overlay/dialog state, and no back-stack, destination or deep-link model. A protected area needs an explicit host state machine or navigation layer.
- **No separation between calculator and security.** `CalculatorUiState` carries `isIntercepted`/`interceptionMessage` (`CalculatorUiState.kt:24-25`), and the calculator display renders the security banner. This couples the public calculator to the hidden feature and is the direct cause of the disclosure finding.
- **Synchronous, Android-coupled ViewModel.** `AndroidViewModel` reads `SharedPreferences` in its constructor (`CalculatorViewModel.kt:25-29`) and uses `getApplication()` inside operations. Persistence is a global singleton. This makes testing and encryption migration harder.
- **No saved state.** Rotation/process death recreates state from defaults plus persisted history; the current expression and modifier state are lost. `collectAsState()` is not lifecycle-aware (`CalculatorScreen.kt:40`), so collection continues while stopped.
- **The trigger is synchronous and boolean.** It cannot cleanly represent “authentication required”, “rate-limited”, “duress”, “cancelled” or an async biometric prompt. A future gate needs a state machine/result type, not `Boolean`.
- **Security state is rendered by the public UI.** The banner, its lock icon content description and the `INTERCEPTED` result are all visible/accessible in the calculator. Removing this coupling is a prerequisite for concealment.
- **No dependency injection or module boundaries.** A single `:app` module means protected code, emergency permissions and evidence handling would be mixed with calculator code unless deliberately separated.
- **No process/lifecycle policy.** `MainActivity` only overrides `onCreate`; there is no `onStop`/`onPause` clearing, timeout, screenshot protection, recents exclusion or task hardening.

## 4.3 Architectural verdict

The app is a **well-structured small calculator**, not a ShadowNet application. The correct strategy is **extend, not rewrite**: keep the Kotlin/Compose stack and the parser, introduce explicit module boundaries and a host-level state machine, and replace the demo trigger with a real, asynchronous, silent security gate.

---

# 5. Calculator functionality inventory

## 5.1 Screen and UI structure

| Element | Implementation | Evidence |
|---|---|---|
| Root | `Scaffold` + `Column`, edge-to-edge, 8 dp outer padding | `CalculatorScreen.kt:52-67` |
| Display | Fixed 210 dp LCD panel; status row, expression row, result row, security banner | `DisplaySection.kt:74-247` |
| Status indicators | `S` (shift), `A` (alpha), `MATH`, `DEG/RAD`, `NORM-11`/`STD` | `DisplaySection.kt:98-133` |
| Toolbar | Theme icon button (26 dp), history icon button (26 dp) | `DisplaySection.kt:136-160` |
| Keypad | Modifier row + eight 5-column rows; Casio-style secondary labels printed above keycaps | `KeypadSection.kt:89-227` |
| History | Slide-over panel, 60% screen height, LazyColumn, clear/close icons | `HistoryOverlay.kt:53-135` |
| Theme | `AlertDialog` with three radio options (System/Light/Dark) | `AppearanceSettingsDialog.kt:39-88` |
| Navigation | None. Booleans `isHistoryOpen`, `isSettingsOpen` in composable local state | `CalculatorScreen.kt:42-44` |
| Exit | Hidden `SHIFT` + `AC` calls `finishAndRemoveTask()` | `CalculatorViewModel.kt:148-161`, `MainActivity.kt:28-31` |

**Layout defect (verified on device):** the keypad is not scrollable and uses fixed heights plus row weights. On a 720×1600 px device at 256 dpi (450×955 dp), the final row was clipped to **23.1 dp** and the `=` key was partly below the navigation bar. See §6.4.

## 5.2 State management

`CalculatorUiState` (`CalculatorUiState.kt:17-28`) contains:

| Field | Purpose | Issue |
|---|---|---|
| `expression` | Current input/result string | Also holds the secret code while typing; also set to literal `"Off"` on exit. |
| `resultPreview` | Live evaluation preview | Evaluates on every keystroke; can preview the secret code. |
| `isScientificMode` | Display `NORM-11` vs `STD` | **Not wired to any control**; `DisplaySection` accepts `onToggleScientific` but never invokes it; `KeypadSection` accepts `isScientificMode` but does not use it. |
| `angleUnit` | DEG/RAD | Toggled by both `MODE` and `DEG/RAD`. |
| `error` | Last parser error | Not announced to accessibility services; contrast failure. |
| `history` | Up to 20 items | Loaded synchronously in ViewModel constructor; plaintext persisted. |
| `isIntercepted` | Demo security state | Couples security to calculator; drives visible banner. |
| `interceptionMessage` | Demo banner text | Direct disclosure string. |
| `isShiftActive`, `isAlphaActive` | Modifier state | Not exposed as toggle/checked semantics; `ALPHA` has no implemented function. |

**State defects**

- `CalculationHistoryItem.id = System.currentTimeMillis()` (`CalculatorUiState.kt:9`) can collide when two results are created in the same millisecond; `LazyColumn` keys must be unique. Low probability, but a crash/visual bug risk. **[V]**
- The ViewModel uses `_uiState.update` consistently, which is good. However, `onDigitClick` sets `expression = digit` when intercepted (`CalculatorViewModel.kt:48`), silently discarding the previous expression; this is a UX choice but not documented.
- `updatePreview()` swallows parser errors and only clears the preview, while `onEqualsClick` surfaces errors. A user can type an invalid expression and see no feedback until `=`. **[V]**
- There is no saved-state handling (`SavedStateHandle`), no process-death restoration, and no `collectAsStateWithLifecycle`. **[V]**

## 5.3 Expression parser

**Algorithm:** sanitize → tokenize → shunting-yard → RPN evaluation → floating-point cleanup → formatting (`ExpressionParser.kt:65-92`).

Supported syntax (verified by tests and direct probes):

| Category | Supported |
|---|---|
| Binary operators | `+`, `-`, `*`, `/`, `^` |
| Unary | unary `-`, postfix `%` (divide by 100), postfix `!` (factorial) |
| Functions | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `log` (base 10), `ln`, `sqrt`, `cbrt`, `abs` |
| Constants | `pi`/`π`, `e` |
| Grouping | parentheses |
| Implicit multiplication | number/constant/`)`/postfix followed by number, constant, function or `(` |
| Angle modes | DEG and RAD |
| Formatting | integer fast path; decimal string up to 10 fractional digits; scientific notation `0.######E0` using `Locale.US` |

**Verified parser behaviour**

| Expression | Result | Comment |
|---|---|---|
| `5 + 3 * 2` | `11` | Precedence correct. |
| `(5 + 3) * 2` | `16` | Parentheses correct. |
| `2 ^ 3 ^ 2` | `512` | Right associativity correct. |
| `sin(90)` DEG | `1` | Trig correct. |
| `sin(pi / 2)` RAD | `1` | Radians correct. |
| `log(100)` | `2` | Base-10 log correct. |
| `ln(e)` | `1` | Natural log correct. |
| `sqrt(16) + pi` | `7.1415926535…` | Constants/functions correct. |
| `2pi` | `6.2831853071…` | Implicit multiplication works. |
| `50%` | `0.5` | Postfix percent works. |
| `5!` | `120` | Factorial works. |
| `10 / 0` | `Error("Cannot divide by zero")` | Division guard works. |
| `log(-10)` | `Error("Domain Error: log")` | Domain guard works. |
| `sqrt(-1)` | `Error("Domain Error: sqrt")` | Domain guard works. |
| `171!` | `Error("Overflow")` | Factorial bound works. |
| `-2^2` | **`4`** | **Convention deviation:** most scientific calculators and mathematics expect `-(2^2) = -4`. Unary minus is given precedence 5, higher than `^` (4) at `ExpressionParser.kt:188,204`. |
| `200 + 10%` | **`200.1`** | This is mathematical “10 percent as 0.1”, not the common calculator semantics `200 + 20 = 220`. If percent means “percentage of the left operand”, this is wrong. |
| `10^1000` | **`Error("Cannot divide by zero")`** | Infinity is mapped to a divide-by-zero message at `ExpressionParser.kt:79-81`; misleading error text. |
| `tan(90)` DEG | `Error("Domain Error: tan undefined")` | Correct guard. |
| `tan(pi/2)` RAD | `1.633123935319537E16` | Floating-point singularity not guarded; acceptable mathematically but surprising. |
| `1.2.3` | `Error("Malformed number")` | Tokenizer rejects multiple decimals. |
| `1/3` | `"0.3333333333"` | Display rounds to 10 fractional digits; raw value retains more precision. |
| `10^(` (shift+log output) | `Error("Mismatched parentheses")` | The **UI can generate this invalid expression**; shift+log is broken. See §5.5. |
| `e^(` (shift+ln output) | `Error("Mismatched parentheses")` | Shift+ln is broken. |

**Parser robustness**

- No expression length, nesting-depth or token-count limit. Input is local and user-driven, so this is low risk today, but a future remote/demo channel should not accept arbitrary expressions. **[A]**
- `evaluate` catches `ArithmeticException`, `IllegalArgumentException` and `Exception`; the generic catch returns `"Syntax Error"`. This is user-facing English only and has no error codes for accessibility/localization. **[V]**
- `formatNumber` uses `BigDecimal(value.toString())` and `setScale(...coerceAtMost(10))`, which can silently reduce precision for values with more than 10 fractional digits. This is a display policy, not a correctness bug, but it should be documented/tested. **[V]**
- Decimal separator is always `.` and scientific formatting uses `Locale.US`; there is no locale-aware number formatting. **[V]**

## 5.4 Calculation history

- Stored as a JSON array in `SharedPreferences` named `calculator_persistent_history_prefs` under `key_history_json` (`HistoryPersistenceManager.kt:12-26`).
- Loaded synchronously in the ViewModel constructor (`CalculatorViewModel.kt:25-29`).
- Newest item prepended; list truncated to 20 (`CalculatorViewModel.kt:217-225`).
- Selecting an item restores its expression (`CalculatorViewModel.kt:258-268`); clearing deletes the key value (`CalculatorViewModel.kt:270-273`).
- No timestamps displayed, no retention policy, no encryption, no export, no secure deletion. `printStackTrace()` on malformed JSON (`HistoryPersistenceManager.kt:45-47`).
- History is included in Android Auto Backup because `allowBackup="true"` and no rules exclude it. **[V]**

## 5.5 Button/action inventory and mismatches

This is the most important functional finding after accessibility: **many printed labels do not perform the labelled function.** The visual design implies a full Casio-class calculator, but the action map is partial and in places actively wrong.

| Visual label (primary / secondary) | Actual action | Evidence | Assessment |
|---|---|---|---|
| `SHIFT` | Toggle shift state | `KeypadSection.kt:104`, `CalculatorViewModel.kt:38-40` | Works, but state not accessible. |
| `ALPHA` | Toggle alpha state | `KeypadSection.kt:105`, `CalculatorViewModel.kt:42-44` | No alpha functions exist; misleading. |
| `MODE` | Toggle DEG/RAD | `KeypadSection.kt:106` | Wrong label; no mode menu. |
| `DEG/RAD` | Toggle DEG/RAD | `KeypadSection.kt:107` | Works, duplicate of MODE. |
| `CALC` / `SOLVE` | Insert `abs(` | `KeypadSection.kt:117` | Wrong. |
| `d/dx` / `∫dx` | Insert `abs(` | `KeypadSection.kt:118` | Wrong; no calculus. |
| `x⁻¹` / `x!` | Insert `^` | `KeypadSection.kt:119` | Wrong; no reciprocal, no factorial button (parser supports `!` but no UI). |
| `logᵧ` / `10ˣ` | Insert `log(` | `KeypadSection.kt:120` | Wrong base; no 10^x. |
| `ln` / `eˣ` | Insert `ln(` | `KeypadSection.kt:121` | Primary works; shift `e^` output is invalid. |
| `⚲` / `a/b` | Insert `/` | `KeypadSection.kt:130` | Obscure glyph; no fraction UI. |
| `√` / `∛` | Insert `sqrt(` or `cbrt(` | `KeypadSection.kt:131-133` | Works. |
| `x²` / `x³` | Insert `^` | `KeypadSection.kt:134` | Wrong; user must type exponent. |
| `xʸ` / `ʸ√x` | Insert `^` | `KeypadSection.kt:135` | Power works only via manual exponent; root not implemented. |
| `log` / `10ˣ` | Insert `log(` | `KeypadSection.kt:136` | Primary works; shift invalid/absent. |
| `(-)` / `STO` | Insert `-` | `KeypadSection.kt:145` | Negative works; store not implemented. |
| `hyp` / `abs` | Insert `abs(` | `KeypadSection.kt:146` | Wrong; no hyperbolic functions. |
| `sin`/`cos`/`tan` (+ shift inverse labels) | Insert `sin(`/`cos(`/`tan(`; shift maps to `asin(`/`acos(`/`atan(` | `KeypadSection.kt:147-149`, `CalculatorViewModel.kt:87-105` | Primary and inverse work. Labels change with shift, which is functionally okay but adds accessibility/state complexity. |
| `RCL` / `CLR` | Insert `e` | `KeypadSection.kt:158` | Wrong. |
| `ENG` / `i` | Insert `π` | `KeypadSection.kt:159` | Wrong; no engineering notation or imaginary unit. |
| `(` / `%` | Insert `(` | `KeypadSection.kt:160` | Parenthesis works; **percent is unreachable from the UI** despite parser support. |
| `)` | Insert `)` | `KeypadSection.kt:161` | Works. |
| `S⇕D` / `M+` | Toggle DEG/RAD | `KeypadSection.kt:162` | Wrong; no S⇕D conversion or memory add. |
| `7 8 9` | Digits | `KeypadSection.kt:174-176` | Work; secondary `CONST`/`CONV`/`CLR` are decorative. |
| `DEL` / `INS` | Delete last token/char | `KeypadSection.kt:177`, `CalculatorViewModel.kt:177-196` | Deletion works, including whole function prefixes; insert mode not implemented. |
| `AC` / `OFF` | Clear, or exit if SHIFT active | `KeypadSection.kt:178`, `CalculatorViewModel.kt:148-161` | Works; hidden exit is undiscoverable and inaccessible. |
| `4 5 6` | Digits | `KeypadSection.kt:187-189` | Work; secondary `MATRIX`/`VECTOR`/`STAT` decorative. |
| `×` / `nPr` | Multiply | `KeypadSection.kt:190` | Works; nPr not implemented. |
| `÷` / `nCr` | Divide | `KeypadSection.kt:191` | Works; nCr not implemented. |
| `1 2 3` | Digits | `KeypadSection.kt:200-202` | Work; secondary `STAT`/`CMPLX`/`DISTR` decorative. |
| `+` / `Pol` | Add | `KeypadSection.kt:203` | Works; polar conversion not implemented. |
| `−` / `Rec` | Subtract | `KeypadSection.kt:204` | Works; rectangular conversion not implemented. |
| `0` | Digit | `KeypadSection.kt:213` | Works. |
| `.` / `Ran#` | Decimal point | `KeypadSection.kt:214` | Works; random number not implemented. |
| `×10ˣ` / `π` | Insert `π` | `KeypadSection.kt:215` | **Wrong:** label suggests exponent entry; action inserts pi. |
| `Ans` / `PreAns` | Insert `e` | `KeypadSection.kt:216` | **Wrong:** no last-answer or previous-answer support; inserts Euler’s number. |
| `=` / `EXE` | Evaluate; long press ignored | `KeypadSection.kt:384-395`, `CalculatorViewModel.kt:198-244` | Visually and functionally core; **not accessible**; `isLongPress` parameter is unused. |

Additional functional notes:

- `SHIFT` + `log` produces `10^(` and `SHIFT` + `ln` produces `e^(` (`CalculatorViewModel.kt:87-105`). Both are invalid at the parser (`Mismatched parentheses`) because the opening parenthesis has no left operand. **Verified.**
- `ALPHA` toggles but no key uses it. It is pure UI state.
- `isScientificMode` is never toggled; the keypad always shows the full scientific layout.
- The `MODE` button does not open a mode menu; both it and `DEG/RAD` just flip the angle unit.
- The parser supports `%` and `!`, but the only `%` and `!` labels are secondary labels on keys whose actions are `(` and `^`. Neither operation is reachable.
- The secondary labels are printed on the faceplate but are not associated with the key actions or the accessibility node. They imply capabilities that do not exist. This is both a product-integrity problem and an accessibility problem (TalkBack may read them as separate text).

## 5.6 Event handling, gestures and error handling

| Aspect | Current behaviour | Evidence |
|---|---|---|
| Tap handling | `Surface(onClick = ...)` for most keys; custom `pointerInput` + `detectTapGestures` for `=`. | `KeypadSection.kt:243-252,322-347,384-395` |
| Long press | Detected only on `=`; `isLongPress` is accepted by the ViewModel but never read. | `KeypadSection.kt:390-394`, `CalculatorViewModel.kt:198` |
| Double tap / swipe / multi-finger | None. | source search |
| Keyboard | No hardware key mapping; `=` is not focusable. Other `Surface` clickables are focusable by Compose. | on-device tree |
| Haptics | None; no `LocalHapticFeedback`, `performHapticFeedback` or `VIBRATE` permission. | source search |
| Accidental activation | Exact string match on `=`; no debounce, confirmation, attempt counter or cooldown. A legitimate calculation equal to the code triggers it. | `TriggerEvaluator.kt:36-41` |
| Error display | Error text replaces the result; colour `#EF4444` on the LCD. No announcement; no retry guidance. | `DisplaySection.kt:220-242` |
| Error coverage | Arithmetic/domain/parse errors are caught; a custom `TriggerEvaluator` throwing would crash `onEqualsClick` because the call is not guarded. | `CalculatorViewModel.kt:202` |
| Input validation | Allows multiple operators (with replacement), leading operator restrictions, multiple decimals rejected only at evaluation. No length limit. | `CalculatorViewModel.kt:59-82`; parser |
| Preview | Every keystroke evaluates; invalid input silently clears preview. | `CalculatorViewModel.kt:275-289` |

## 5.7 Theme and appearance

- Three modes: `SYSTEM`, `LIGHT`, `DARK` (`ThemePreferenceManager.kt:9-13`), persisted as a string.
- Palettes are hardcoded in `Color.kt`; Material 3 colour schemes are assembled in `Theme.kt:14-34`.
- Window status/navigation bar colours are set directly (`Theme.kt:49-57`), using APIs deprecated in API 35. Compose edge-to-edge is enabled (`MainActivity.kt:21`).
- No dynamic colour, high-contrast theme, colour-blind-safe palette or user font-size setting.
- The theme dialog is a standard `AlertDialog`; its radio rows use `Role.RadioButton` and `selectable`, which is good, but row height at large font is not verified.
- **Contrast is insufficient in several key combinations** (see §6.5), including DEL/AC/ALPHA keys, error text and the interception text.

## 5.8 Calculator readiness verdict

| Question | Verdict |
|---|---|
| Does basic/scientific arithmetic work? | **Yes** for the tested subset. 11/11 parser tests pass, and direct probes confirmed standard precedence, trig, logs, roots, constants, implicit multiplication, percent and factorial. |
| Is the engine production-grade? | **Not yet.** Unary-minus precedence and percent semantics are non-standard; overflow is mislabelled; no locale-aware formatting; test coverage is narrow. |
| Is the UI a faithful scientific calculator? | **No.** Many keys are decorative or miswired; scientific mode is dead; shift log/ln are broken; percent/factorial are unreachable. |
| Is it usable by a blind user? | **No.** See §6. |
| Can it serve as ShadowNet’s public interface today? | **No, not without hardening.** The core engine is reusable; the screen layout, semantics, key map and security coupling must be fixed first. |

---

# 6. Accessibility findings

## 6.1 Method and test environment

Accessibility was assessed by:

1. Static review of every Compose component for semantics, modifiers, labels, touch targets, typography and colour tokens.
2. Contrast-ratio calculation from the exact ARGB tokens in `Color.kt` and component-local colours, using the WCAG 2.x relative-luminance formula.
3. On-device UI Automator hierarchy dump of the installed 1.0.0 build.
4. On-device screenshot capture (which also verified the absence of `FLAG_SECURE`).
5. Source review of focus order, gesture handling, lifecycle and announcements.

**Test device (verified):** TECNO BD4j, Android 11 (API 30), 720×1600 px, density override 256 dpi (≈450×955 dp), font scale **0.85**, layout direction forced RTL via developer option, TalkBack installed but **accessibility disabled** (`accessibility_enabled=0`, no enabled services). This is a low-end `armeabi-v7a` device, relevant to ShadowNet’s target audience.

**Important limitation:** because TalkBack and Switch Access were not enabled, this audit **does not verify spoken output, speech order, verbosity or gesture behaviour**. UI Automator exposes the same accessibility node tree that those services consume, so the structural findings below are strong evidence, but exact TalkBack utterances and Switch Access scanning remain **physical-device tests to perform**. No screen reader was enabled and no user data was entered deliberately.

**RTL note:** the device had “Force RTL layout” enabled (`debug.force_rtl=1`). This explains why the keypad appeared mirrored and why the expression aligned to the left. The underlying app declares `android:supportsRtl="true"` (`AndroidManifest.xml:9`) but contains no RTL-specific layout strategy. On a normal LTR device the keypad is composed left-to-right as coded; under RTL it mirrors. Whether a calculator should mirror its numeric keypad is a design decision that must be made explicitly (see §15).

## 6.2 On-device accessibility tree — verified facts

| Observation | Verified value |
|---|---|
| Total accessibility nodes | 143 |
| Clickable nodes | **45** |
| Clickable nodes with non-empty `text` or `content-desc` | **0** |
| Focusable nodes | 45 |
| Checkable/toggle nodes | **0** |
| Long-clickable nodes | **0** |
| `=` node | Parent `Surface` bounds `[23,1491][150,1528]` = **79.4 × 23.1 dp**, `clickable=false`, `focusable=false`, `long-clickable=false`; child `=` text is `4.4 × 5.6 dp`. |
| Bottom numeric row (`0 . ×10ˣ Ans =`) | All four digit/constant targets **23.1 dp high**; clipped by the parent at the navigation bar. |
| Normal keycap targets | 78.8–79.4 × 53.8 dp (meets 48 dp). |
| Modifier keys | 100.6 × 61.9 dp (meets 48 dp). |
| Theme icon | Clickable region 32.5 × 47.5 dp (width below 48 dp). |
| History icon | 47.5 × 47.5 dp (borderline). |
| Secondary labels | Separate `TextView` nodes at 7.5 sp, e.g. `SOLVE`, `∫dx`, `x!`, `10ˣ`, `eˣ`; not associated with the key. |
| Status text | `MATH`, `DEG`, `NORM-11`, `•` exposed as separate text nodes. |
| Security banner | `Security Intercepted` content description plus `SECURITY HOOK TRIGGERED` text. |

These facts are **verified on device**. They mean:

- A screen reader user encounters dozens of focusable “button” nodes whose accessible name is empty.
- The primary action for completing a calculation is not in the accessibility tree as a clickable control at all.
- The bottom row is not reachable/usable by touch in the visible area.
- Modifier state is not exposed as checked/selected/toggle state.
- Decorative labels and security text pollute the reading order.

## 6.3 Findings

| ID | Severity | Finding | Evidence | Impact | Remediation |
|---|---|---|---|---|---|
| **A-01** | **CRITICAL** | `=` has no accessibility click action. | `KeypadSection.kt:384-395` uses `pointerInput`/`detectTapGestures` on a `Surface` with no `onClick`; on-device node `clickable=false`, `focusable=false`. | TalkBack, Switch Access, keyboard and voice access cannot execute a calculation. The calculator is not independently operable. | Use `Modifier.clickable`/`combinedClickable` or `Surface(onClick=...)`; add `semantics { role = Role.Button; contentDescription = "Equals"; onClick("Equals") { ...; true } }`. Keep long-press as a custom accessibility action, not the only route. |
| **A-02** | **CRITICAL/HIGH** | All 45 clickable controls have empty accessible names. | UI Automator dump: 45 clickable, 0 named. `CasioKeyCell` and `CasioModifierKey` put text in child `Text` nodes without merging or semantics. | Screen reader users hear “button” repeatedly; cannot know which key they are on. Labels may be read separately, producing duplicate and out-of-order stops. | Add explicit `contentDescription`/`text` semantics per key, use `mergeDescendants = true` or `clearAndSetSemantics`, and hide decorative secondary labels with `clearAndSetSemantics {}`/`invisibleToUser`. Test with TalkBack. |
| **A-03** | **HIGH** | Bottom row clipped; `=` target 23.1 dp high. | On-device bounds; fixed 210 dp display (`DisplaySection.kt:77`), weighted rows (`KeypadSection.kt`), no scroll/compact layout. | Touch users miss the key; accessibility focus may land on an off-screen/obscured target. On smaller screens (360×640 dp) it will be worse. | Make the calculator vertically scrollable or responsive; reserve system insets; guarantee ≥48 dp for every key at all font scales; test 320×480, 360×640, 360×800, tablets, landscape. |
| **A-04** | **HIGH** | Text is extremely small and does not scale safely. | 7.5 sp secondary labels (`KeypadSection.kt:299,308,374`), 11 sp status (`DisplaySection.kt:101-133`), 12.5 sp keycaps (`KeypadSection.kt:339`), fixed heights, `maxLines=1`, `Ellipsis`. Device was at 0.85 scale and already clipped. | Low-vision users cannot read labels; at 200% font the layout will clip/ellipsize further. | Use ≥12 sp for functional labels, allow wrapping/scroll, avoid fixed heights, test font scale 1.0/1.3/1.5/2.0, and provide content descriptions independent of visual size. |
| **A-05** | **HIGH** | Multiple colour pairs fail WCAG AA. | Calculated from `Color.kt`: DEL 2.42:1, AC 3.60:1, ALPHA 3.37:1, light error 2.82:1, light intercepted 4.26:1, dark intercepted 3.02:1. See §6.5. | Text is unreadable in bright light/low vision; security/error messages are especially affected. | Adjust tokens to ≥4.5:1 for normal text and ≥3:1 for large text; add high-contrast theme; test with a contrast checker and on device. |
| **A-06** | **HIGH** | No deliberate focus order/grouping. | 45 focusable nodes; each row is composed in one order but RTL mirrors it; labels are separate nodes; no `traversalIndex`, `focusGroup`, `paneTitle` or heading semantics. | Screen reader/switch users traverse keys in an unexpected order, often right-to-left, with duplicate label stops. | Define logical order (display → status → keypad rows left-to-right in LTR; decide RTL policy), merge each key into one node, hide decorative labels, use `traversalIndex` only where composition order cannot express intent. |
| **A-07** | **HIGH** | `SHIFT`/`ALPHA` state not exposed. | `isShiftActive`/`isAlphaActive` are only tiny `S`/`A` text (`DisplaySection.kt:98-113`); on-device `checkable=0`, no `stateDescription`/`selected`. | A blind user cannot know whether the next key will insert an inverse function or a normal function, leading to wrong results. | Use `Modifier.toggleable`/`selectable` or `semantics { stateDescription = "Shift on/off"; toggleableState = ... }`; announce state changes politely; consider making SHIFT a one-shot with clear feedback. |
| **A-08** | **HIGH** | Results and errors are not announced. | Result/error are plain `Text` (`DisplaySection.kt:233-242`); no `liveRegion`, no `semantics` announcement; `updatePreview` changes silently. | A screen reader user presses `=` and gets no confirmation; must navigate back to the display to read the result. | Add a polite `liveRegion = LiveRegionMode.Polite` to the result node and assertive for errors; **do not** announce sensitive states on protected screens. Provide a focusable “result” node. |
| **A-09** | **HIGH (privacy + a11y)** | Security state is visible and announced. | `SECURITY HOOK TRIGGERED` / `INTERCEPTED` (`CalculatorViewModel.kt:204-207`, `DisplaySection.kt:163-190,220-231`); lock icon has `contentDescription = "Security Intercepted"` (`DisplaySection.kt:179`). | An abuser (or anyone near the device) sees/hears that a hidden feature was activated. This is the opposite of duress-safe concealment. | Remove all visible/announced security state from the calculator. Activation must be silent and indistinguishable from a normal calculation. |
| **A-10** | **MEDIUM** | Toolbar icon touch targets below 48 dp. | Theme button `Modifier.size(26.dp)` inside `IconButton` (`DisplaySection.kt:137-147`); measured 32.5 dp wide. | Hard to hit for users with tremor/limited dexterity. | Use 48×48 dp minimum, larger visual icon if desired, and content descriptions that describe the action (“Appearance settings”, “Calculation history”). |
| **A-11** | **MEDIUM** | No haptic feedback. | No `LocalHapticFeedback`/`performHapticFeedback`/`VIBRATE` anywhere. | Deaf/blind users lose a confirmatory channel; keypresses feel unresponsive. | Add optional haptics for keypress, equals, error and SOS confirmation; respect system settings; never use vibration as the only signal. |
| **A-12** | **MEDIUM** | Keyboard/hardware navigation not supported end-to-end. | No key event handling; `=` is not focusable; only Compose clickables are focusable. | Keyboard/switch users cannot complete a calculation; tab order is unclear. | Add focusable keys, `onKeyEvent` for digits/operators/enter/backspace, visible focus indicators, and test with a hardware keyboard and Switch Access. |
| **A-13** | **MEDIUM** | Switch Access compatibility unverified and likely broken. | Depends on A-01/A-02/A-06; 45 unnamed clickables, no equals action. | Switch users cannot scan/activate meaningfully. | Fix semantics first; test with Switch Access scanning, group selection and auto-scan. |
| **A-14** | **MEDIUM** | History overlay is not modal. | `HistoryOverlay` is an `AnimatedVisibility` box inside the root `Box` (`CalculatorScreen.kt:105-113`), not a `Dialog`/modal; no `paneTitle`, focus trap, or back handling. | Screen reader focus can move to obscured calculator keys; back may exit the app rather than close history. | Use `Dialog`/`ModalBottomSheet` semantics or `Modifier.semantics { paneTitle = "Calculation history"; isTraversalGroup = true }` plus focus management and `BackHandler`. |
| **A-15** | **MEDIUM** | Obscure/misleading labels. | `⚲`, `x⁻¹`, `x²`, `S⇕D`, `Ans`, `×10ˣ`, `d/dx`, `∫dx`; actions often differ (see §5.5). | Screen reader may read a symbol with no meaning; sighted users press a key that does something else. | Give every key an explicit, action-accurate label (e.g. “divide”, “power”, “square root”) and make the visible label match behaviour; localize. |
| **A-16** | **MEDIUM** | Dynamic font scaling not validated. | Fixed 210 dp display, weighted rows, single-line/ellipsized text; on-device 0.85 scale already clipped the last row. | At larger accessibility font sizes the keypad becomes unusable. | Make layout reflow/scroll; test 200% font and display size; avoid `sp` labels inside fixed-height containers. |
| **A-17** | **LOW** | Decorative status text exposed. | `MATH`, `DEG`, `NORM-11`, `•`, `S`, `A` are separate text nodes (`DisplaySection.kt:98-133`). | Adds noise to screen-reader traversal. | Merge meaningful status into one description; hide purely decorative separators/indicators. |
| **A-18** | **INFORMATIONAL** | TalkBack/Switch Access not actually exercised. | `accessibility_enabled=0`; no enabled services; no instrumentation accessibility tests. | Spoken behaviour, focus order and announcements remain unverified. | Perform scripted TalkBack and Switch Access tests on a physical device before release; include blind/low-vision testers. |
| **A-19** | **INFORMATIONAL** | No accessibility test gate. | No UI tests, no `AccessibilityChecks`, no lint report, no a11y checklist. | Regressions will go unnoticed. | Add Compose `AccessibilityChecks`/Espresso checks, TalkBack smoke tests, contrast checks and a release checklist. |

## 6.4 Touch-target and layout evidence

From the on-device hierarchy (dp values at 256 dpi):

| Region | Width × height (dp) | Assessment |
|---|---|---|
| Display toolbar theme button | 32.5 × 47.5 | Width below 48 dp. |
| Display toolbar history button | 47.5 × 47.5 | Borderline; visual icon only 16 dp. |
| Modifier keys (`SHIFT`, `ALPHA`, `MODE`, `DEG/RAD`) | 100.6 × 61.9 | Meets 48 dp. |
| Scientific/numeric keycaps | 78.8–79.4 × 53.8 | Meets 48 dp. |
| Bottom row (`0`, `.`, `×10ˣ`, `Ans`) | 79.4 × **23.1** | **Fails**; clipped. |
| `=` | 79.4 × **23.1** | **Fails**; clipped and not accessibility-clickable. |

The screen is 450×955 dp, which is taller than many budget devices; if it clips here, a 360×640 dp device will be substantially worse. The root cause is a fixed 210 dp display plus a non-scrollable weighted keypad that does not account for system insets and font metrics. This is not a cosmetic issue: the primary action is partially off-screen.

## 6.5 Colour contrast results

WCAG 2.2 AA requires **4.5:1** for normal text and **3:1** for large text (≥18 pt, or ≥14 pt bold). The functional key labels are 12.5 sp bold; at standard scaling this is **not** “large text”. Secondary labels are 7.5 sp. Ratios below are calculated from the exact hex tokens.

| Element | Foreground / background | Ratio | Result |
|---|---|---|---|
| Light faceplate secondary label | `#0F172A` / `#CBD5E1` | 12.02:1 | Pass |
| Light faceplate alpha label | `#9D174D` / `#CBD5E1` | 5.31:1 | Pass |
| Light scientific key | `#F8FAFC` / `#475569` | 7.24:1 | Pass |
| Light numeric key | `#0F172A` / `#F1F5F9` | 16.30:1 | Pass |
| Light operator key | `#FFFFFF` / `#64748B` | 4.76:1 | Pass (narrowly) |
| **DEL key** | `#F8FAFC` / `#10B981` | **2.42:1** | **FAIL** |
| **AC key** | `#F8FAFC` / `#059669` | **3.60:1** | **FAIL** |
| SHIFT key | `#000000` / `#EAB308` | 10.95:1 | Pass |
| **ALPHA key** | `#F8FAFC` / `#EC4899` | **3.37:1** | **FAIL** |
| LCD light normal | `#13221B` / `#D4E2DC` | 12.35:1 | Pass |
| **LCD light error** | `#EF4444` / `#D4E2DC` | **2.82:1** | **FAIL** |
| **LCD light intercepted** | `#7C3AED` / `#D4E2DC` | **4.26:1** | **FAIL** |
| LCD dark normal | `#34D399` / `#151D19` | 8.94:1 | Pass |
| LCD dark error | `#EF4444` / `#151D19` | 4.57:1 | Pass (narrow) |
| **LCD dark intercepted** | `#7C3AED` / `#151D19` | **3.02:1** | **FAIL** |
| Dark secondary label | `#FACC15` / `#161A18` | 11.47:1 | Pass |
| Dark key text (sci/num/op) | `#F8FAFC` / `#222825`, `#2C3431`, `#333E3B` | 10.6–14.35:1 | Pass |
| History secondary text | `#94A3B8` / `#191E1C` | 6.58:1 | Pass |
| History result | `#34D399` / `#191E1C` | 8.78:1 | Pass |
| Intercept banner text | `#FFFFFF` / `#7C3AED` | 5.70:1 | Pass |

The failures cluster on the keys most likely to be used in an emergency (delete/clear), the ALPHA modifier and the error/interception messages. The security message is both a disclosure risk and hard to read.

## 6.6 Can a blind user operate the calculator independently?

**No, not reliably.** The decisive blocker is **A-01**: the equals key has no accessibility click action. Even if a screen reader could find and read the keys, it cannot activate `=`. Combined with A-02 (no accessible names), A-03 (clipped bottom row), A-07 (no modifier state) and A-08 (no result announcement), the calculator fails the core task for a blind user.

A user could potentially type digits and operators through some unnamed buttons if they memorised the traversal order, but they could not reliably confirm what they pressed, know the shift state, or execute the calculation. That is not independent operation.

## 6.7 Accessibility remediation architecture

**Non-negotiable release gates [R]:**

1. Every interactive key is a single accessibility node with a stable, action-accurate `contentDescription`, `Role.Button`, and an `onClick` action. `=` especially must expose both click and long-click custom actions.
2. Every key target is ≥48×48 dp at all supported font scales and screen sizes; layout scrolls/reflows rather than clipping.
3. TalkBack can complete `2 + 3 = 5` without sighted assistance; the result is announced once, politely.
4. Switch Access can scan and activate every key; keyboard can Tab/arrow/Enter through the keypad.
5. SHIFT/ALPHA are exposed as toggle state with spoken feedback.
6. Contrast meets WCAG 2.2 AA for all functional text, error text and any safety-critical UI.
7. Font scale 2.0 and display size maximum do not clip keys or hide `=`.
8. No security/duress state is visible or announced in the calculator.
9. Haptics are available and optional.
10. Accessibility is tested on a physical device with a blind or low-vision tester, not just static analysis.

**Design-system approach [R]:** create accessible primitives (`AccessibleKey`, `AccessibleIconButton`, `AccessibleDisplay`) in a `:core:designsystem` module. Centralise minimum target size, label/state semantics, focus grouping, haptics and contrast tokens. Do not rely on individual composables to remember semantics. Use `Modifier.semantics(mergeDescendants = true) { ... }`, `clearAndSetSemantics` for decorative labels, `stateDescription`, `liveRegion`, `traversalIndex`, `focusRequester` and `focusGroup` deliberately. Keep the calculator’s expression display LTR (numbers are LTR) even if the surrounding UI is RTL, unless user testing shows otherwise.

---

# 7. Security and privacy findings

## 7.1 Threat model

The required threat model is **physical access by an abusive individual**, not a remote nation-state attacker. Relevant scenarios:

- The abuser knows the survivor owns a phone and may recognise a “calculator” app, but should not be able to tell that it contains ShadowNet.
- The abuser picks up an unlocked device, opens recent apps, takes a screenshot, or scrolls through the app.
- The abuser knows the survivor, watches the screen, or shares the device.
- The abuser has brief access to backups, files or app data (without root), or installs the APK and inspects it.
- The survivor may be forced to unlock the device or reveal a code; duress behaviour matters.
- The device may be old, unpatched, rooted, or have malicious accessibility/overlay services. **No app can guarantee concealment or safety on a compromised device.**

This audit does **not** claim the app is or can be made undetectable. It identifies concrete gaps relative to the proposed design.

## 7.2 Manifest and platform configuration

Source manifest (`app/src/main/AndroidManifest.xml`):

| Attribute / component | Value | Assessment |
|---|---|---|
| `android:allowBackup` | `true` | **HIGH:** app data is eligible for Auto Backup/device-to-device transfer; no rules exclude history or future protected data. |
| `android:fullBackupContent` | absent | No include/exclude policy. |
| `android:dataExtractionRules` | absent | No Android 12+ cloud/D2D rules. |
| `android:supportsRtl` | `true` | No RTL-specific design; keypad mirrors under RTL. |
| `android:icon` / label | calculator icon; “Scientific Calculator” | Good generic public disguise. |
| `MainActivity` | `exported="true"`, launcher intent | Required for a launcher; only exported component in the release source manifest. |
| Permissions | **None** in source | Good privacy baseline; also means no network/SMS/location capability yet. |
| Services / receivers / providers | None in source | Good minimal surface. |
| `android:networkSecurityConfig` | absent | Target 35 defaults cleartext to disabled; no custom pinning. |
| `android:usesCleartextTraffic` | absent | Safe default for target 35. |
| `android:excludeFromRecents` | absent | App appears in recents with a thumbnail. |
| `android:noHistory` | absent | Activity state remains in the task. |
| `FLAG_SECURE` | not set | Screenshots and recents previews allowed. |
| `setRecentsScreenshotEnabled(false)` | not called | API 33+ recents snapshot not suppressed. |

**Debug merged manifest (verified from `app/build/intermediates/merged_manifests/debug/...`):** the debug build additionally exposes `androidx.compose.ui.tooling.PreviewActivity` and `androidx.activity.ComponentActivity` as exported activities, and includes the AndroidX `InitializationProvider` plus `ProfileInstallReceiver` (protected by `android.permission.DUMP`). These are debug/tooling artifacts, not intended for release, but the deploy script installs the **debug** APK. The source manifest itself is minimal.

**APK metadata (verified with `aapt2`):** `application-debuggable` is present on the debug APK; no runtime permissions beyond a signature-level dynamic-receiver permission; package `com.shadownet.calculator`; min 24 / target 35; label “Scientific Calculator”.

## 7.3 Findings

| ID | Severity | Finding | Evidence | Impact / assessment |
|---|---|---|---|---|
| **S-01** | **CRITICAL** | Hardcoded 4-digit credential. | Literal in `MainActivity.kt:25`; default parameter in `TriggerEvaluator.kt:32`; comparison in `TriggerEvaluator.kt:37`. Value **redacted** in this report. | Anyone with the APK, source or device can recover it. A 4-digit space is trivially brute-forced. It is not a credential store or authentication gate. A normal calculation equal to the code triggers it. |
| **S-02** | **CRITICAL** | No protected authentication or destination. | `SampleSecurityTriggerEvaluator` default callback is `{}` (`TriggerEvaluator.kt:33`); `MainActivity` passes no callback; trigger only sets `isIntercepted`/message (`CalculatorViewModel.kt:202-211`). | There is no vault, no protected screen, no session, no authorization. The product currently offers security theatre, which is dangerous if users rely on it. |
| **S-03** | **CRITICAL/HIGH** | Visible disclosure of the hidden feature. | `SECURITY HOOK TRIGGERED` and `INTERCEPTED` strings (`CalculatorViewModel.kt:206-207`); banner and result rendering (`DisplaySection.kt:163-190,220-231`); lock icon description `Security Intercepted` (`DisplaySection.kt:179`). | An abuser looking at the screen or hearing TalkBack learns there is a hidden feature. This can escalate danger and destroys plausible deniability. |
| **S-04** | **HIGH** | No screenshot/recents protection. | No `FLAG_SECURE`; `adb exec-out screencap -p` succeeded on the running app; no `excludeFromRecents`/`setRecentsScreenshotEnabled`. | Passcodes, calculations, banners and future protected content can be captured or left in the recents thumbnail. |
| **S-05** | **HIGH** | Backups include app data. | `allowBackup="true"` (`AndroidManifest.xml:5`), no `fullBackupContent`/`dataExtractionRules`. | History and theme preferences are backed up to cloud/D2D. Future sensitive data stored in prefs would leak unless explicitly excluded. |
| **S-06** | **HIGH** | Sensitive data stored in plaintext. | `HistoryPersistenceManager.kt:16,26` writes JSON to `SharedPreferences`; `ThemePreferenceManager.kt:24,34`; no encryption dependency/Keystore. | Readable by backup extraction, forensic tools, rooted devices, or a future accidental export. No cryptographic protection at rest. |
| **S-07** | **HIGH** | No lifecycle/background protection. | `MainActivity` only overrides `onCreate`; no `onStop` clear, timeout, or task hardening; Compose state remains in the task. | An abuser reopening the app sees the last expression/result. Hidden state can be exposed during app switching. |
| **S-08** | **HIGH** | No attempt limits or lockout. | `onEqualsClick` calls the trigger on every equals (`CalculatorViewModel.kt:202`); no counter, delay, lockout, audit or wipe. | Brute force is trivial, especially with a 4-digit code. No protection against repeated guessing. |
| **S-09** | **HIGH** | Package/class names disclose the product. | `applicationId = "com.shadownet.calculator"` (`app/build.gradle.kts:8,12`); class `security.SampleSecurityTriggerEvaluator`; `Shadownet` project name; package appears in app info, crash reports and APK metadata. | The launcher label/icon are generic, but the package is a strong identifier. An abuser who inspects app info or the APK can identify ShadowNet. Renaming the applicationId/package before first release is far cheaper than after. |
| **S-10** | **MEDIUM** | History retention has no privacy controls. | Newest 20 kept indefinitely (`CalculatorViewModel.kt:217-225`); only “clear history” (`CalculatorViewModel.kt:270-273`); no retention setting, auto-expiry or secure delete. | Sensitive calculations/codes may persist. SharedPreferences deletion is not a guaranteed secure wipe. |
| **S-11** | **MEDIUM** | Secret is previewed while typing. | `updatePreview()` evaluates on every keystroke (`CalculatorViewModel.kt:275-289`); `resultPreview` renders under the expression (`DisplaySection.kt:219-242`). | A numeric passcode produces a visible preview/result before `=`, making shoulder surfing easier. Suppress previews for code entry or use a non-numeric activation gesture. |
| **S-12** | **MEDIUM** | No secure authentication primitives. | No `BiometricPrompt`, `KeyguardManager`, Keystore, password hashing, HMAC, constant-time compare, or attempt persistence. | Cannot support device credential, app passphrase, biometrics or duress credentials. |
| **S-13** | **MEDIUM** | Debug build exposed/debuggable. | `application-debuggable`; debug-only exported tooling activities; `deploy_to_device.sh` installs `app-debug.apk`. | Debug builds are not suitable for distribution; they permit debugging/backup/inspection. Release config has no signing config and `isMinifyEnabled=false`. |
| **S-14** | **MEDIUM** | No release hardening. | `app/build.gradle.kts:24-32`: release `isMinifyEnabled=false`, no `shrinkResources`, no signing config. | Larger APK, easier reverse engineering, unsigned release unless signed externally. |
| **S-15** | **MEDIUM** | No integrity/tamper/overlay defences. | No root/tamper checks, no overlay detection, no accessibility-service abuse checks, no Play Integrity. | Expected for a first version, but the threat model includes compromised devices. Must be documented as a limitation, not solved by claiming concealment. |
| **S-16** | **LOW** | Production `printStackTrace`. | `HistoryPersistenceManager.kt:46`. | May emit file paths/stack traces to logcat; no calculation data observed. Use structured, non-sensitive logging or remove. |
| **S-17** | **LOW** | Hidden SHIFT+AC exit is fragile. | `CalculatorViewModel.kt:149-161`; `MainActivity.kt:28-31`. | Not a security control; easy to trigger accidentally; inaccessible to screen-reader users; no confirmation. |
| **S-18** | **LOW** | History ID collision. | `CalculatorUiState.kt:9`. | Rare LazyColumn duplicate-key crash/visual glitch. Use a UUID or monotonic counter. |
| **S-19** | **INFORMATIONAL** | No analytics, crash reporting, network or unnecessary permissions. | dependency/manifest search. | Good privacy baseline; keep it that way unless a concrete, consented need arises. |
| **S-20** | **INFORMATIONAL** | No backend secrets or API keys found. | Source/manifest scan; only the redacted credential-like literal was found. | No secret values are reproduced here. `local.properties` contains the local SDK path only, not a credential. |

## 7.4 Data storage inventory

| Data | Location | Protection | Retention | Backup |
|---|---|---|---|---|
| Calculation history (expression, result, epoch id) | `SharedPreferences` `calculator_persistent_history_prefs` / `key_history_json` | None; app-private XML | Up to 20, indefinite | Included (allowBackup true) |
| Theme mode | `SharedPreferences` `calculator_theme_prefs` / `key_theme_mode` | None | Indefinite | Included |
| Current expression/result, modifier/security state | In-memory `StateFlow` | Process memory only | Until process death; survives background | N/A |
| Secret code | Hardcoded in DEX/source | None | Forever | N/A |
| Evidence, responders, alerts, location | **Do not exist** | — | — | — |
| Logs | Android logcat | `printStackTrace` only | Device-dependent | No |

## 7.5 Authentication and activation assessment

**Current mechanism:** compare `expression.trim()` to a 4-digit literal (`TriggerEvaluator.kt:36-41`). The `TriggerEvaluator` is installed in `MainActivity` and invoked from `onEqualsClick` before normal evaluation. If it returns `true`, the ViewModel shows the banner and skips math.

**Why this is not an authentication gate:**

- The secret is not stored securely; it is a compile-time constant.
- There is no identity, session, expiry, lockout, attempt log or recovery.
- It is a single factor with a 10,000-combination space and no rate limit.
- It can be observed by shoulder surfing; the expression and preview are visible.
- It triggers on a normal `=` press and is indistinguishable from a legitimate calculation until the banner appears.
- It does not lead anywhere; the callback is a no-op.
- It is not accessible (equals is not an accessibility action), so a blind user cannot even trigger the demo.
- It is not silent; it announces itself visually and via the lock icon description.

**Accidental activation:** exact match prevents most false positives, but a user who legitimately calculates the 4-digit value will trigger the hook. There is no confirmation, debounce, “press and hold then enter code” sequence, or cancel path. There is no separation between “I want to calculate this number” and “I want to open the protected area.”

**Lifecycle:** no timeout, no clear-on-background, no session invalidation, no task exclusion. The activity remains in recents.

**Duress:** none. No duress code, no decoy mode, no silent-alert path, no wipe policy, no plausible alternative explanation. `isLongPress` is accepted but unused, so even the intended gesture hook is inert.

**Credential storage:** none. No Keystore, no encrypted preferences, no password KDF, no biometric crypto object.

**Recommendations [R]:** see §13.6. At minimum: remove the literal; use a device-bound secret in Android Keystore; require device credential and/or a high-entropy app passphrase; use a memory-hard KDF (Argon2id/scrypt/PBKDF2 with appropriate parameters) if a passphrase is used; store only a verifier; rate-limit with exponential backoff and a persistent lockout; make activation silent; support an accessible non-visual alternative; keep duress behaviour configurable and separately designed.

## 7.6 Privacy risks specific to physical-access threats

| Risk | Current reality | Severity |
|---|---|---|
| Abuser sees hidden feature | Banner says “SECURITY HOOK TRIGGERED” / “INTERCEPTED”. | **CRITICAL** |
| Abuser finds the code | Hardcoded 4 digits; visible in source/APK; shoulder-surfable. | **CRITICAL** |
| Abuser screenshots/recents | No `FLAG_SECURE`; verified screenshot works. | **HIGH** |
| Abuser restores a backup | `allowBackup=true`; plaintext prefs. | **HIGH** |
| Abuser reads app data on rooted device | No encryption. | **HIGH** (accepted platform limitation only if documented) |
| Abuser identifies app by package name | `com.shadownet.calculator`. | **HIGH** |
| Survivor is forced to open the app | No duress/decoy; the only “protected” state is a visible banner. | **CRITICAL** |
| Sensitive data retained after use | History up to 20, indefinite, no secure wipe. | **MEDIUM** |
| Sensitive data in logs | Only stack trace today; no policy. | **LOW/MEDIUM** |

**Non-guarantee:** No Android app can reliably conceal itself from an abuser with root, a compromised OS, a hardware keylogger, or prolonged unrestricted access. ShadowNet must state this clearly in its safety guidance and should design for harm reduction, not absolute secrecy. The current app does not meet even the baseline for an uncompromised device.

## 7.7 Positive security observations

- The source manifest requests **no dangerous permissions** and no network access.
- There is no analytics, advertising, crash-reporting or third-party data SDK.
- There is no cleartext networking configuration to misconfigure.
- The app does not log calculations or secrets (apart from the generic stack trace).
- The launcher label and icon are generic calculator branding.
- The expression parser is local and does not execute arbitrary code.
- The codebase is small (≈2.3k Kotlin lines), making a security review tractable.

---

# 8. Backend and infrastructure inventory

## 8.1 What exists

| Capability | Status | Evidence |
|---|---|---|
| Firebase / google-services | **None** | No `google-services.json`, no Firebase dependency, no plugin. |
| Authentication service | **None** | No auth SDK, no login screen, no token storage. |
| Remote database | **None** | No Room/remote DB/client. |
| API client | **None** | No Retrofit/OkHttp/Ktor; no `INTERNET` permission. |
| Cloud Functions / serverless | **None** | No functions directory, no deployment config. |
| Object storage / evidence upload | **None** | No storage SDK, no upload code. |
| SMS integration | **None** | No SMS permission, provider SDK or backend endpoint. |
| Background workers | **None** | No WorkManager, JobScheduler, foreground service or coroutine worker. |
| Local persistence | **`SharedPreferences` only** | History + theme; plaintext. |
| Offline queue / sync | **None** | No queue, retry, conflict handling or sync state. |
| Environment configuration | **`local.properties` only** | Local SDK path; no `.env`, no secrets, no build flavors. |
| CI/CD | **None** | No workflow/config; only a local `deploy_to_device.sh`. |
| Monitoring / crash reporting | **None** | No SDK. |
| Backend secrets | **None present** | Nothing to expose; no secret values reproduced in this report. |

## 8.2 Implications

- The app currently **cannot** send an alert: it lacks `INTERNET`, a backend endpoint, an SMS provider and a notification channel.
- There is no server-side identity, responder directory, delivery receipt, retry scheduler, audit trail or data retention mechanism.
- There is no offline-first storage suitable for an alert queue or evidence metadata.
- There is no CI/CD or environment separation (dev/staging/production), so a future backend would be unreproducible and risky to operate.
- The absence of analytics/crash reporting is good for privacy but also means there is no operational visibility; any future telemetry must be opt-in, minimized and survivor-safe.

## 8.3 Reusable infrastructure

**Reusable:** the Android app shell, Compose UI foundation, theme, navigation host pattern to be introduced, Gradle build (after pinning), and local persistence abstraction (conceptually; the implementation must be replaced with encrypted storage).

**Not reusable:** the demo trigger, the visible security banner, plaintext prefs, the hardcoded deploy script, the current build wrapper.

**Missing components:** backend service, API contract, device registration/auth, responder store, alert queue, SMS provider integration, delivery status webhook, retry/backoff, location service, evidence storage, key management, audit logging, retention jobs, monitoring, CI/CD, environment configuration.

## 8.4 Backend options (recommendation, decision pending)

| Option | Strengths | Weaknesses | Fit |
|---|---|---|---|
| **Firebase** (Auth, Firestore, Functions, Cloud Messaging; SMS via provider extension) | Fastest to prototype; managed auth/DB; familiar Android SDK. | Google dependency; data residency/vendor lock-in; security rules are easy to misconfigure; Play Services availability; less control over SMS delivery semantics. | Acceptable for a pilot if privacy/legal review approves and a provider handles SMS. |
| **Custom serverless API** (e.g. Cloudflare Workers + D1/Postgres + Twilio/Termii) | Control over API, idempotency, retries, data minimization, region; low ops; SMS provider choice. | Requires backend engineering and operational ownership; vendor-specific runtime. | **Recommended default** for a small, safety-critical pilot. |
| **Self-hosted service** (Go/Node + Postgres + SMS gateway) | Maximum control, auditability, residency. | Highest ops/security burden; patching, scaling, on-call. | Only with an operations team. |

**Recommendation [R]:** define a backend-agnostic client interface first (`AlertGateway`, `ResponderDirectory`, `DeliveryStatusSource`), then choose the provider. Require HTTPS, certificate validation, idempotency keys, per-device credentials, rate limits, request signing/replay protection, minimal payloads, short retention, and delivery-status webhooks. Route SMS through the cloud; **do not request `SEND_SMS` on the device** unless there is a deliberate, reviewed offline fallback.

---

# 9. ShadowNet requirements gap matrix

Status definitions: **IMPLEMENTED** = working and verified; **PARTIALLY IMPLEMENTED** = some code exists but is incomplete/unsafe; **NOT IMPLEMENTED** = absent; **REQUIRES VERIFICATION** = code exists but behaviour cannot be confirmed without tests/credentials/backend.

| # | Requirement | Status | Evidence | Gap / next step | Severity |
|---|---|---|---|---|---|
| 1 | Fully functional calculator | **PARTIALLY IMPLEMENTED** | `ExpressionParser.kt`; 11 passing tests; UI in `KeypadSection.kt`; §5.5 key mismatches; §6 accessibility failures. | Fix key map, shift functions, layout, semantics, accessibility; add broad tests. Core engine is reusable. | HIGH |
| 2 | Protected authentication gate | **NOT IMPLEMENTED** | `TriggerEvaluator.kt:31-42`; `MainActivity.kt:24-26`; no auth SDK/Keystore/session. A demo comparison is not authentication. | Replace with real gate; remove hardcoded secret; define activation and recovery. | CRITICAL |
| 3 | Secure onboarding | **NOT IMPLEMENTED** | No onboarding screen, consent, safety planning, credential setup, accessibility setup or data notice. | Design consent/safety-first onboarding; accessible; minimal data; no dark patterns. | HIGH |
| 4 | Accessible emergency interface | **NOT IMPLEMENTED** | No emergency screen, no accessible SOS control, no confirmation/cancel, no non-visual path. | Build with blind/deaf/non-speaking/mobility requirements; test with users. | CRITICAL |
| 5 | Trusted responder management | **NOT IMPLEMENTED** | No responder model, storage, consent, verification, revocation or UI. | Define data model, consent, limits, revocation, notification, localization. | HIGH |
| 6 | Silent SOS alerts | **NOT IMPLEMENTED** | No alert sender; current trigger is explicitly visible (`CalculatorViewModel.kt:204-207`). | Silent send with no visible/audible/notification trace by default; configurable confirmation; cancel window. | CRITICAL |
| 7 | Location sharing | **NOT IMPLEMENTED** | No location permission, provider, service or payload. | Decide precision/frequency; foreground/background; consent; offline last-known; provider fallback. | HIGH |
| 8 | Cloud-routed SMS delivery | **NOT IMPLEMENTED** | No network permission/backend/SMS provider. | Backend endpoint + managed SMS; sender ID; templates; receipts; rate limits; regional compliance. | HIGH |
| 9 | Encrypted offline alert queue | **NOT IMPLEMENTED** | No WorkManager/queue/database/encryption. | Encrypted local queue, idempotency, ordering, expiry, deduplication, secure deletion. | CRITICAL |
| 10 | Alert delivery status and retry | **NOT IMPLEMENTED** | No status model, scheduler, backoff or UI. | WorkManager/foreground retry, exponential backoff, idempotency, terminal states, accessible status. | HIGH |
| 11 | Protected evidence capture | **NOT IMPLEMENTED** | No camera/mic/photo picker, storage or permissions. | Consentful capture, foreground indication, safe file handling, no cloud by default. | HIGH |
| 12 | Client-side evidence encryption | **NOT IMPLEMENTED** | No crypto library/Keystore. | AEAD encryption with per-file keys wrapped by Keystore; authenticated metadata; test vectors. | CRITICAL |
| 13 | Evidence integrity verification | **NOT IMPLEMENTED** | No hashing/signing/manifest. | SHA-256 + HMAC/signature over ciphertext + metadata; verify before recovery/export; chain-of-custody notes. | HIGH |
| 14 | Secure evidence recovery | **NOT IMPLEMENTED** | No recovery/export/restore flow. | Authenticated recovery, key escrow policy, rate limits, audit, user consent, secure deletion. | HIGH |
| 15 | Duress handling | **NOT IMPLEMENTED** | No duress code/decoy/silent path; `isLongPress` unused (`CalculatorViewModel.kt:198`). | Design duress credential/gesture, decoy behaviour, silent alert, safe cancellation; test for coercion. | CRITICAL |
| 16 | Application lifecycle protection | **NOT IMPLEMENTED** | No `FLAG_SECURE`, `excludeFromRecents`, timeout, clear-on-stop, backup rules. | Harden activity/task; clear sensitive UI; timeout; block screenshots; disable backup for sensitive data. | CRITICAL |
| 17 | Privacy and data-retention controls | **PARTIALLY IMPLEMENTED** | Clear-history action (`CalculatorViewModel.kt:270-273`); max 20 (`:222`); no retention setting, no secure delete, backup enabled. | Retention policy, per-data-type TTL, export/delete, backup exclusion, privacy notice. | HIGH |
| 18 | Demonstration mode | **NOT IMPLEMENTED** | No demo mode; the sample trigger is a hardcoded demo, not a safe demo mode. | Isolated synthetic data, obvious demo banner, no real alerts, no production credentials, reset. | MEDIUM |
| 19 | Comprehensive accessibility support | **PARTIALLY IMPLEMENTED** | Some content descriptions and mostly ≥48 dp keys; but 45/45 unnamed, equals inaccessible, clipping, contrast failures, no announcements. | Full accessibility program; see §6.7; physical-device and user testing. | CRITICAL |

**Summary:** 0 fully implemented; 3 partial (calculator, privacy controls, accessibility); 16 not implemented. There is no ShadowNet emergency capability to reuse.

---

# 10. Reusable components

| Component | Reuse decision | What to keep | What to change | Evidence |
|---|---|---|---|---|
| **Expression engine** | **Reuse with fixes** | Tokenizer, shunting-yard, RPN, function set, error model, decimal formatting approach. | Fix unary-minus precedence, percent semantics, overflow message; add limits, locale policy, fuzz/property tests. | `ExpressionParser.kt`; 11 tests pass; probes in §5.3. |
| **Calculator state pattern** | **Reuse pattern** | Immutable `CalculatorUiState`, `StateFlow`, explicit ViewModel actions. | Remove security fields, add `SavedStateHandle`, make persistence async/encrypted, lifecycle-aware collection. | `CalculatorUiState.kt`, `CalculatorViewModel.kt`. |
| **Theme/palette** | **Reuse with remediation** | Casio visual identity, light/dark/system modes, centralised tokens. | Fix contrast, remove deprecated window APIs, add high-contrast and font-scale support. | `Color.kt`, `Theme.kt`, `ThemePreferenceManager.kt`. |
| **Display/keypad composables** | **Reuse as visual base** | Shape, hierarchy, faceplate look, key grouping. | Rebuild semantics, responsive layout, 48 dp guarantees, key map, state descriptions, focus order. | `DisplaySection.kt`, `KeypadSection.kt`. |
| **History model/persistence** | **Model reuse; storage replace** | `CalculationHistoryItem` shape, limit concept, clear action. | Encrypt at rest, add retention/TTL, secure delete, backup exclusion, non-colliding IDs. | `CalculatorUiState.kt:8-12`, `HistoryPersistenceManager.kt`. |
| **TriggerEvaluator seam** | **Concept reuse; implementation replace** | A narrow interface between “equals” and the host. | Make async/result-bearing, no hardcoded secret, no visible state, no direct UI coupling. | `TriggerEvaluator.kt`. |
| **Build foundation** | **Reuse** | Kotlin, Compose, Material 3, AndroidX, single-activity host, JDK 17 target. | Pin Gradle, add CI, signing, R8, flavors, dependency verification, tests. | `app/build.gradle.kts`, `build.gradle.kts`. |
| **Design asset/icon** | **Reuse** | Generic calculator icon/branding for public disguise. | Ensure no ShadowNet identifiers in icon/asset metadata; keep release assets minimal. | `statics/shadownet.png`, mipmaps. |
| **Local deploy script** | **Do not reuse** | ADB convenience idea only. | Replace hardcoded paths/toolchain; call `./gradlew`; separate debug/release; never deploy debug to participants. | `deploy_to_device.sh`. |
| **Demo trigger / banner** | **Do not reuse** | Nothing. | Delete/replace; no visible security state. | `TriggerEvaluator.kt`, `DisplaySection.kt`, `CalculatorViewModel.kt`. |

**Why retain the stack:** the app is small, the parser is tested and pure, and a rewrite would discard the only verified functional asset while adding risk. The deficiencies are concentrated in UI semantics, layout, security plumbing and missing features—all addressable by extension. There is no concrete justification for a new UI framework, cross-platform runtime or backend framework at this stage.

---

# 11. Technical risks and blockers

| ID | Risk | Severity | Evidence | Impact | Mitigation |
|---|---|---|---|---|---|
| R-01 | Build is not reproducible with the checked-in toolchain. | **CRITICAL (delivery)** | Wrapper 8.7 vs AGP 8.7.2 (needs 8.9+); deploy script uses 9.7.1 and failed `processDebugResources` after 18m14s. | No reliable CI, release or onboarding of engineers; every “does it build?” question is ambiguous. | Pin Gradle 8.9–8.13; use `./gradlew` everywhere; remove hardcoded tool paths; add clean-checkout CI. |
| R-02 | Hardcoded credential and no authentication. | **CRITICAL (security)** | `MainActivity.kt:25`, `TriggerEvaluator.kt:32`; no auth stack. | False sense of protection; trivial bypass; safety risk. | Remove secret; design/implement real auth in WP3. |
| R-03 | Visible interception state. | **CRITICAL (safety)** | Banner/strings in `DisplaySection.kt`/`CalculatorViewModel.kt`. | Abuser discovers hidden feature; potential escalation. | Silent activation; no banner/announcement; WP3. |
| R-04 | Accessibility is not usable by blind users. | **CRITICAL (accessibility)** | 45/45 unnamed; equals non-clickable; clipping. | Excludes the primary audience; fails mandatory requirement. | WP1 design system + WP2 hardening + WP9 validation. |
| R-05 | No backend or delivery infrastructure. | **HIGH (schedule)** | §8. | SOS cannot work; backend is greenfield; SMS/location/retry require design, contracts, ops and legal review. | Decide backend in WP1; build contract-first; pilot with a provider. |
| R-06 | SMS delivery, regional compliance and cost. | **HIGH** | No provider; unknown countries. | Alerts may fail, be delayed, be blocked, or violate regulations; sender ID rules vary. | Stakeholder decisions; provider evaluation; delivery receipts; fallback channels; legal review. |
| R-07 | Background location restrictions. | **HIGH** | No location code; Android 11–15 foreground-service/background-location rules. | SOS may not obtain location when needed; OEM battery killers (TECNO/others) may kill workers. | Foreground service with correct type; last-known fallback; OEM guidance; test on target devices. |
| R-08 | Cryptographic/key-recovery design. | **HIGH** | No crypto; evidence recovery requirement. | Custom crypto or lost keys can destroy evidence or expose it; recovery conflict with concealment. | Use vetted AEAD/Keystore; define escrow/recovery policy; test vectors; external review. |
| R-09 | Duress design can increase danger. | **HIGH** | No duress; current visible trigger. | A poorly designed duress/decoy path may alert the abuser or fail silently when needed. | Co-design with survivor-safety specialists; threat-model coercion; test; configurable. |
| R-10 | No VCS, CI, tests, lint or docs. | **HIGH (process)** | No `.git`, no CI, 11 parser tests only, lint blocked. | Regressions, lost work, unreviewable changes, no release gate. | WP1: git, `.gitignore`, CI, lint, test strategy, ADRs. |
| R-11 | Misleading key map / incomplete calculator. | **HIGH (product)** | §5.5. | Users cannot trust the public interface; blind users get wrong actions; “fully functional” claim is false. | WP2: fix or remove non-functional keys; test every key; align labels/actions. |
| R-12 | Low-end device performance and size. | **MEDIUM** | Debug APK 16.7 MB; tested on TECNO Android 11, armeabi-v7a; Compose + potential crypto/DB/SQLCipher additions. | Slow startup, memory pressure, large download, battery drain. | R8/resource shrinking; avoid heavy native libs; baseline profiles; measure on 1–2 GB devices; prefer LocationManager. |
| R-13 | RTL/localization. | **MEDIUM** | `supportsRtl=true`; forced-RTL test mirrored keypad/display; all strings English. | Unexpected layout for RTL users; untranslated safety instructions. | Decide calculator LTR policy; add RTL-aware layout; localize with reviewed translations. |
| R-14 | App-store/distribution policy. | **MEDIUM** | No store config; SMS/location/emergency claims attract review. | Rejection or removal at a critical moment; sideloading has update/security trade-offs. | Early policy review; clear safety documentation; staged rollout; update channel. |
| R-15 | Evidence integrity/legal chain of custody. | **HIGH (legal)** | No hashing/export/audit. | Evidence may be inadmissible or challenged; recovery may leak data. | Define integrity manifest, timestamps, export format, custody log; legal review. |
| R-16 | Data retention/privacy compliance. | **HIGH** | No policy; backup enabled; plaintext prefs. | Regulatory breach; survivor data retained too long; compelled disclosure risk. | DPIA, retention matrix, deletion jobs, backup exclusion, privacy notice, consent. |
| R-17 | Dependency/supply chain. | **LOW** | Gradle wrapper JAR; versions pinned but no lockfile/verification; `distributionSha256Sum` absent. | Tampered dependency or unexpected upgrade. | Dependency verification/lockfiles; wrapper checksum; Dependabot/Renovate with review. |
| R-18 | Release hardening/signing. | **MEDIUM** | `isMinifyEnabled=false`, no signing config. | Larger, easier-to-reverse APK; unsigned release. | Configure release signing in CI/secret store; R8; shrink; audit. |
| R-19 | Physical-device accessibility testing gap. | **MEDIUM** | TalkBack/Switch Access disabled; no user testing. | Structural fixes may still produce poor spoken UX. | WP9 with blind/low-vision/deaf/motor-impaired testers. |
| R-20 | Compromised-device limitations. | **INFORMATIONAL** | No tamper/root/overlay defence. | Cannot guarantee concealment. | Document limits; harm-reduction guidance; optional integrity signals with privacy trade-offs. |

---

# 12. Build and test results

## 12.1 Commands and exact outcomes

Environment for all commands: JDK `/home/zayne/.jdks/jbr-21.0.11` (21.0.11), Android SDK at the local SDK path, Linux. No application source was modified; the successful build was performed in `/tmp/opencode/shadownet-verify`, a copy excluding build outputs/caches. Verification runs refreshed generated build outputs under `app/build/`, which are not source.

| # | Command | Result | Details |
|---|---|---|---|
| 1 | `./gradlew --version` | **Gradle 8.7** | Wrapper runs; no build attempted. |
| 2 | `./gradlew testDebugUnitTest` | **FAILED** after 2m06s | `Failed to apply plugin 'com.android.internal.version-check'`: “Minimum supported Gradle version is 8.9. Current version is 8.7.” at `app/build.gradle.kts:1`. |
| 3 | `./deploy_to_device.sh` (user-run) | **FAILED** after 18m14s | Script selected Gradle 9.7.1; `:app:processDebugResources FAILED`; “Cannot load processed manifest files, please file a bug.” 21 tasks executed. Device detected; debug APK not rebuilt. |
| 4 | `gradle testDebugUnitTest assembleDebug --offline --no-daemon` (Gradle 8.13, isolated copy) | **BUILD SUCCESSFUL in 9m42s** | 41 tasks executed; 11 tests, 0 failures; debug APK 16,674,848 bytes. |
| 5 | Direct JUnit: `java -cp <debug-classes>:<test-classes>:<kotlin-stdlib>:<junit>:<hamcrest> org.junit.runner.JUnitCore com.shadownet.calculator.ExpressionParserTest` | **OK (11 tests)** | 0.936 s; confirms the compiled test classes from the Gradle build pass independently. |
| 6 | `gradle lintDebug --offline --no-daemon` (isolated copy) | **FAILED** after 1m24s | `:app:generateDebugAndroidTestLintModel FAILED`: no cached `androidx.compose.ui:ui-test-junit4:1.7.4` and `androidx.collection:collection-jvm:1.4.0` for offline mode. No lint report generated. |
| 7 | `aapt2 dump badging/permissions` on existing debug APK | **Success** | `application-debuggable`; min 24/target 35; 4 ABIs; no runtime permissions; label “Scientific Calculator”. |
| 8 | On-device UI Automator dump + screenshot | **Success** | 143 nodes; 45 clickable, 0 named; `=` non-clickable; bottom row clipped; screenshot allowed. |

## 12.2 Compilation warnings

Only three Kotlin warnings were emitted:

- `HistoryOverlay.kt:128:29` — `Divider` is deprecated; renamed to `HorizontalDivider`.
- `Theme.kt:53:20` — `statusBarColor` deprecated.
- `Theme.kt:54:20` — `navigationBarColor` deprecated.

No compilation errors with Gradle 8.13. `stripDebugDebugSymbols` reported it could not strip `libandroidx.graphics.path.so`; the library was packaged as-is (debug build, not a failure).

## 12.3 Test coverage

- **11 unit tests**, all parser tests, all pass.
- Covered: basic arithmetic, parentheses/precedence, power associativity, DEG/RAD trig, logs, roots/constants, implicit multiplication, percent/factorial, division by zero, log domain.
- **Not covered:** UI, Compose semantics, ViewModel state transitions, history persistence, theme persistence, trigger/security, shift/alpha, every key mapping, error formatting, accessibility, lifecycle, backup, encryption, RTL, localization, performance.
- No instrumentation tests exist (`app/src/androidTest` absent) despite AndroidX test dependencies.
- No JaCoCo/Kover configuration; coverage percentage is **not available**.
- No lint report; no ktlint/detekt/Detekt baseline.

## 12.4 APK facts

| Property | Value |
|---|---|
| Existing debug APK | 16,830,404 bytes, timestamp 2026-10-29 |
| Isolated build APK | 16,674,848 bytes |
| Package | `com.shadownet.calculator` |
| Version | 1.0.0 (1) |
| min / target / compile | 24 / 35 / 35 |
| Debuggable | Yes (`application-debuggable`) |
| ABIs | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Permissions | Only signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |
| Release config | minify off, no signing config (`app/build.gradle.kts:24-32`) |

## 12.5 On-device verification details

- Installed package: `com.shadownet.calculator`, versionName 1.0.0.
- Device: TECNO BD4j, Android 11 (API 30), `armeabi-v7a`, 720×1600, density override 256 dpi, font scale 0.85, force-RTL enabled.
- TalkBack installed but accessibility disabled; no spoken test performed.
- Screenshot capture succeeded, proving `FLAG_SECURE` is absent.
- UI Automator dump showed the layout/semantics issues in §6.
- A redacted 4-digit code produced the visible `SECURITY HOOK TRIGGERED` / `INTERCEPTED` state and no protected screen; this matches source review. No alert was sent and no survivor data was used.

## 12.6 Build/test blockers

1. **Wrapper/AGP mismatch** — must be fixed before any CI or release work.
2. **Deploy script toolchain** — must use the wrapper, not a hardcoded Gradle 9.7.1 path.
3. **Missing cached test dependencies for lint** — lint cannot run offline; resolve by configuring a supported online dependency cache or removing unused androidTest dependencies, then run lint in CI.
4. **No instrumentation/accessibility tests** — cannot verify UI behaviour automatically.
5. **No version control/CI** — no clean-checkout reproducibility or review gate.

---

# 13. Recommended target architecture

## 13.1 Retain or rewrite?

**Retain the existing technology stack and extend the app.** There is no evidence that a rewrite is justified:

- The parser is pure Kotlin, tested, and isolated.
- The Compose UI is small and visually distinctive.
- The app already targets the right platform and minimum OS.
- A rewrite would discard the only verified functional component and introduce a new accessibility/security baseline.
- The deficiencies are addressable: semantics, layout, key mapping, storage, auth, backend.

Do **not** introduce a new UI framework, cross-platform runtime, reactive database or backend framework without a concrete requirement. Add libraries only for specific, reviewed needs (WorkManager, a vetted crypto library, a location provider, a backend SDK if chosen).

## 13.2 Proposed module boundaries

For a small team, start with **three modules** and split when justified:

```text
:app                    # host activity, composition root, build flavors
:core                   # models, accessible design system, crypto, storage, network contracts
:feature:calculator     # public calculator screen (no sensitive permissions)
:feature:emergency      # accessible SOS/emergency UI
:feature:onboarding     # consent, safety planning, credential setup
:feature:responders     # trusted responder management
:feature:evidence       # capture, encryption, integrity, recovery
:core:security          # auth, Keystore, duress, lifecycle, rate limits
:core:data              # encrypted storage, queue, retention
:core:network           # API client, idempotency, delivery status
:core:designsystem      # accessible primitives, tokens, semantics helpers
:core:testing           # fakes, test rules, accessibility checks
```

**Practical phasing:** `:app` + `:core` + `:feature:calculator` first; add `:feature:emergency` and `:core:security` for WP3/WP5; add data/network/evidence when those features begin. Ten modules on day one would slow a small team; package boundaries plus `internal` visibility are acceptable until the code justifies enforcement.

**Isolation rules:**

- `:feature:calculator` must not depend on `:core:security`, `:core:data` or emergency features. It exposes only calculator state/events.
- The host owns the state machine: `Calculator → ActivationGate → ProtectedShell`.
- Protected screens live in a separate activity/task with `FLAG_SECURE`, `excludeFromRecents`, no history and clear-on-stop. Consider a separate process only if a concrete threat requires it; it adds IPC, startup and debugging cost.
- Sensitive permissions and components are declared only where needed; review the merged manifest per flavor.
- No sensitive strings/resources in the calculator module or its logs.

## 13.3 Host state machine and navigation

Use a small, explicit host state machine rather than a full navigation graph initially:

```text
AppState:
  Calculator
  Authenticating(attemptContext)
  Protected(session)
  Locked(reason)
  DuressDecoy
```

- `Calculator` is the only state visible on launch and after timeout.
- `Authenticating` is silent; no banner, no title change, no obvious transition.
- `Protected` runs in a separate activity/task; timeout/background clears it.
- `Locked` shows the calculator (or a neutral screen), never “locked by ShadowNet”.
- `DuressDecoy` must be indistinguishable from normal calculator behaviour by default; alerting behaviour is policy-driven.

If navigation complexity grows (deep links, responder flows, evidence review), adopt Navigation Compose or a typed state machine. Do not add it solely for the current two-screen need.

## 13.4 Persistence and encryption

| Data | Recommended storage | Protection |
|---|---|---|
| Theme, accessibility preferences | DataStore/SharedPreferences | Non-sensitive; exclude from backup if desired. |
| Activation verifier, attempt counters, session metadata | Keystore-wrapped encrypted prefs/DataStore | AES-GCM; key in Android Keystore (hardware-backed where available); no plaintext secret. |
| Responders, alert queue metadata, evidence index | Room (or encrypted record store) | Field-level AEAD or SQLCipher; per-record nonce; authenticated metadata. |
| Evidence blobs (photo/audio/file) | App-private files | Per-file AES-GCM key wrapped by Keystore; hash + HMAC/signature; no plaintext cache. |
| Offline alert queue | Encrypted Room/WorkManager input | Idempotency key, expiry, retry count, no plaintext PII. |
| Logs/analytics | None by default | If added: opt-in, redacted, short retention, no identifiers. |

**Crypto guidance [R]:** use a vetted library (e.g. Google Tink) or a well-reviewed Keystore/AES-GCM wrapper; do not invent a cipher, key derivation or recovery scheme. Store only a verifier for passphrases (Argon2id/scrypt/PBKDF2 with reviewed parameters), never the passphrase. Use constant-time comparisons. Define key loss/recovery explicitly: evidence recovery and concealment are in tension; the stakeholder must choose escrow/recovery behaviour.

**Why not plaintext prefs:** they are included in backups by default, readable on rooted devices and easy to inspect. `SharedPreferences` is acceptable for the public calculator’s cosmetic settings, not for protected data.

## 13.5 Backend integration

**Client contract [R]:**

- Device registration with a random device credential stored in Keystore; short-lived access tokens; rotation/revocation.
- `POST /alerts` with idempotency key, minimal payload, client timestamp, optional encrypted location/evidence references; server returns an alert id and accepted status.
- `GET /alerts/{id}/status` or webhook/push for delivery states; retry with exponential backoff and jitter.
- Responder directory scoped to the user/account; consent and revocation endpoints.
- No survivor identity in SMS body; use a neutral sender and a minimal message; define fallback channels.
- Server-side rate limits, abuse detection, audit logs with minimal retention, regional data handling.

**Provider choice:** see §8.4. The client should depend on interfaces, not a vendor SDK, so a pilot can swap providers.

## 13.6 Authentication, duress and lifecycle

**Authentication [R]:**

- Prefer **device credential** (`BiometricPrompt` with `DEVICE_CREDENTIAL` allowed) plus an app-level secret for portability/duress.
- If an app passphrase is used, require sufficient entropy, use a memory-hard KDF, store a verifier in Keystore-protected storage, and rate-limit.
- Support an accessible alternative: no reliance on long-press, drag patterns or precise timing; allow TalkBack/Switch Access; provide a non-visual confirmation.
- Attempt limits: persistent counter, exponential backoff, lockout after a configurable number, optional wipe after a higher threshold (with explicit consent and safe defaults).
- Session: short idle timeout, clear on background, re-auth for sensitive actions.
- Recovery: define account/device recovery that does not create an obvious “ShadowNet” trail; avoid security questions that an abuser can answer.

**Activation [R]:**

- Remove the hardcoded literal and the “equals-only” comparison.
- Use a deliberate sequence that is unlikely during normal calculation, e.g. a specific modifier combination followed by a credential, or a dedicated invisible affordance with a confirmation step. It must not produce a visible/audible state until authentication succeeds.
- Prevent accidental activation with exact sequence matching, a time window, debounce, and a cancel path. Do not break legitimate calculations that happen to equal a number.
- Provide a duress credential/gesture that opens a decoy or sends a silent alert according to policy, without revealing which path was taken.
- Test with blind, deaf, non-speaking and motor-impaired users; a secret gesture must have an accessible equivalent.

**Lifecycle [R]:**

- `FLAG_SECURE` on protected activities (and consider calculator during code entry).
- `excludeFromRecents`, `noHistory`, `setRecentsScreenshotEnabled(false)` where appropriate.
- Clear sensitive Compose state and clipboard on `onStop`/timeout; avoid sensitive text in accessibility announcements.
- Disable backups for sensitive data (`allowBackup=false` or explicit `dataExtractionRules`/`fullBackupContent`).
- Do not log sensitive state; disable debug builds for participants.

## 13.7 Accessibility architecture

- **Design system first:** `AccessibleKey`, `AccessibleIconButton`, `AccessibleDisplay`, `AccessibleDialog` with enforced 48 dp targets, labels, roles, states, focus grouping and haptics.
- **Semantics contract:** every interactive node has one name, one role, one action; decorative text is hidden; state changes use `stateDescription`/`toggleableState`; results/errors use live regions only where safe.
- **Layout:** responsive/scrollable, system-inset aware, no fixed 210 dp panel on small screens; test 320×480 through tablets, portrait/landscape, font scale 1.0–2.0.
- **Input:** touch, keyboard, Switch Access, TalkBack, voice access; no gesture-only or long-press-only path for critical actions.
- **Multi-sensory:** visual + haptic + optional audio; never rely on one channel; respect reduced motion and sound settings.
- **Privacy-aware announcements:** never announce “ShadowNet”, “SOS”, “duress”, “evidence” or a security state in the calculator. On protected screens, use neutral labels and consider `invisibleToUser` for truly hidden controls—but never make a critical action unreachable.
- **Testing:** automated `AccessibilityChecks`, contrast unit tests, TalkBack/Switch Access scripts, and paid testing with disabled users before release.

## 13.8 Background tasks and low-end compatibility

- **WorkManager** for queued alerts, retries, delivery polling and retention jobs; unique work + idempotency; network constraint; exponential backoff.
- **Foreground service** for active SOS/location/evidence capture with a clear, neutral notification (or a policy-approved silent mode where legally/technically possible); declare `foregroundServiceType` correctly for Android 14+.
- **Location:** prefer `LocationManager`/`getCurrentLocation` to avoid a Play Services dependency on low-end/no-GMS devices; fall back to last-known; request fine/coarse only when needed; explain consent.
- **Do not use exact alarms** unless a concrete requirement justifies the permission/policy cost.
- **Low-end:** minSdk 24, armeabi-v7a, 1–2 GB RAM; avoid SQLCipher/native libs unless necessary; enable R8/resource shrinking; add baseline profiles; measure cold start, memory, battery and APK size on the TECNO-class device.
- **OEM battery optimisation:** provide guidance and test on TECNO/Xiaomi/Samsung et al.; do not assume WorkManager runs promptly.

## 13.9 Estimated implementation complexity

Assumptions: one senior Android engineer, one backend engineer, accessibility consultant and survivor-safety/legal review; estimates are order-of-magnitude and exclude app-store/legal delays.

| Work package | Estimate (engineer-weeks) | Confidence | Notes |
|---|---|---|---|
| WP1 Foundation, build, accessibility design system | 3–4 | Medium | Toolchain, CI, modules, semantics primitives, threat model. |
| WP2 Calculator hardening | 3–4 | Medium | Layout, key map, tests, RTL, contrast. |
| WP3 Secure activation, auth, lifecycle, duress design | 4–6 | Low–medium | Crypto/Keystore, biometrics, rate limits, silent flow, safety review. |
| WP4 Onboarding + responders | 4–5 | Medium | Consent, encrypted storage, directory, revocation. |
| WP5 Accessible emergency UI + silent SOS | 4–6 | Medium | Multi-modal UX, confirmation/cancel, user testing. |
| WP6 Location + cloud SMS + queue + status/retry | 6–8 | Low–medium | Backend, provider, receipts, retries, regional rules. |
| WP7 Evidence capture/encryption/integrity/recovery | 6–8 | Low | Crypto, storage, export, chain of custody, legal. |
| WP8 Demo mode, retention, privacy controls, release hardening | 3–4 | Medium | Synthetic data, TTL, backup rules, signing/R8. |
| WP9 Accessibility/security validation and pilot | 4–6 | Medium | TalkBack/Switch Access, disabled testers, pen test, low-end devices. |
| **Total** | **37–51** | **Low–medium** | Plus backend operations, SMS costs, translation, legal/DPIA. |

The range is deliberately wide because the backend, duress and evidence requirements are under-specified. WP1 should refine estimates after the stakeholder decisions in §15.

---

# 14. Proposed implementation work packages

**WP0 — Repository audit (this document).** Complete. Read-only; no feature code; architecture and gaps identified.

**WP1 — Foundation, reproducibility and accessibility design system.**
- Fix Gradle/AGP compatibility; make `./gradlew` canonical; remove hardcoded deploy paths.
- Initialise git, `.gitignore`, CI (build + unit tests + lint), release signing plan, dependency verification.
- Introduce module boundaries and host state machine skeleton.
- Build accessible primitives (48 dp, semantics, focus, contrast tokens, haptics) and a test harness.
- Produce ADRs for auth, storage, crypto, backend and accessibility.
- **Exit:** clean checkout builds/tests/lints in CI; calculator renders unchanged; semantics primitives demonstrated; no security feature added.

**WP2 — Calculator production hardening.**
- Responsive/scrollable layout; fix clipping and font scaling; 48 dp everywhere.
- Correct every key label/action or remove unsupported keys; fix shift log/ln; decide percent/factorial semantics.
- Add accessible names/states/actions; make `=` fully accessible; add result/error announcements.
- Fix contrast; add RTL policy; keyboard/switch/haptics.
- Expand unit/UI/accessibility tests; run TalkBack smoke tests.
- **Exit:** blind tester can calculate independently; no clipped keys at 200% font; all keys truthful; contrast AA.

**WP3 — Secure activation, authentication, lifecycle and duress design.**
- Remove hardcoded secret and visible banner.
- Implement silent activation sequence with accessible alternative.
- Device credential/biometric + app secret verifier in Keystore; rate limits/lockout; session timeout.
- `FLAG_SECURE`, recents exclusion, clear-on-stop, backup rules.
- Duress/decoy design with safety review; no real alerts yet.
- **Exit:** no secret in source/APK; silent activation; bypass/rate-limit tests; TalkBack path works; threat-model review signed off.

**WP4 — Secure onboarding and trusted responders.**
- Consent/safety planning, accessibility setup, credential setup, privacy notice.
- Encrypted responder store, add/verify/revoke, limits, notification preferences.
- **Exit:** survivor can configure responders without exposing data; deletion/revocation verified.

**WP5 — Accessible emergency interface and silent SOS.**
- Multi-modal SOS control (touch, keyboard, switch, voice where available), confirmation/cancel policy, offline state.
- Silent alert creation; no visible trace by default; accessible status.
- User testing with blind/deaf/non-speaking/motor-impaired participants.
- **Exit:** end-to-end fake alert to a test responder; no real alerts; cancel works; accessibility sign-off.

**WP6 — Location, cloud SMS, encrypted queue, delivery status and retry.**
- Backend contract, device auth, idempotency, rate limits, SMS provider, receipts.
- Encrypted offline queue, WorkManager retries/backoff, foreground service where needed.
- Delivery status UI and operational runbook.
- **Exit:** alerts deliver in test regions; offline queue survives reboot; duplicate suppression; failure states visible/accessible.

**WP7 — Evidence capture, encryption, integrity and recovery.**
- Consentful capture (camera/audio/file), encrypted at rest, hashes/HMAC, integrity manifest.
- Recovery/export with authentication, audit and retention; chain-of-custody documentation.
- **Exit:** evidence survives restart; tamper detection works; recovery tested; legal review.

**WP8 — Demo mode, retention, privacy controls and release hardening.**
- Isolated synthetic demo mode with obvious demo indicator and no real alerts.
- Retention/TTL, delete/export, backup exclusion, secure deletion.
- R8/shrink, signing, release checklist, store/privacy documentation.
- **Exit:** demo cannot contact production; retention jobs tested; signed release candidate.

**WP9 — Accessibility and security validation, pilot.**
- TalkBack, Switch Access, keyboard, 200% font, contrast, RTL, low-end performance.
- External pen test, crypto review, safety review, usability pilot.
- **Exit:** release gate met; residual risks documented; go/no-go.

**Sequencing:** WP1 and WP2 can overlap. WP3 must precede any protected feature. Backend decision is needed before WP6; legal/DPIA before WP4/WP6/WP7; accessibility testing is continuous, not only WP9.

---

# 15. Questions requiring stakeholder decisions

These are decisions the code cannot make. They should be resolved before WP3/WP6/WP7; several affect safety and law, not just engineering.

## Activation and duress

1. What activation methods are required (secret code, gesture, device credential, biometric, hidden button, combination)? Which must work for blind, deaf, non-speaking and motor-impaired users?
2. Should normal calculations ever trigger activation if they equal a code? How should the app distinguish “calculation” from “credential”?
3. What is the duress behaviour: silent alert, decoy calculator, wipe, fake error, or nothing? Who approves the safety design?
4. Should a duress session look identical to a normal session? For how long? What happens if the abuser keeps the device?
5. Is a panic wipe required? Under what confirmation/consent? What are the legal/evidentiary consequences?
6. How are forgotten credentials recovered without creating an obvious ShadowNet trail or a backdoor?

## Authentication and lifecycle

7. Device credential only, app passphrase only, or both? Is biometric required/allowed? What is the fallback on devices without biometrics?
8. What are the attempt limits, lockout duration and wipe threshold? Should attempts survive reboot/reinstall?
9. What is the idle/background timeout? Should the calculator clear on background, or only protected screens?
10. What is the supported OS range and device baseline (minSdk 24? Android 11+? 2 GB RAM? armeabi-v7a)?

## Responders and alerts

11. How many trusted responders per user? How are they verified/consented? Can the survivor revoke them? What if a responder changes number?
12. What exactly is sent in an SMS (neutral text, link, code, location, no identity)? What sender ID is legal in each target country?
13. Which countries/regions must be supported? Which SMS provider, and what are the delivery/status/cost constraints?
14. Are alerts one-shot or persistent? What is the retry/expiry policy? What happens when the device is offline for hours?
15. Are silent notifications required? Is a notification legally/technically required for a foreground service? How is that reconciled with concealment?
16. What is the fallback if SMS fails (push, voice call, second provider, pre-designated contact app)?

## Location and evidence

17. What location precision/frequency is acceptable? Is background location required? Is last-known acceptable? How is consent obtained and withdrawn?
18. What evidence types (photo, audio, video, text, screenshots)? Is capture overt or covert? What are the legal/consent constraints?
19. Who can recover evidence, and how? Is key escrow allowed? What happens if the device is lost? What is the chain-of-custody/export format?
20. What retention periods apply to alerts, location, evidence and responder data? What must be deletable on request?

## Backend, privacy and operations

21. Backend option: Firebase vs custom serverless vs self-hosted? Hosting region? Who operates it? What is the breach-response plan?
22. What is the data-protection jurisdiction and legal basis (e.g. GDPR, Kenya DPA, other)? Is a DPIA required? Who signs off?
23. What telemetry, if any, is acceptable? Crash reporting must not include identifiers/content. Is analytics prohibited entirely?
24. How is demo mode isolated? Can it ever contact production? What synthetic data is used?

## Product, branding and distribution

25. Should the application ID/package remain `com.shadownet.calculator`? (Recommendation: neutralise before first release.) What about class names, assets and store listing?
26. Distribution: Play Store, direct APK, MDM, NGO channel? What update mechanism? How are safety advisories communicated?
27. What languages/locales are required? Should the calculator keypad stay LTR in RTL locales? Who reviews translations of safety-critical text?
28. What accessibility standard is the release gate (WCAG 2.2 AA, Android accessibility guidelines, local regulation)? Who are the disabled user testers?
29. What is the timeline/budget, and which work packages are MVP vs later? Is a pilot with one region/one responder group acceptable?

---

# 16. Final readiness assessment

## 16.1 Scorecard

| Area | Status | Summary |
|---|---|---|
| Calculator engine | **GREEN** | Pure Kotlin, tested, reusable. Needs precedence/percent/overflow fixes and broader tests. |
| Calculator UI | **RED** | Clipped bottom row, misleading keys, dead scientific mode, no accessible names, equals inaccessible. |
| Accessibility | **RED** | Fails independent blind operation; contrast failures; no announcements; no Switch Access/keyboard support; no user testing. |
| Security | **RED** | Hardcoded credential, no auth, visible trigger, no encryption, no lifecycle protection. |
| Privacy | **RED** | Backup enabled, plaintext prefs, screenshots allowed, package name identifies product. |
| Backend/infrastructure | **RED (greenfield)** | No network permission, API, DB, SMS, queue, workers or CI/CD. |
| Build/reproducibility | **AMBER** | Source builds with Gradle 8.13; checked-in wrapper and deploy path are broken. |
| Tests/quality gates | **RED** | 11 parser tests; no UI/security/accessibility tests; lint blocked; no CI. |
| Documentation/process | **RED** | No git, README, architecture docs, threat model, privacy policy or ADRs. |
| Architecture-design readiness | **GREEN** | Enough verified information to design without guessing. |

## 16.2 Is the audit complete?

**Yes, for the stated scope.** We have verified:

- the technology stack, structure, entry point, build system and dependencies;
- the calculator’s engine, UI, state, history, theme, key map and error handling;
- the input/navigation seams relevant to a protected gate;
- the accessibility tree, touch targets, contrast, font/layout behaviour and announcement risks;
- the manifest, permissions, storage, backup, screenshot and lifecycle posture;
- the absence of backend/infrastructure;
- the 19 requirement gaps with evidence;
- build/test results and blockers.

**Not verified (requires future work):** exact TalkBack/Switch Access spoken behaviour, 200% font rendering, keyboard navigation, real low-end performance under load, cryptographic designs, backend delivery, legal admissibility, and user safety outcomes.

## 16.3 What can proceed safely

- **Architecture design and WP1 planning** can proceed now.
- The parser, theme foundation and visual design can be retained.
- Accessibility and security must be treated as first-class architecture, not later polish.
- No emergency feature should be built on the current trigger or plaintext storage.

## 16.4 Conditions before WP1

1. Stakeholder answers to §15, especially activation/duress, auth, backend, jurisdiction and accessibility standard.
2. Approval to fix the build toolchain and initialise version control/CI.
3. Agreement that the hardcoded credential and visible banner will be removed, not reused.
4. A named accessibility lead/test group and a survivor-safety reviewer.
5. A decision on backend provider/hosting and a data-protection review.
6. Acceptance that concealment cannot be guaranteed on compromised devices.

## 16.5 Final statement

ShadowNet currently consists of a **credible calculator prototype with a demo security hook**, not a survivable safety product. The calculator engine is worth keeping; the security story and accessibility are not. The most urgent work is not an SOS feature — it is removing the false security, making the public calculator genuinely usable by disabled users, and establishing a reproducible, reviewed engineering baseline. With those foundations, the proposed feature set is achievable by extending the existing Android/Compose application.

**Recommendation:** approve **WP1 (foundation, build reproducibility, accessibility design system and security architecture)**, then WP2 (calculator hardening). Do not begin WP3+ until the §15 decisions are made.

---

# Appendix A — Source inventory and reproducibility hashes

Hashes are SHA-256 prefixes of the audited source files (first 16 hex characters), for change detection. Binary image line counts are omitted.

| File | Lines | SHA-256 prefix |
|---|---:|---|
| `app/src/main/AndroidManifest.xml` | 22 | `32810bea2584102d` |
| `engine/ExpressionParser.kt` | 426 | `2b121df8bccff05b` |
| `MainActivity.kt` | 37 | `b0476ed6b2e859b5` |
| `security/TriggerEvaluator.kt` | 43 | `8756d654f5ea2311` |
| `ui/CalculatorScreen.kt` | 129 | `cf457a90a0f7671a` |
| `ui/CalculatorUiState.kt` | 28 | `cca0e35ac13d8c50` |
| `ui/CalculatorViewModel.kt` | 294 | `743493c97c9792c3` |
| `ui/components/AppearanceSettingsDialog.kt` | 89 | `8bc27617db1c7bdc` |
| `ui/components/DisplaySection.kt` | 247 | `a68f1fa9f86d4b72` |
| `ui/components/HistoryOverlay.kt` | 163 | `2b8b535bf0c82e6f` |
| `ui/components/KeypadSection.kt` | 415 | `5f73bc9c18a56b5d` |
| `ui/HistoryPersistenceManager.kt` | 50 | `58758f0857ca36f4` |
| `ui/theme/Color.kt` | 69 | `353e5787e9dd8ab5` |
| `ui/theme/Theme.kt` | 64 | `7c633497dd371b7a` |
| `ui/theme/ThemePreferenceManager.kt` | 37 | `cd327786c929ddbd` |
| `ui/theme/Type.kt` | 41 | `aa742c0e19f7bbab` |
| `res/values/strings.xml` | 3 | `b4b112c6c679b563` |
| `res/values/themes.xml` | 7 | `2ada60da9c1d15fb` |
| `src/test/.../ExpressionParserTest.kt` | 101 | `d92b906fd2ce98a0` |

Totals: **15 main Kotlin files, 1 test Kotlin file, 2,233 Kotlin lines, 32 XML lines**. No git commit exists, so hashes are the only snapshot identifier.

# Appendix B — Commands run (summary)

```bash
# Environment
export JAVA_HOME=/home/zayne/.jdks/jbr-21.0.11
export ANDROID_HOME=/home/zayne/Android/Sdk

# Repo inspection
find /home/zayne/Shadownet -maxdepth 4 ...
git status                       # not a git repository
./gradlew --version              # Gradle 8.7

# Failed checked-in build
./gradlew testDebugUnitTest      # minimum Gradle 8.9 required

# Failed deploy path (user-run)
./deploy_to_device.sh            # Gradle 9.7.1; processDebugResources FAILED

# Isolated verification copy
tar --exclude=build --exclude=.gradle ... -cf - . | tar -xf - -C /tmp/opencode/shadownet-verify
printf 'sdk.dir=<local SDK>\n' > /tmp/opencode/shadownet-verify/local.properties
cd /tmp/opencode/shadownet-verify
<gradle-8.13>/bin/gradle testDebugUnitTest assembleDebug --offline --no-daemon
# BUILD SUCCESSFUL in 9m 42s; 11 tests, 0 failures

# Direct JUnit
java -cp ... org.junit.runner.JUnitCore com.shadownet.calculator.ExpressionParserTest
# OK (11 tests)

# Lint attempt
<gradle-8.13>/bin/gradle lintDebug --offline --no-daemon
# FAILED: uncached ui-test-junit4/collection-jvm for offline mode

# APK inspection
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk

# On-device (read-only inspection)
adb shell am start -n com.shadownet.calculator/.MainActivity
adb shell uiautomator dump --compressed /sdcard/shadownet-ui.xml
adb exec-out screencap -p > /tmp/opencode/device.png
adb shell settings get system font_scale
adb shell settings get global debug.force_rtl
```

No source file was edited, no code was refactored, no alert was sent, no real survivor data was collected, and no commit/push was performed. The only authored repository change is this report; verification runs refreshed generated build outputs under `app/build/`.

# Appendix C — Severity definitions and limitations

- **Verified** findings are reproducible from the evidence cited. On-device findings are specific to the test device/configuration; layout and spoken behaviour may differ on other devices.
- **Assumptions** are explicitly marked; they are not evidence.
- **Recommendations** are design proposals, not implemented changes.
- No claim is made that the app can guarantee concealment, prevent determined physical access, or protect against a compromised OS, root, keylogger, malicious accessibility service or coerced disclosure.
- The redacted credential is intentionally not reproduced. It remains present in the source and must be removed in WP3.
- The audit did not enable TalkBack, Switch Access, or change device settings; those tests remain required.
- The isolated build used Gradle 8.13 with cached dependencies; a clean online build may surface additional dependency/licence issues.
- Lint did not run; static analysis findings are therefore manual, not exhaustive.

---

**End of WP0 audit.** Await engineering approval before WP1. Do not implement emergency functionality, commit changes or begin WP1 on the basis of this report alone.

