package com.lamahuissn.currency

import com.lamahuissn.currency.data.Expr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The amount sheet's arithmetic. A wrong total here is a wrong conversion everywhere else, so every
 * rule the pad promises is pinned: precedence, a half-typed sum, and a refused division by zero.
 */
class ExprTest {

    @Test
    fun `a plain amount is itself`() {
        assertEquals(100.0, Expr.evaluate("100")!!, 0.0001)
        assertEquals(1.5, Expr.evaluate("1.5")!!, 0.0001)
        assertEquals(0.0, Expr.evaluate("0")!!, 0.0001)
    }

    @Test
    fun `multiplication and division bind tighter than addition`() {
        assertEquals(14.0, Expr.evaluate("2+3×4")!!, 0.0001)
        assertEquals(4500.0, Expr.evaluate("1500×3")!!, 0.0001)
        assertEquals(25.0, Expr.evaluate("100÷4")!!, 0.0001)
        assertEquals(750.0, Expr.evaluate("1000−250")!!, 0.0001)
        assertEquals(20.0, Expr.evaluate("100÷5×1")!!, 0.0001)
    }

    @Test
    fun `a subtraction chain runs left to right`() {
        assertEquals(2.0, Expr.evaluate("5−2−1")!!, 0.0001)
        assertEquals(9.0, Expr.evaluate("10−3+2")!!, 0.0001)
    }

    @Test
    fun `a half typed sum still shows its running total`() {
        assertEquals(1500.0, Expr.evaluate("1500+")!!, 0.0001)
        assertEquals(1503.0, Expr.evaluate("1500+3×")!!, 0.0001)
    }

    @Test
    fun `cursive times and hyphen mix with the pad glyphs`() {
        assertEquals(300.0, Expr.evaluate("100*3")!!, 0.0001)
        assertEquals(50.0, Expr.evaluate("100/2")!!, 0.0001)
        assertEquals(1.0, Expr.evaluate("3-2")!!, 0.0001)
        assertEquals('÷', Expr.normalize('/'))
        assertEquals('×', Expr.normalize('*'))
    }

    @Test
    fun `nothing to compute reads as nothing, never as zero`() {
        assertNull(Expr.evaluate(""))
        assertNull(Expr.evaluate("   "))
        assertNull(Expr.evaluate("+"))
        assertNull(Expr.evaluate("×3"))
        assertNull(Expr.evaluate("1.2.3"))
        assertNull(Expr.evaluate("abc"))
    }

    @Test
    fun `division by zero is refused instead of becoming infinity`() {
        assertNull(Expr.evaluate("10÷0"))
        assertNull(Expr.evaluate("10/0"))
        assertEquals(0.0, Expr.evaluate("0÷10")!!, 0.0001)
    }

    @Test
    fun `cents survive the arithmetic`() {
        assertEquals(0.3, Expr.evaluate("0.1+0.2")!!, 0.0001)
        assertEquals(3.0, Expr.evaluate("1.5×2")!!, 0.0001)
        assertEquals("0.3", Expr.resultText(Expr.evaluate("0.1+0.2")!!))
    }

    @Test
    fun `the expression reads with its thousands grouped`() {
        assertEquals("1,500 × 3", Expr.display("1500×3"))
        assertEquals("1,500,000 ÷ 12", Expr.display("1500000÷12"))
        assertEquals("100", Expr.display("100"))
        assertEquals("1,234.50", Expr.display("1234.50"))
    }

    @Test
    fun `a result drops trailing zeros but keeps real cents`() {
        assertEquals("4,500", Expr.resultText(4500.0))
        assertEquals("2.5", Expr.resultText(2.5))
        assertEquals("1,234.57", Expr.resultText(1234.567))
        assertEquals("0", Expr.resultText(0.0))
    }

    @Test
    fun `the pad knows when it is showing a sum rather than an amount`() {
        assertEquals(0, Expr.operatorCount("100"))
        assertEquals(1, Expr.operatorCount("100×3"))
        assertEquals(2, Expr.operatorCount("100×3+50"))
    }
}
