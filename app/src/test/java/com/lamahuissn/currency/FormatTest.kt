package com.lamahuissn.currency

import com.lamahuissn.currency.data.CurrencyCatalog
import com.lamahuissn.currency.data.badgeText
import com.lamahuissn.currency.data.formatValue
import com.lamahuissn.currency.data.percentChange
import com.lamahuissn.currency.data.groupDigits
import com.lamahuissn.currency.data.orderRows
import com.lamahuissn.currency.data.sectionStartIndices
import com.lamahuissn.currency.data.togglePin
import com.lamahuissn.currency.data.visibleTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FormatTest {

    @Test
    fun `large figures carry two decimals and grouping`() {
        assertEquals("89,550.64", formatValue(89550.64))
        assertEquals("13,002.00", formatValue(13002.0))
        assertEquals("1,345.15", formatValue(1345.15))
    }

    @Test
    fun `figures under one hundred carry three decimals`() {
        assertEquals("53.899", formatValue(53.899))
        assertEquals("0.742", formatValue(0.742))
        assertEquals("0.89", formatValue(0.89))
    }

    @Test
    fun `a coin worth less than a cent still shows a figure`() {
        val text = formatValue(0.0000131)
        assertTrue(text, text.startsWith("0.0000"))
    }

    @Test
    fun `badges lead with the currency's own symbol`() {
        assertEquals("$1.00", badgeText("USD", 1.0))
        assertEquals("£S 13,002.00", badgeText("SYP", 13002.0))
        assertEquals("₹94.469", badgeText("INR", 94.469))
        assertEquals("kr 6.65", badgeText("DKK", 6.65))
    }

    @Test
    fun `percent change matches the pair in the screenshot`() {
        val change = percentChange(1.1523, 1.1223)
        assertTrue("was $change", abs(change - (-2.6035)) < 0.01)
    }

    @Test
    fun `catalogue is complete and unique`() {
        val codes = CurrencyCatalog.fiat.map { it.code }
        assertEquals("duplicate codes", codes.size, codes.distinct().size)
        assertTrue("too few currencies: ${codes.size}", codes.size >= 150)
        assertTrue(codes.all { it.length == 3 })
        assertTrue(CurrencyCatalog.fiat.all { it.name.isNotBlank() })
        assertTrue(CurrencyCatalog.fiat.all { it.flag.isNotEmpty() })
    }

    @Test
    fun `the currencies the owner watches are present`() {
        listOf("USD", "EUR", "GBP", "LBP", "SYP", "TRY", "EGP", "INR", "SAR", "AED", "BTC").forEach { code ->
            assertNotNull(code, CurrencyCatalog.all.firstOrNull { it.code == code })
        }
        assertEquals("Lebanese Pound", CurrencyCatalog.of("LBP").name)
        assertEquals("£S", CurrencyCatalog.of("SYP").symbol)
    }

    @Test
    fun `the base currency is never also a target row`() {
        // The bug the owner photographed: base GBP, and GBP also in the watch list.
        assertEquals(listOf("USD", "LBP"), visibleTargets("GBP", listOf("USD", "GBP", "LBP")))
        assertEquals(listOf("LBP"), visibleTargets("USD", listOf("LBP", "USD", "LBP")))
        assertEquals(emptyList<String>(), visibleTargets("EUR", listOf("EUR")))
        // The stored list is not rewritten, so another base restores the row.
        val stored = listOf("USD", "GBP")
        assertEquals(listOf("USD"), visibleTargets("GBP", stored))
        assertEquals(listOf("USD", "GBP"), visibleTargets("EUR", stored))
    }

    @Test
    fun `pinned rows come first and keep their own order`() {
        val targets = listOf("LBP", "EGP", "TRY", "EUR")
        assertEquals(listOf("TRY", "LBP", "EGP", "EUR"), orderRows("USD", targets, listOf("TRY", "LBP")))
        // A pin on the base, or on a currency no longer watched, is ignored.
        assertEquals(listOf("LBP", "EGP"), orderRows("USD", listOf("LBP", "EGP"), listOf("USD", "BTC")))
        assertEquals(listOf("LBP", "EGP"), orderRows("USD", listOf("LBP", "EGP"), emptyList()))
    }

    @Test
    fun `pinning is a toggle`() {
        assertEquals(listOf("LBP"), togglePin(emptyList(), "LBP"))
        assertEquals(emptyList<String>(), togglePin(listOf("LBP"), "LBP"))
    }

    @Test
    fun `the scrubber's letter indices match the list order`() {
        // Nothing above the alphabet: A's header is the first item.
        assertEquals(
            mapOf('A' to 0, 'B' to 5, 'C' to 8),
            sectionStartIndices(listOf('A', 'B', 'C'), listOf(4, 2, 1), leadingItems = 0),
        )
        // A "My Location" block (2 items) plus a "Selected" block (header + 2 rows) = 5 items first.
        val withLeading = sectionStartIndices(listOf('A', 'B'), listOf(3, 4), leadingItems = 5)
        assertEquals(5, withLeading['A'])
        assertEquals(5 + 1 + 3, withLeading['B'])
        // A short rows list must not throw; a letter with no entry still gets its header slot.
        assertEquals(
            mapOf('A' to 0, 'B' to 3, 'C' to 4),
            sectionStartIndices(listOf('A', 'B', 'C'), listOf(2), leadingItems = 0),
        )
    }

    @Test
    fun `the amount line shows what was typed`() {
        assertEquals("100", groupDigits("100"))
        assertEquals("1,000", groupDigits("1000"))
        assertEquals("12.5", groupDigits("12.5"))
        assertEquals("1,000,000.75", groupDigits("1000000.75"))
        assertEquals("1.", groupDigits("1."))
        assertEquals("0.5", groupDigits("0.5"))
        assertEquals("", groupDigits(""))
    }

    @Test
    fun `search matches name, code, country and region`() {
        val lebanese = CurrencyCatalog.of("LBP")
        assertTrue(lebanese.matches("leban"))
        assertTrue(lebanese.matches("lbp"))
        assertTrue(lebanese.matches("asia"))
        assertTrue(!lebanese.matches("zimbabwe"))
    }
}
