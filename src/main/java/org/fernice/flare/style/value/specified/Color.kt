/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import java.io.Writer
import org.fernice.flare.cssparser.Color as ParserColor
import org.fernice.flare.style.value.computed.Color as ComputedColor
import org.fernice.flare.style.value.computed.RGBAColor as ComputedColorPropertyValue

sealed class Color : SpecifiedValue<ComputedColor>, ToCss {

    data class RGBA(val rgba: org.fernice.flare.cssparser.RGBA, val keyword: String?) : Color()
    object CurrentColor : Color()

    final override fun toComputedValue(context: Context): ComputedColor {
        return when (this) {
            is RGBA -> ComputedColor.RGBA(rgba)
            is CurrentColor -> ComputedColor.CurrentColor
        }
    }

    override fun toCss(writer: Writer) {
        when (this) {
            is RGBA -> {
                if (keyword != null) {
                    writer.append(keyword)
                } else {
                    rgba.toCss(writer)
                }
            }
            is CurrentColor -> writer.append("currentcolor")
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Color, ParseError> {
            val state = input.state()
            val keyword = input.expectIdentifier().ok()
            input.reset(state)

            val color = when (val color = ParserColor.parse(input)) {
                is Ok -> color.value
                is Err -> return color
            }

            return when (color) {
                is ParserColor.RGBA -> Ok(RGBA(color.rgba, keyword))
                is ParserColor.CurrentColor -> Ok(CurrentColor)
            }
        }

        private val transparent: Color by lazy {
            RGBA(
                RGBA(0, 0, 0, 0),
                "transparent"
            )
        }

        fun transparent(): Color {
            return transparent
        }
    }
}

data class RGBAColor(val color: Color) : SpecifiedValue<RGBA>, ToCss {
    companion object {
        fun parse(context: ParserContext, input: Parser): Result<RGBAColor, ParseError> {
            return Color.parse(context, input).map(::RGBAColor)
        }
    }

    override fun toComputedValue(context: Context): RGBA {
        return color.toComputedValue(context)
            .toRGBA(context.style().getColor().color)
    }

    override fun toCss(writer: Writer) {
        color.toCss(writer)
    }
}

data class ColorPropertyValue(val color: Color) : SpecifiedValue<ComputedColorPropertyValue>, ToCss {

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<ColorPropertyValue, ParseError> {
            return Color.parse(context, input).map(::ColorPropertyValue)
        }
    }

    override fun toComputedValue(context: Context): RGBA {
        return color.toComputedValue(context)
            .toRGBA(context.style().getParentColor().color)
    }

    override fun toCss(writer: Writer) {
        color.toCss(writer)
    }
}
