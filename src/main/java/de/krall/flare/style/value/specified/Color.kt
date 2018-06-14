package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.RGBA
import de.krall.flare.std.*
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.cssparser.Color as ParserColor
import de.krall.flare.style.value.computed.Color as ComputedColor
import de.krall.flare.style.value.computed.RGBAColor as ComputedColorPropertyValue

sealed class Color : SpecifiedValue<ComputedColor> {

    class RGBA(val rgba: de.krall.flare.cssparser.RGBA, keyword: Option<String>) : Color() {
        override fun toComputedValue(context: Context): ComputedColor {
            return ComputedColor.RGBA(rgba)
        }
    }

    class CurrentColor : Color() {
        override fun toComputedValue(context: Context): ComputedColor {
            return ComputedColor.CurrentColor()
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
                is ParserColor.RGBA -> Ok(Color.RGBA(color.rgba, keyword))
                is ParserColor.CurrentColor -> Ok(Color.CurrentColor())
            }
        }

        private val transparent: Color by lazy {
            Color.RGBA(
                    de.krall.flare.cssparser.RGBA(0, 0, 0, 0),
                    Some("transparent")
            )
        }

        fun transparent(): Color {
            return transparent
        }
    }
}

class RGBAColor(val color: Color) : SpecifiedValue<RGBA> {
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

class ColorPropertyValue(val color: Color) : SpecifiedValue<ComputedColorPropertyValue> {

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
