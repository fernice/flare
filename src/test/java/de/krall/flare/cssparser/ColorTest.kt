package de.krall.flare.cssparser

import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ColorParseTest {

    @Test
    fun keywordWhite() {
        parse("white",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun keywordCurrentColor() {
        parse("currentcolor",
                expectCurrentColor())
    }

    @Test
    fun hashWhite8() {
        parse("#FFFFFFFF",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun hashWhite6() {
        parse("#FFFFFF",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun hashWhite4() {
        parse("#FFFF",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun hashWhite3() {
        parse("#FFF",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun hashArbitrary8() {
        parse("#78072c82",
                expectRGBA(120, 7, 44, 130))
    }

    @Test
    fun hashArbitrary6() {
        parse("#1aa251",
                expectRGBA(26, 162, 81))
    }

    @Test
    fun hashArbitrary4() {
        parse("#AaAa",
                expectRGBA(170, 170, 170, 170))
    }

    @Test
    fun hashArbitrary3() {
        parse("#abc",
                expectRGBA(170, 187, 204))
    }

    @Test
    fun rgbCommasNumbersWhite() {
        parse("rgb(255, 255, 255)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbCommasPercentagesWhite() {
        parse("rgb(100%, 100%, 100%)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbNumbersWhite() {
        parse("rgb(255 255 255)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbPercentagesWhite() {
        parse("rgb(100% 100% 100%)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbaCommasNumbersNumberWhite() {
        parse("rgba(255, 255, 255, 1)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbaCommasNumbersPercentageWhite() {
        parse("rgba(255, 255, 255, 100%)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbaCommasPercentagesNumberWhite() {
        parse("rgba(100%, 100%, 100%, 1)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun rgbaCommasPercentagesPercentageWhite() {
        parse("rgba(100%, 100%, 100%, 100%)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun rgbaNumbersNumberWhite() {
        parse("rgba(255 255 255 / 1)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbaNumbersPercentageWhite() {
        parse("rgba(255 255 255/100%)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun rgbaPercentagesNumberWhite() {
        parse("rgba(100% 100% 100%/ 1)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun rgbaPercentagesPercentageWhite() {
        parse("rgba(100% 100% 100% /100%)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun hslCommasWhite() {
        parse("hsl(0, 0%, 100%)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun hslWhite() {
        parse("hsl(0 0% 100%)",
                expectRGBA(255, 255, 255))
    }

    @Test
    fun hslaCommasNumberWhite() {
        parse("hsla(0, 0%, 100%, 1)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun hslaCommasPercentageWhite() {
        parse("hsla(0, 0%, 100%, 100%)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun hslaNumberWhite() {
        parse("hsla(0 0% 100%/ 1)",
                expectRGBA(255, 255, 255, 255))
    }

    @Test
    fun hslaPercentageWhite() {
        parse("hsla(0 0% 100% /100%)",
                expectRGBA(255, 255, 255, 255))
    }

    private fun parse(text: String, assert: (Color) -> Unit) {
        val input = Parser(ParserInput(text))

        val result = Color.parse(input)

        val color = when (result) {
            is Ok -> result.value
            is Err -> fail("parsing failed: ${result.value}")
        }

        assert(color)
    }

    private fun expectRGBA(red: Int, green: Int, blue: Int): (Color) -> Unit {
        return expectRGBA(red, green, blue, 255)
    }

    private fun expectRGBA(red: Int, green: Int, blue: Int, alpha: Int): (Color) -> Unit {
        return {
            assertTrue(it is Color.RGBA)

            val color = it as Color.RGBA
            val rgba = color.rgba

            assertEquals(red, rgba.red)
            assertEquals(green, rgba.green)
            assertEquals(blue, rgba.blue)
            assertEquals(alpha, rgba.alpha)
        }
    }

    private fun expectCurrentColor(): (Color) -> Unit {
        return {
            assertTrue(it is Color.CurrentColor)
        }
    }
}