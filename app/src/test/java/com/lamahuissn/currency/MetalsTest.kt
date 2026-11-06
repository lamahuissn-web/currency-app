package com.lamahuissn.currency

import com.lamahuissn.currency.data.Metals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gold and silver arithmetic. A jeweller's gram, a karat and the Lebanese gold pound are all derived
 * from one ounce price, so the derivation is what these tests pin — the feed only supplies the ounce.
 */
class MetalsTest {

    // The bodies api.gold-api.com actually returned when this was written.
    private val goldBody = """{"currency":"USD","currencySymbol":"$","exchangeRate":1.0,"name":"Gold","price":4037.800049,"symbol":"XAU","updatedAt":"2026-11-06T14:09:35Z"}"""
    private val silverBody = """{"currency":"USD","currencySymbol":"$","exchangeRate":1.0,"name":"Silver","price":51.616001,"symbol":"XAG","updatedAt":"2026-11-06T14:09:34Z"}"""

    @Test
    fun `the ounce price is read out of the feed`() {
        assertEquals(4037.800049, Metals.parseOunceUsd(goldBody)!!, 0.0001)
        assertEquals(51.616001, Metals.parseOunceUsd(silverBody)!!, 0.0001)
    }

    @Test
    fun `a body without a usable price is refused`() {
        assertNull(Metals.parseOunceUsd("{}"))
        assertNull(Metals.parseOunceUsd("""{"price":0}"""))
        assertNull(Metals.parseOunceUsd("""{"price":-12}"""))
        assertNull(Metals.parseOunceUsd("not json"))
        assertNull(Metals.parseOunceUsd(""))
    }

    @Test
    fun `the table stores the ounce price the way it stores any currency`() {
        val perUsd = Metals.perUsd(4037.800049)
        assertEquals(0.0002476596, perUsd, 1e-9)
        assertEquals(4037.800049, Metals.ounceUsd(perUsd), 0.0001)
        assertEquals("a zero price never becomes a divide by zero", 0.0, Metals.perUsd(0.0), 0.0)
        assertEquals(0.0, Metals.ounceUsd(0.0), 0.0)
    }

    @Test
    fun `a gram is an ounce over 31 point 1034768`() {
        val ounce = 4037.800049
        assertEquals(129.82, Metals.gramUsd(ounce, 24), 0.01)
        assertEquals(119.00, Metals.gramUsd(ounce, 22), 0.01)
        assertEquals(113.59, Metals.gramUsd(ounce, 21), 0.01)
        assertEquals(97.36, Metals.gramUsd(ounce, 18), 0.01)
    }

    @Test
    fun `the karat scales the price proportionally`() {
        val ounce = 4000.0
        val fine = Metals.gramUsd(ounce, 24)
        assertEquals(fine * 18.0 / 24.0, Metals.gramUsd(ounce, 18), 1e-9)
        assertEquals(fine * 21.0 / 24.0, Metals.gramUsd(ounce, 21), 1e-9)
        assertEquals("a karat above 24 is clamped, not overpriced", fine, Metals.gramUsd(ounce, 30), 1e-9)
        assertEquals("a silly karat still yields a price, never a crash", fine / 24.0, Metals.gramUsd(ounce, 1), 1e-9)
    }

    @Test
    fun `the lebanese gold pound is 7 point 32 grams of 21 karat`() {
        val ounce = 4037.800049
        assertEquals(Metals.gramUsd(ounce, 21) * 7.32, Metals.poundUsd(ounce), 1e-9)
        assertEquals(831.5, Metals.poundUsd(ounce), 0.5)
    }

    @Test
    fun `silver is priced per gram of fine metal`() {
        assertEquals(1.66, Metals.gramUsd(51.616001, 24), 0.01)
    }

    @Test
    fun `the convert row shows a price per ounce, not a fraction of one`() {
        assertEquals("$4,037.80 / oz", Metals.rowBadge(Metals.perUsd(4037.800049)))
        assertEquals("$51.62 / oz", Metals.rowBadge(Metals.perUsd(51.616001)))
        assertEquals("—", Metals.rowBadge(0.0))
    }

    @Test
    fun `only the metals with a feed are treated as metals`() {
        assertTrue(Metals.isMetal("XAU"))
        assertTrue(Metals.isMetal("XAG"))
        assertFalse("platinum has no free feed in this build", Metals.isMetal("XPT"))
        assertFalse(Metals.isMetal("USD"))
        assertEquals(listOf("XAU", "XAG"), Metals.codes)
    }
}
