# ZhynorCalculator

Jetpack Compose calculator for Android with a swipeable **Standard ⇄ Scientific** keypad
(portrait) and a combined layout in landscape — styled after the dark pill-key design.

## Run it
1. Open Android Studio (Ladybug 2024.2+ recommended) → **Open** → select this `ZhynorCalculator` folder.
2. Let Gradle sync (it downloads Gradle 8.9 automatically).
3. Pick a device/emulator (Android 8.0 / API 26+) → **Run ▶**.

## Features
- Swipe left/right between Standard and Scientific pages (landscape shows both)
- sin/cos/tan, sinh/cosh/tanh, 2nd → inverse functions, ln, lg, log₂, x², x³, xʸ, ʸ√x, √, x!, π, e, EE, Rand
- Rad/Deg toggle, memory (mc, m+, m−, mr), % (200+10% = 220), live result preview
- History sheet (tap an entry to reuse it), haptic key feedback
- Adaptive + themed (monochrome) launcher icon

## Structure
```
app/src/main/java/com/zhynorcalculator/app/
  MainActivity.kt
  logic/   Evaluator.kt  ResultFormatter.kt  CalcState.kt  CalculatorViewModel.kt
  ui/      Theme.kt  Keys.kt  CalculatorScreen.kt
app/src/main/res/  launcher icon (drawable/ic_launcher_*.xml, mipmap-anydpi/)
logo/              logo.svg (+ logo.png for Play Store)
```

## Customise
- Colours: `ui/Theme.kt` → `CalcColors`
- Key layout / labels: `ui/Keys.kt`
- App name & package: `strings.xml`, `app/build.gradle.kts` (`applicationId`)
- The unit-converter and currency icons are placeholders (show "Coming soon").

## Publish to Google Play
1. Create a keystore: Android Studio → Build → Generate Signed App Bundle → Create new... (keep it safe!).
2. Copy `keystore.properties.example` to `keystore.properties` and fill it in (it is git-ignored).
3. Build: `./gradlew bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`
4. Upload the .aab in Play Console. Use the files in `store/` (icon, feature graphic, listing text, privacy policy).
5. Host `store/PRIVACY_POLICY.md` (e.g. GitHub Pages or the raw GitHub URL) and paste the link into Play Console.
