# Currency

A native Android currency converter — Kotlin + Jetpack Compose, dark‑first, no web view and no
API keys.

- **Convert** — one base amount, a watch list of currencies, live rates.
- **Chart** — EUR/USD style trends with 1D → 2Y ranges.
- **Add Currency** — 160 fiat codes, 16 coins, search, alphabetical scrubber.
- **Pin / swipe** — swipe right to pin a row to the top, left to delete it.

## Data sources (no key required)

| What | Where |
| --- | --- |
| Fiat rates (166 codes) | `open.er-api.com/v6/latest/USD` |
| Historical series | `api.frankfurter.dev` (ECB reference rates, ~30 pairs) |
| Crypto spot + history | `api.coingecko.com` |

A pair the ECB does not publish shows "No published series" rather than a drawn guess. Metals are
listed but not priced: that feed is paid.

## Build locally

```bash
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export CURRENCY_KEYSTORE_PROPERTIES=~/.currency/keystore.properties   # not in the repo
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

`keystore.properties` (kept **outside** the working tree) holds:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

A release build fails loudly when that file is missing, so an unsigned APK can never be delivered by
accident.

## Build on GitHub Actions

`.github/workflows/release-apk.yml` builds, tests, verifies and uploads the signed APK — run it from
the **Actions** tab (`Run workflow`) or push a tag:

```bash
git tag v1.0.3 && git push origin v1.0.3
```

Required repository secrets (already set for this repo):

| Secret | Meaning |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias inside the keystore |
| `KEY_PASSWORD` | key password |

The artifact is uploaded as `currency-release-apk` and, for a tag, attached to the GitHub release.
The keystore stays out of the repository: the workflow decodes it into `$RUNNER_TEMP` for the build.

> Keep the same signing key. Android refuses to install an update signed by a different certificate,
> and every installed phone would have to uninstall first.

## Layout

```
app/src/main/java/com/lamahuissn/currency/
├── data/            catalogue, rates client + parsers, prefs, formatting, list rules
├── ui/              Convert, Chart, Add Currency, keypad, pair picker, theme, components
└── MainActivity.kt  screen state + transitions
app/src/test/        JVM tests (formatting, rules, parsers pinned to captured bodies)
app/src/test/resources/fixtures/  live captures from the three endpoints
```
