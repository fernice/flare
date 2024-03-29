/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.cssparser

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.std.mapErr
import org.fernice.std.trunc
import java.io.Writer
import kotlin.math.roundToInt

/**
 * Represents a 8 bit, int based RGBA color.
 */
data class RGBA(val red: Float, val green: Float, val blue: Float, val alpha: Float) : ToCss {

    override fun toCss(writer: Writer) {
        val hasAlpha = alpha != 1f

        writer.append(if (hasAlpha) "rgba" else "rgb")

        writer.append('(')
        writer.append("${red * 255}")
        writer.append(", ")
        writer.append("${green * 255}")
        writer.append(", ")
        writer.append("${blue * 255}")

        if (hasAlpha) {
            writer.append(", ")

            var roundedAlpha = (alpha * 100f).roundToInt() / 100f
            if (clampUnit(roundedAlpha) != alpha) {
                roundedAlpha = (alpha * 1000f).roundToInt() / 1000f
            }

            writer.append("$roundedAlpha")
        }

        writer.append(')')
    }
}

/**
 * Represents either a [RGBA] color or the keyword 'currentcolor'.
 */
sealed class Color {

    class RGBA(val rgba: org.fernice.flare.cssparser.RGBA) : Color()

    object CurrentColor : Color()

    companion object {

        /**
         * Parses the token stream [input] into a [Color] using the [DefaultColorComponentParser].
         */
        fun parse(input: Parser): Result<Color, ParseError> {
            return parse(input, DefaultColorComponentParser())
        }

        /**
         * Parses the token stream [input] into a [Color] using the specified [ColorComponentParser].
         */
        fun parse(input: Parser, parser: ColorComponentParser): Result<Color, ParseError> {
            val location = input.sourceLocation()
            val tokenResult = input.next()

            val token = when (tokenResult) {
                is Ok -> tokenResult.value
                is Err -> return tokenResult
            }

            return when (token) {
                is Token.IdHash -> {
                    parseHash(token.value)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }

                is Token.Hash -> {
                    parseHash(token.value)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }

                is Token.Identifier -> {
                    parseColorKeyword(token.name)
                        .mapErr { location.newUnexpectedTokenError(token) }
                }

                is Token.Function -> {
                    input.parseNestedBlock {
                        parseColorFunction(it, parser, token.name)
                    }
                }

                else -> {
                    Err(location.newUnexpectedTokenError(token))
                }
            }
        }
    }
}


/**
 * A default implementation of the [ColorComponentParser] providing a basic parser.
 */
class DefaultColorComponentParser : ColorComponentParser

/**
 * Represents either a [Number] or a [Percentage].
 */
sealed class NumberOrPercentage {

    abstract fun unitValue(): Float

    /**
     * Represents [NumberOrPercentage] as a Number. The [value] is an arbitrary number.
     */
    class Number(val value: Float) : NumberOrPercentage() {
        override fun unitValue(): Float {
            return value
        }
    }

    /**
     * Represents [NumberOrPercentage] as a Percentage. The [value] is depicted as a value from 0 to 1 corresponding
     * to 0% and 100% respectively. The value is unclamped and therefore might exceed 1.
     */
    class Percentage(val value: Float) : NumberOrPercentage() {
        override fun unitValue(): Float {
            return value
        }
    }
}

/**
 * Represents either an [Angle] or a [Number].
 */
sealed class AngleOrNumber {

    abstract fun degrees(): Float

    /**
     * Represents [AngleOrNumber] as an Angle. The [value] is the angle in degrees.
     */
    class Angle(val value: Float) : AngleOrNumber() {
        override fun degrees(): Float {
            return value
        }
    }

    /**
     * Represents [AngleOrNumber] as a Number. The [value] is an arbitrary number.
     */
    class Number(val value: Float) : AngleOrNumber() {
        override fun degrees(): Float {
            return value
        }
    }
}

/**
 * Parses the essential components of a color. Providing own implementation allows extending the possible
 * allowed value further.
 */
interface ColorComponentParser {

    fun parseNumberOrPercentage(input: Parser): Result<NumberOrPercentage, ParseError> {
        val location = input.sourceLocation()

        val token = when (val token = input.next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Number -> {
                Ok(NumberOrPercentage.Number(token.number.float()))
            }

            is Token.Percentage -> {
                Ok(NumberOrPercentage.Percentage(token.number.float()))
            }

            else -> {
                Err(location.newUnexpectedTokenError(token))
            }
        }
    }

    fun parseNumber(input: Parser): Result<Float, ParseError> {
        return input.expectNumber()
    }

    fun parsePercentage(input: Parser): Result<Float, ParseError> {
        return input.expectPercentage()
    }

    fun parseAngleOrNumber(input: Parser): Result<AngleOrNumber, ParseError> {
        val location = input.sourceLocation()

        val token = when (val token = input.next()) {
            is Ok -> token.value
            is Err -> return token
        }

        return when (token) {
            is Token.Number -> {
                Ok(AngleOrNumber.Number(token.number.float()))
            }

            is Token.Dimension -> {
                val degrees = when (token.unit.lowercase()) {
                    "deg" -> token.number.float()
                    "grad" -> token.number.float() * (360 / 400)
                    "rad" -> Math.toDegrees(token.number.value).toFloat()
                    "turn" -> token.number.float() * 360
                    else -> return Err(location.newUnexpectedTokenError(token))
                }

                Ok(AngleOrNumber.Angle(degrees))
            }

            else -> {
                Err(location.newUnexpectedTokenError(token))
            }
        }
    }
}

/**
 * Parses a color hash into a [RGBA] color.
 */
private fun parseHash(hash: String): Result<Color, Unit> {
    val chars = hash.toCharArray()

    try {
        return when (chars.size) {
            8 -> Ok(
                rgba(
                    fromHex(chars[0]) * 16 + fromHex(chars[1]),
                    fromHex(chars[2]) * 16 + fromHex(chars[3]),
                    fromHex(chars[4]) * 16 + fromHex(chars[5]),
                    fromHex(chars[6]) * 16 + fromHex(chars[7])
                )
            )

            6 -> Ok(
                rgb(
                    fromHex(chars[0]) * 16 + fromHex(chars[1]),
                    fromHex(chars[2]) * 16 + fromHex(chars[3]),
                    fromHex(chars[4]) * 16 + fromHex(chars[5])
                )
            )

            4 -> Ok(
                rgba(
                    fromHex(chars[0]) * 17,
                    fromHex(chars[1]) * 17,
                    fromHex(chars[2]) * 17,
                    fromHex(chars[3]) * 17
                )
            )

            3 -> Ok(
                rgb(
                    fromHex(chars[0]) * 17,
                    fromHex(chars[1]) * 17,
                    fromHex(chars[2]) * 17
                )
            )

            else -> Err()
        }
    } catch (e: NumberFormatException) {
        return Err()
    }
}

/**
 * Returns a [RGBA] color with the specified [red], [green] and [blue] values and a alpha value of 255.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun rgb(red: Int, green: Int, blue: Int): Color {
    return rgba(red, green, blue, 255)
}

/**
 * Returns a [RGBA] color with the specified [red], [green], [blue] and [alpha] values.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun rgba(red: Int, green: Int, blue: Int, alpha: Int): Color {
    return Color.RGBA(RGBA(red / 255f, green / 255f, blue / 255f, alpha / 255f))
}

/**
 * Returns the associated hex value of a char ranging from '0' to 'f'
 */
private fun fromHex(c: Char): Int {
    when (c) {
        '0' -> return 0
        '1' -> return 1
        '2' -> return 2
        '3' -> return 3
        '4' -> return 4
        '5' -> return 5
        '6' -> return 6
        '7' -> return 7
        '8' -> return 8
        '9' -> return 9
        'a', 'A' -> return 10
        'b', 'B' -> return 11
        'c', 'C' -> return 12
        'd', 'D' -> return 13
        'e', 'E' -> return 14
        'f', 'F' -> return 15
        else -> throw NumberFormatException("illegal char '$c'")
    }
}

/**
 * Parses a color keyword into a [Color].
 */
private fun parseColorKeyword(keyword: String): Result<Color, Unit> {
    val color = when (keyword.toLowerCase()) {
        "black" -> rgb(0, 0, 0)
        "silver" -> rgb(192, 192, 192)
        "gray" -> rgb(128, 128, 128)
        "white" -> rgb(255, 255, 255)
        "maroon" -> rgb(128, 0, 0)
        "red" -> rgb(255, 0, 0)
        "purple" -> rgb(128, 0, 128)
        "fuchsia" -> rgb(255, 0, 255)
        "green" -> rgb(0, 128, 0)
        "lime" -> rgb(0, 255, 0)
        "olive" -> rgb(128, 128, 0)
        "yellow" -> rgb(255, 255, 0)
        "navy" -> rgb(0, 0, 128)
        "blue" -> rgb(0, 0, 255)
        "teal" -> rgb(0, 128, 128)
        "aqua" -> rgb(0, 255, 255)

        "aliceblue" -> rgb(240, 248, 255)
        "antiquewhite" -> rgb(250, 235, 215)
        "aquamarine" -> rgb(127, 255, 212)
        "azure" -> rgb(240, 255, 255)
        "beige" -> rgb(245, 245, 220)
        "bisque" -> rgb(255, 228, 196)
        "blanchedalmond" -> rgb(255, 235, 205)
        "blueviolet" -> rgb(138, 43, 226)
        "brown" -> rgb(165, 42, 42)
        "burlywood" -> rgb(222, 184, 135)
        "cadetblue" -> rgb(95, 158, 160)
        "chartreuse" -> rgb(127, 255, 0)
        "chocolate" -> rgb(210, 105, 30)
        "coral" -> rgb(255, 127, 80)
        "cornflowerblue" -> rgb(100, 149, 237)
        "cornsilk" -> rgb(255, 248, 220)
        "crimson" -> rgb(220, 20, 60)
        "cyan" -> rgb(0, 255, 255)
        "darkblue" -> rgb(0, 0, 139)
        "darkcyan" -> rgb(0, 139, 139)
        "darkgoldenrod" -> rgb(184, 134, 11)
        "darkgray" -> rgb(169, 169, 169)
        "darkgreen" -> rgb(0, 100, 0)
        "darkgrey" -> rgb(169, 169, 169)
        "darkkhaki" -> rgb(189, 183, 107)
        "darkmagenta" -> rgb(139, 0, 139)
        "darkolivegreen" -> rgb(85, 107, 47)
        "darkorange" -> rgb(255, 140, 0)
        "darkorchid" -> rgb(153, 50, 204)
        "darkred" -> rgb(139, 0, 0)
        "darksalmon" -> rgb(233, 150, 122)
        "darkseagreen" -> rgb(143, 188, 143)
        "darkslateblue" -> rgb(72, 61, 139)
        "darkslategray" -> rgb(47, 79, 79)
        "darkslategrey" -> rgb(47, 79, 79)
        "darkturquoise" -> rgb(0, 206, 209)
        "darkviolet" -> rgb(148, 0, 211)
        "deeppink" -> rgb(255, 20, 147)
        "deepskyblue" -> rgb(0, 191, 255)
        "dimgray" -> rgb(105, 105, 105)
        "dimgrey" -> rgb(105, 105, 105)
        "dodgerblue" -> rgb(30, 144, 255)
        "firebrick" -> rgb(178, 34, 34)
        "floralwhite" -> rgb(255, 250, 240)
        "forestgreen" -> rgb(34, 139, 34)
        "gainsboro" -> rgb(220, 220, 220)
        "ghostwhite" -> rgb(248, 248, 255)
        "gold" -> rgb(255, 215, 0)
        "goldenrod" -> rgb(218, 165, 32)
        "greenyellow" -> rgb(173, 255, 47)
        "grey" -> rgb(128, 128, 128)
        "honeydew" -> rgb(240, 255, 240)
        "hotpink" -> rgb(255, 105, 180)
        "indianred" -> rgb(205, 92, 92)
        "indigo" -> rgb(75, 0, 130)
        "ivory" -> rgb(255, 255, 240)
        "khaki" -> rgb(240, 230, 140)
        "lavender" -> rgb(230, 230, 250)
        "lavenderblush" -> rgb(255, 240, 245)
        "lawngreen" -> rgb(124, 252, 0)
        "lemonchiffon" -> rgb(255, 250, 205)
        "lightblue" -> rgb(173, 216, 230)
        "lightcoral" -> rgb(240, 128, 128)
        "lightcyan" -> rgb(224, 255, 255)
        "lightgoldenrodyellow" -> rgb(250, 250, 210)
        "lightgray" -> rgb(211, 211, 211)
        "lightgreen" -> rgb(144, 238, 144)
        "lightgrey" -> rgb(211, 211, 211)
        "lightpink" -> rgb(255, 182, 193)
        "lightsalmon" -> rgb(255, 160, 122)
        "lightseagreen" -> rgb(32, 178, 170)
        "lightskyblue" -> rgb(135, 206, 250)
        "lightslategray" -> rgb(119, 136, 153)
        "lightslategrey" -> rgb(119, 136, 153)
        "lightsteelblue" -> rgb(176, 196, 222)
        "lightyellow" -> rgb(255, 255, 224)
        "limegreen" -> rgb(50, 205, 50)
        "linen" -> rgb(250, 240, 230)
        "magenta" -> rgb(255, 0, 255)
        "mediumaquamarine" -> rgb(102, 205, 170)
        "mediumblue" -> rgb(0, 0, 205)
        "mediumorchid" -> rgb(186, 85, 211)
        "mediumpurple" -> rgb(147, 112, 219)
        "mediumseagreen" -> rgb(60, 179, 113)
        "mediumslateblue" -> rgb(123, 104, 238)
        "mediumspringgreen" -> rgb(0, 250, 154)
        "mediumturquoise" -> rgb(72, 209, 204)
        "mediumvioletred" -> rgb(199, 21, 133)
        "midnightblue" -> rgb(25, 25, 112)
        "mintcream" -> rgb(245, 255, 250)
        "mistyrose" -> rgb(255, 228, 225)
        "moccasin" -> rgb(255, 228, 181)
        "navajowhite" -> rgb(255, 222, 173)
        "oldlace" -> rgb(253, 245, 230)
        "olivedrab" -> rgb(107, 142, 35)
        "orange" -> rgb(255, 165, 0)
        "orangered" -> rgb(255, 69, 0)
        "orchid" -> rgb(218, 112, 214)
        "palegoldenrod" -> rgb(238, 232, 170)
        "palegreen" -> rgb(152, 251, 152)
        "paleturquoise" -> rgb(175, 238, 238)
        "palevioletred" -> rgb(219, 112, 147)
        "papayawhip" -> rgb(255, 239, 213)
        "peachpuff" -> rgb(255, 218, 185)
        "peru" -> rgb(205, 133, 63)
        "pink" -> rgb(255, 192, 203)
        "plum" -> rgb(221, 160, 221)
        "powderblue" -> rgb(176, 224, 230)
        "rebeccapurple" -> rgb(102, 51, 153)
        "rosybrown" -> rgb(188, 143, 143)
        "royalblue" -> rgb(65, 105, 225)
        "saddlebrown" -> rgb(139, 69, 19)
        "salmon" -> rgb(250, 128, 114)
        "sandybrown" -> rgb(244, 164, 96)
        "seagreen" -> rgb(46, 139, 87)
        "seashell" -> rgb(255, 245, 238)
        "sienna" -> rgb(160, 82, 45)
        "skyblue" -> rgb(135, 206, 235)
        "slateblue" -> rgb(106, 90, 205)
        "slategray" -> rgb(112, 128, 144)
        "slategrey" -> rgb(112, 128, 144)
        "snow" -> rgb(255, 250, 250)
        "springgreen" -> rgb(0, 255, 127)
        "steelblue" -> rgb(70, 130, 180)
        "tan" -> rgb(210, 180, 140)
        "thistle" -> rgb(216, 191, 216)
        "tomato" -> rgb(255, 99, 71)
        "turquoise" -> rgb(64, 224, 208)
        "violet" -> rgb(238, 130, 238)
        "wheat" -> rgb(245, 222, 179)
        "whitesmoke" -> rgb(245, 245, 245)
        "yellowgreen" -> rgb(154, 205, 50)

        "transparent" -> rgba(0, 0, 0, 0)
        "currentcolor" -> Color.CurrentColor

        else -> return Err()
    }

    return Ok(color)
}

/**
 * Represents the RGB part of a [RGBA], including a flag [usesCommas] indicating whether the arguments were comma
 * separated.
 */
private data class RGBF(val red: Float, val green: Float, val blue: Float, val usesCommas: Boolean)

/**
 * Parses the arguments [input] of the color function with the specified [name] into a [RGBA] color. Supports
 * RGB, RGBA, HSL and HSLA.
 */
private fun parseColorFunction(input: Parser, parser: ColorComponentParser, name: String): Result<Color, ParseError> {
    val parseResult = when (name) {
        "rgb", "rgba" -> parseRGBColorFunction(input, parser)
        "hsl", "hsla" -> parseHSLColorFunction(input, parser)
        else -> return Err(input.newUnexpectedTokenError(Token.Identifier(name)))
    }

    val (red, green, blue, usesCommas) = when (parseResult) {
        is Ok -> parseResult.value
        is Err -> return parseResult
    }

    val alpha = if (!input.isExhausted()) {
        if (usesCommas) {
            val commaResult = input.expectComma()

            if (commaResult is Err) {
                return commaResult
            }
        } else {
            val solidusResult = input.expectSolidus()

            if (solidusResult is Err) {
                return solidusResult
            }
        }

        when (val numberOrPercentageResult = parser.parseNumberOrPercentage(input)) {
            is Ok -> clampUnit(numberOrPercentageResult.value.unitValue())
            is Err -> return numberOrPercentageResult
        }
    } else {
        1f
    }

    return Ok(Color.RGBA(RGBA(red, green, blue, alpha)))
}

/**
 * Parse the RGB arguments and returns them as a RGB color, including a flag whether the arguments are comma
 * separated. This function does not parse the alpha argument if present.
 */
private fun parseRGBColorFunction(input: Parser, parser: ColorComponentParser): Result<RGBF, ParseError> {
    val numberOrPercentage = when (val numberOrPercentage = parser.parseNumberOrPercentage(input)) {
        is Ok -> numberOrPercentage.value
        is Err -> return numberOrPercentage
    }

    val (red, isNumber) = when (numberOrPercentage) {
        is NumberOrPercentage.Number -> {
            Pair(clampNumber(numberOrPercentage.value), true)
        }

        is NumberOrPercentage.Percentage -> {
            Pair(clampUnit(numberOrPercentage.value), false)
        }
    }

    val usesCommas = input.tryParse { it.expectComma() }.isOk()

    val green: Float
    val blue: Float

    if (isNumber) {
        green = when (val greenResult = parser.parseNumber(input)) {
            is Ok -> clampNumber(greenResult.value)
            is Err -> return greenResult
        }

        if (usesCommas) {
            val commaResult = input.expectComma()

            if (commaResult is Err) {
                return commaResult
            }
        }

        blue = when (val blueResult = parser.parseNumber(input)) {
            is Ok -> clampNumber(blueResult.value)
            is Err -> return blueResult
        }
    } else {
        green = when (val greenResult = parser.parsePercentage(input)) {
            is Ok -> clampUnit(greenResult.value)
            is Err -> return greenResult
        }

        if (usesCommas) {
            val commaResult = input.expectComma()

            if (commaResult is Err) {
                return commaResult
            }
        }

        blue = when (val blueResult = parser.parsePercentage(input)) {
            is Ok -> clampUnit(blueResult.value)
            is Err -> return blueResult
        }
    }

    return Ok(RGBF(red, green, blue, usesCommas))
}

/**
 * Parses the HSL arguments of the function returns them as a RGB color, including a flag, whether the arguments are
 * comma separated. This function does not parse the alpha argument if present.
 */
private fun parseHSLColorFunction(input: Parser, parser: ColorComponentParser): Result<RGBF, ParseError> {
    val degrees = when (val angleOrNumber = parser.parseAngleOrNumber(input)) {
        is Ok -> angleOrNumber.value.degrees()
        is Err -> return angleOrNumber
    }
    val normalizedDegrees = degrees - (360.0 * (degrees / 360.0).trunc()).toFloat()
    val hue = normalizedDegrees / 360

    val usesCommas = input.tryParse { it.expectComma() }.isOk()

    val saturation = when (val saturation = parser.parsePercentage(input)) {
        is Ok -> saturation.value
        is Err -> return saturation
    }

    if (usesCommas) {
        val commaResult = input.expectComma()

        if (commaResult is Err) {
            return commaResult
        }
    }

    val lightness = when (val lightness = parser.parsePercentage(input)) {
        is Ok -> lightness.value
        is Err -> return lightness
    }

    val m2 = if (lightness <= 0.5) {
        lightness * (saturation + 1)
    } else {
        lightness + saturation - lightness * saturation
    }

    val m1 = lightness * 2 - m2
    val hueTimes3 = hue * 3
    val red = clampUnit(hueToRgb(m1, m2, hueTimes3 + 1))
    val green = clampUnit(hueToRgb(m1, m2, hueTimes3))
    val blue = clampUnit(hueToRgb(m1, m2, hueTimes3 - 1))

    return Ok(RGBF(red, green, blue, usesCommas))
}

/**
 * Converts a hue to rgb.
 */
private fun hueToRgb(m1: Float, m2: Float, h3i: Float): Float {
    var h3 = h3i
    if (h3 < 0f) {
        h3 += 3f
    }
    if (h3 > 3f) {
        h3 -= 3f
    }

    return when {
        (h3 * 2f < 1f) -> m1 + (m2 - m1) * h3 * 2f
        (h3 * 2f < 3f) -> m2
        (h3 < 2f) -> m1 + (m2 - m1) * (2f - h3) * 2f
        else -> m1
    }
}

private fun clampUnit(value: Float): Float {
    return value.coerceAtLeast(0f).coerceAtMost(1f)
}

/**
 * Clamps [value] to a value ranging from 0 to 255.
 */
private fun clampNumber(value: Float): Float {
    return clampUnit(value / 255f)
}
