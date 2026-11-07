package com.lamahuissn.currency

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import com.lamahuissn.currency.data.CountCodec
import com.lamahuissn.currency.data.CountState
import com.lamahuissn.currency.data.Metals
import com.lamahuissn.currency.ui.ChartScreen
import com.lamahuissn.currency.ui.ChartUi
import com.lamahuissn.currency.ui.ConvertScreen
import com.lamahuissn.currency.ui.ConvertUi
import com.lamahuissn.currency.ui.CountScreen
import com.lamahuissn.currency.ui.CurrencyTheme
import com.lamahuissn.currency.ui.KeypadSheet
import com.lamahuissn.currency.ui.MetalsSheet
import com.lamahuissn.currency.ui.NavTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * The screens, rendered on the JVM and saved as PNGs.
 *
 * This exists because the app has no emulator in its build environment: every visual question used to
 * end with "install it and look". Robolectric renders the same composables the phone renders, so a
 * layout change shows up as a picture in CI — and a screenshot that changes unexpectedly is a
 * conversation, not a surprise release.
 *
 * The screens are fed fixed state, so these tests never touch the network.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val output: File
        get() = File(System.getProperty("screens.dir") ?: "build/outputs/screens").apply { mkdirs() }

    private fun shoot(name: String) {
        // Let the entrance animations settle before the frame is taken.
        compose.mainClock.advanceTimeBy(1_500)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(output, "$name.png")).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    /** A rate table in the app's own shape: units of that currency per 1 USD. */
    private val rates = mapOf(
        "USD" to 1.0,
        "LBP" to 89_550.64,
        "EGP" to 53.899,
        "TRY" to 50.063,
        "EUR" to 0.8905,
        "INR" to 94.469,
        "GBP" to 0.742,
        "SYP" to 13_002.0,
        "XAU" to Metals.perUsd(4037.80),
        "XAG" to Metals.perUsd(51.62),
    )

    private fun convertState() = ConvertUi(
        base = "USD",
        amount = 100.0,
        targets = listOf("LBP", "EGP", "TRY", "EUR", "INR", "GBP", "SYP", "XAU", "XAG"),
        pinned = listOf("LBP"),
        soundOn = true,
        usdTo = rates,
        updatedAt = 1_760_000_000_000,
        metalsAt = 1_760_000_000_000,
    )

    @Test
    fun convert() {
        compose.setContent {
            CurrencyTheme {
                ConvertScreen(
                    state = convertState(),
                    onRowTap = {},
                    onAdd = {},
                    onRemove = {},
                    onPin = {},
                    onMove = { _, _ -> },
                    onSort = {},
                    onReset = {},
                    onRefresh = {},
                    onToggleSound = {},
                    onSelectTab = {},
                )
            }
        }
        shoot("01-convert")
    }

    @Test
    fun count() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seeded = CountState(
            bag = listOf("USD", "GBP", "LBP"),
            counts = mapOf(
                "USD" to mapOf(100L to 7, 20L to 4, 1L to 1),
                "GBP" to mapOf(50L to 3),
                "LBP" to mapOf(100_000L to 40),
            ),
            expected = 1_000.0,
            savedFingerprint = "",
        )
        context.getSharedPreferences("currency_count", Context.MODE_PRIVATE).edit()
            .putString("bag", seeded.bag.joinToString(","))
            .putString("counts", CountCodec.encodeStacks(seeded.counts))
            .putFloat("expected", seeded.expected.toFloat())
            .apply()

        compose.setContent {
            CurrencyTheme {
                CountScreen(
                    rates = rates,
                    ratesLive = true,
                    ratesUpdatedAt = 1_760_000_000_000,
                    onSelectTab = { _: NavTab -> },
                )
            }
        }
        shoot("02-count")
    }

    @Test
    fun goldSheet() {
        compose.setContent {
            CurrencyTheme {
                MetalsSheet(
                    code = "XAU",
                    rates = rates,
                    updatedAt = 1_760_000_000_000,
                    metalsAt = System.currentTimeMillis(),
                    onDismiss = {},
                )
            }
        }
        shoot("03-gold")
    }

    @Test
    fun keypad() {
        compose.setContent {
            CurrencyTheme {
                KeypadSheet(
                    code = "USD",
                    initialAmount = 4_500.0,
                    onDismiss = {},
                    onConvert = {},
                )
            }
        }
        shoot("04-keypad")
    }

    @Test
    fun chart() {
        val points = (0 until 90).map { day ->
            com.lamahuissn.currency.data.Point(
                at = 1_752_000_000_000L + day * 86_400_000L,
                value = 1.08 + kotlin.math.sin(day / 9.0) * 0.02,
            )
        }
        compose.setContent {
            CurrencyTheme {
                ChartScreen(
                    state = ChartUi(base = "EUR", quote = "USD", period = "3M", points = points),
                    onPeriod = {},
                    onSwap = {},
                    onPickBase = {},
                    onPickQuote = {},
                    onSelectTab = {},
                )
            }
        }
        shoot("05-chart")
    }
}
