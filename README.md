# Currency

A native Android app for the money in your pocket: convert currencies, read the charts, and count
cash by banknote — including gold and silver by the ounce, gram and karat.

Kotlin + Jetpack Compose, dark-first, no web view, no API keys, no account.

## What it does

**Convert** — one base amount against a watch list of 166 fiat codes and 16 coins. Swipe a row right
to pin it, left to delete it; drag to reorder; pull down to refresh. Tapping a row opens the amount
pad for that currency.

**Chart** — a hand-drawn daily line for a pair, from 1D to 2Y, with the period and the pair one tap
away. A pair the ECB does not publish says so instead of drawing a guess.

**Count** — a cash counter for a bag of currencies: each currency has its real banknote ladder, its
own `− n +` steppers, and a subtotal both in its own money and in USD. Set an **expected** amount and
the card answers `✓ Match`, `Short $49.00` or `Over $12.00` as you count. Counts persist, **Save**
writes a line into a history that restores the exact notes, and each currency can carry a **manual
rate** (the street rate, not the published one) with the sheet saying plainly when rates are cached.

**Gold and silver** — spot prices by the troy ounce from a keyless feed. A metal row shows its price,
not a converted fraction; tapping it opens the ounce, the gram at 24 / 22 / 21 / 18 karat, and the
7.32 g 21k gold pound. Metals refresh every 15 seconds while the app is on screen, and stop the moment
it is in your pocket.

**Arithmetic in the amount field** — `1500×3`, `200+350`, with `×` and `÷` binding before `+` and `−`,
a running total while you type, and a refusal to convert a half-typed or divide-by-zero entry.

## Data sources — all keyless

| What | Where |
| --- | --- |
| Fiat rates (166 codes) | `open.er-api.com/v6/latest/USD` |
| Historical series | `api.frankfurter.dev` (ECB reference rates, ~30 pairs) |
| Crypto spot + history | `api.coingecko.com` |
| Gold and silver spot | `api.gold-api.com/price/XAU` · `/XAG` |

Every rate is stored in one shape — **units of that currency per 1 USD** — and every figure in the app
is derived from it. When a value cannot be known the app says so rather than inventing one: an
unpublished chart pair, a currency with no rate, a metals feed that did not answer, rates older than
the last successful refresh. A stack of foreign cash is priced by **dividing** by that table, which is
what the `50,000 LBP` regression test exists to protect.

## Build locally

```bash
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk-21
export CURRENCY_KEYSTORE_PROPERTIES=~/.currency/keystore.properties   # not in the repo
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

`keystore.properties` lives **outside** the working tree and holds:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

A release build fails loudly when that file is missing, so an unsigned APK can never be delivered by
accident. The keystore is never committed: the signature is the only thing that lets an update install
over an existing copy of the app.

## Build on GitHub Actions

`.github/workflows/release-apk.yml` runs the tests, builds, verifies the signature and uploads the
APK — from the **Actions** tab, or by pushing a tag:

```bash
git tag v1.7.1 && git push origin v1.7.1
```

| Secret | Meaning |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias inside the keystore |
| `KEY_PASSWORD` | key password |

The artifact is `currency-release-apk`; for a tag it is attached to the release. Release builds are
deliberately **not** run on the development host — resource shrinking needs more memory than it has.

## Tests

`./gradlew :app:testDebugUnitTest` — 71 JVM tests: money formatting, list rules, gesture
disambiguation, the count arithmetic (mixed bags, karats, expected-vs-counted, the 50,000 LBP case),
storage round-trips, and parsers pinned to bodies captured live from each endpoint.

## Layout

```
app/src/main/java/com/lamahuissn/currency/
├── data/            catalogue, rate + metal clients, parsers, prefs, count maths, formatting, rules
├── ui/              Convert, Chart, Count, Add Currency, keypad, sheets, theme, components
└── MainActivity.kt  tab state, sheets, splash
app/src/test/        JVM tests
app/src/test/resources/fixtures/   live captures from the endpoints
design/              the HTML design demos the screens were built from
```

## License

No license file yet: the code is public to read, not yet licensed for reuse.
