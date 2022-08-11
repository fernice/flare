/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.flare.style.parser.ParseMode
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.parser.QuirksMode
import org.fernice.flare.url.Url
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class LengthParseTest {

    @Test
    fun lengthUnitPx() {
        val length = withInput("1px", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'px'") { absolute is AbsoluteLength.Px }

        val value = (absolute as AbsoluteLength.Px).value

        assertEquals(1f, value, "expected a value of '1'")
    }

    @Test
    fun lengthUnitIn() {
        val length = withInput("2in", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'in'") { absolute is AbsoluteLength.In }

        val value = (absolute as AbsoluteLength.In).value

        assertEquals(2f, value, "expected a value of '2'")
    }

    @Test
    fun lengthUnitCm() {
        val length = withInput("3cm", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'cm'") { absolute is AbsoluteLength.Cm }

        val value = (absolute as AbsoluteLength.Cm).value

        assertEquals(3f, value, "expected a value of '3'")
    }

    @Test
    fun lengthUnitMm() {
        val length = withInput("4mm", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'mm'") { absolute is AbsoluteLength.Mm }

        val value = (absolute as AbsoluteLength.Mm).value

        assertEquals(4f, value, "expected a value of '4'")
    }

    @Test
    fun lengthUnitQ() {
        val length = withInput("5q", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'q'") { absolute is AbsoluteLength.Q }

        val value = (absolute as AbsoluteLength.Q).value

        assertEquals(5f, value, "expected a value of '5'")
    }

    @Test
    fun lengthUnitPt() {
        val length = withInput("6pt", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'pt'") { absolute is AbsoluteLength.Pt }

        val value = (absolute as AbsoluteLength.Pt).value

        assertEquals(6f, value, "expected a value of '6'")
    }

    @Test
    fun lengthUnitPc() {
        val length = withInput("7pc", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected absolute length") { noCalc is NoCalcLength.Absolute }

        val absolute = (noCalc as NoCalcLength.Absolute).length

        assertTrue("expected unit 'pc'") { absolute is AbsoluteLength.Pc }

        val value = (absolute as AbsoluteLength.Pc).value

        assertEquals(7f, value, "expected a value of '7'")
    }

    @Test
    fun lengthUnitEm() {
        val length = withInput("8em", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected font relative length") { noCalc is NoCalcLength.FontRelative }

        val absolute = (noCalc as NoCalcLength.FontRelative).length

        assertTrue("expected unit 'em'") { absolute is FontRelativeLength.Em }

        val value = (absolute as FontRelativeLength.Em).value

        assertEquals(8f, value, "expected a value of '8'")
    }

    @Test
    fun lengthUnitEx() {
        val length = withInput("9ex", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected font relative length") { noCalc is NoCalcLength.FontRelative }

        val absolute = (noCalc as NoCalcLength.FontRelative).length

        assertTrue("expected unit 'ex'") { absolute is FontRelativeLength.Ex }

        val value = (absolute as FontRelativeLength.Ex).value

        assertEquals(9f, value, "expected a value of '9'")
    }

    @Test
    fun lengthUnitCh() {
        val length = withInput("10ch", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected font relative length") { noCalc is NoCalcLength.FontRelative }

        val absolute = (noCalc as NoCalcLength.FontRelative).length

        assertTrue("expected unit 'ch'") { absolute is FontRelativeLength.Ch }

        val value = (absolute as FontRelativeLength.Ch).value

        assertEquals(10f, value, "expected a value of '10'")
    }

    @Test
    fun lengthUnitRem() {
        val length = withInput("1.5rem", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected font relative length") { noCalc is NoCalcLength.FontRelative }

        val absolute = (noCalc as NoCalcLength.FontRelative).length

        assertTrue("expected unit 'rem'") { absolute is FontRelativeLength.Rem }

        val value = (absolute as FontRelativeLength.Rem).value

        assertEquals(1.5f, value, "expected a value of '1.5'")
    }

    @Test
    fun lengthUnitVmin() {
        val length = withInput("2.25vmin", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected viewport percentage length") { noCalc is NoCalcLength.ViewportPercentage }

        val absolute = (noCalc as NoCalcLength.ViewportPercentage).length

        assertTrue("expected unit 'vmin'") { absolute is ViewportPercentageLength.Vmin }

        val value = (absolute as ViewportPercentageLength.Vmin).value

        assertEquals(2.25f, value, "expected a value of '2.25'")
    }

    @Test
    fun lengthUnitVmax() {
        val length = withInput("3.73vmax", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected viewport percentage length") { noCalc is NoCalcLength.ViewportPercentage }

        val absolute = (noCalc as NoCalcLength.ViewportPercentage).length

        assertTrue("expected unit 'vmax'") { absolute is ViewportPercentageLength.Vmax }

        val value = (absolute as ViewportPercentageLength.Vmax).value

        assertEquals(3.73f, value, "expected a value of '3.73'")
    }

    @Test
    fun lengthUnitVw() {
        val length = withInput("0.4vw", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected viewport percentage length") { noCalc is NoCalcLength.ViewportPercentage }

        val absolute = (noCalc as NoCalcLength.ViewportPercentage).length

        assertTrue("expected unit 'vw'") { absolute is ViewportPercentageLength.Vw }

        val value = (absolute as ViewportPercentageLength.Vw).value

        assertEquals(0.4f, value, "expected a value of '0.4'")
    }

    @Test
    fun lengthUnitVh() {
        val length = withInput("0.1e+2vh", Length.Companion::parse)

        assertTrue("expected no calc length") { length is Length.NoCalc }

        val noCalc = (length as Length.NoCalc).length

        assertTrue("expected viewport percentage length") { noCalc is NoCalcLength.ViewportPercentage }

        val absolute = (noCalc as NoCalcLength.ViewportPercentage).length

        assertTrue("expected unit 'vh'") { absolute is ViewportPercentageLength.Vh }

        val value = (absolute as ViewportPercentageLength.Vh).value

        assertEquals(0.1e+2f, value, "expected a value of '0.1e+2'")
    }

    private inline fun <T> withInput(text: String, parse: (ParserContext, Parser) -> Result<T, ParseError>): T {
        val input = Parser.new(ParserInput(text))
        val context = ParserContext(ParseMode.Default, QuirksMode.NO_QUIRKS, Url(""))

        val result = parse(context, input)

        return when (result) {
            is Ok -> result.value
            is Err -> fail("parsing failed: ${result.value}")
        }
    }
}
