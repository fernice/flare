/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.RGBA
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import org.fernice.flare.cssparser.Color as ParserColor
import org.fernice.flare.style.value.computed.Color as ComputedColor
import org.fernice.flare.style.value.computed.RGBAColor as ComputedColorPropertyValue

sealed class Color : SpecifiedValue<ComputedColor> {

    data class RGBA(val rgba: org.fernice.flare.cssparser.RGBA, val keyword: Option<String>) : Color() {
        override fun toComputedValue(context: Context): ComputedColor {
            return ComputedColor.RGBA(rgba)
        }
    }

    object CurrentColor : Color() {
        override fun toComputedValue(context: Context): ComputedColor {
            return ComputedColor.CurrentColor
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<Color, ParseError> {
            val state = input.state()
            val keyword = input.expectIdentifier().ok()
            input.reset(state)

            val colorResult = ParserColor.parse(input)

            val color = when (colorResult) {
                is Ok -> colorResult.value
                is Err -> return colorResult
            }

            return when (color) {
                is ParserColor.RGBA -> Ok(RGBA(color.rgba, keyword))
                is ParserColor.CurrentColor -> Ok(CurrentColor)
            }
        }

        private val transparent: Color by lazy {
            Color.RGBA(
                    org.fernice.flare.cssparser.RGBA(0, 0, 0, 0),
                    Some("transparent")
            )
        }

        fun transparent(): Color {
            return transparent
        }
    }
}

data class RGBAColor(val color: Color) : SpecifiedValue<RGBA> {
    companion object {
        fun parse(context: ParserContext, input: Parser): Result<RGBAColor, ParseError> {
            return Color.parse(context, input).map(::RGBAColor)
        }
    }

    override fun toComputedValue(context: Context): RGBA {
        return color.toComputedValue(context)
                .toRGBA(context.style().getColor().color)
    }
}

data class ColorPropertyValue(val color: Color) : SpecifiedValue<ComputedColorPropertyValue> {

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<ColorPropertyValue, ParseError> {
            return Color.parse(context, input).map(::ColorPropertyValue)
        }
    }

    override fun toComputedValue(context: Context): RGBA {
        return color.toComputedValue(context)
                .toRGBA(context.style().getParentColor().color)
    }
}
