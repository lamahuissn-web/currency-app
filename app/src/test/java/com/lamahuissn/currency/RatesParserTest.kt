package com.lamahuissn.currency

import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.RatesClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parsers are pinned against bodies captured from the live endpoints (see `resources/fixtures`),
 * not against shapes typed by hand: a fixture I invent only proves the parser agrees with me.
 *
 * Refresh a fixture by re-running the same URL and replacing the file — the assertions below name
 * the figures they expect so a silent change in the payload fails the build.
 */
class RatesParserTest {

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun `fiat table parses the live open er-api body`() {
        val table = RatesClient.parseFiat(fixture("erapi.json"))
        assertTrue("USD missing", table.containsKey("USD"))
        assertEquals(1.0, table.getValue("USD"), 0.0)
        assertTrue("too few currencies: ${table.size}", table.size >= 100)
        // The owner's own currency is quoted in the thousands per dollar.
        assertTrue("LBP missing", table.containsKey("LBP"))
        assertTrue(table.getValue("LBP") > 100.0)
        assertTrue("no negative or zero rates allowed", table.values.all { it > 0.0 })
    }

    @Test
    fun `crypto table is inverted into coin per dollar`() {
        val table = RatesClient.parseCrypto(fixture("coingecko.json"))
        assertTrue("BTC missing", table.containsKey("BTC"))
        // One dollar buys a tiny fraction of a bitcoin; the app divides, it never multiplies by price.
        assertTrue(table.getValue("BTC") < 0.01)
        assertTrue(table.values.all { it > 0.0 })
    }

    @Test
    fun `ecb history parses into an ordered daily series`() {
        val series = RatesClient.parseHistory(fixture("frankfurter.json"), "USD")
        assertTrue("series too short: ${series.size}", series.size >= 20)
        assertTrue("not ordered", series.zipWithNext().all { (a, b) -> a.at <= b.at })
        assertTrue("implausible EUR USD rate: ${series.first().value}",
            series.all { it.value in 0.5..2.5 })
    }

    @Test
    fun `coingecko market chart parses into an ordered series`() {
        val series = RatesClient.parseMarketChart(fixture("marketchart.json"))
        assertTrue("series too short: ${series.size}", series.size >= 5)
        assertTrue("not ordered", series.zipWithNext().all { (a, b) -> a.at <= b.at })
        assertTrue("BTC price implausible: ${series.first().value}",
            series.all { it.value in 100.0..1_000_000.0 })
    }

    @Test
    fun `a body for an unpublished pair yields an empty series, not an exception`() {
        val empty = RatesClient.parseHistory("""{"amount":1.0,"base":"EUR","rates":{}}""", "USD")
        assertTrue(empty.isEmpty())
        assertTrue(RatesClient.parseHistory("""{"message":"not found"}""", "USD").isEmpty())
    }

    @Test
    fun `every crypto row has a CoinGecko id`() {
        CurrencyCatalog.crypto.forEach { coin ->
            assertTrue("no id for ${coin.code}", CurrencyCatalog.cryptoIds.containsKey(coin.code))
        }
    }
}
