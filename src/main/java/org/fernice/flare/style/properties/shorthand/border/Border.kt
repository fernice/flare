/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.shorthand.border

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.border.BorderBottomColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomColorId
import org.fernice.flare.style.properties.longhand.border.BorderBottomLeftRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomLeftRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderBottomRightRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomRightRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderBottomStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomStyleId
import org.fernice.flare.style.properties.longhand.border.BorderBottomWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderBottomWidthId
import org.fernice.flare.style.properties.longhand.border.BorderLeftColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderLeftColorId
import org.fernice.flare.style.properties.longhand.border.BorderLeftStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderLeftStyleId
import org.fernice.flare.style.properties.longhand.border.BorderLeftWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderLeftWidthId
import org.fernice.flare.style.properties.longhand.border.BorderRightColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderRightColorId
import org.fernice.flare.style.properties.longhand.border.BorderRightStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderRightStyleId
import org.fernice.flare.style.properties.longhand.border.BorderRightWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderRightWidthId
import org.fernice.flare.style.properties.longhand.border.BorderTopColorDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopColorId
import org.fernice.flare.style.properties.longhand.border.BorderTopLeftRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopLeftRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderTopRightRadiusDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopRightRadiusId
import org.fernice.flare.style.properties.longhand.border.BorderTopStyleDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopStyleId
import org.fernice.flare.style.properties.longhand.border.BorderTopWidthDeclaration
import org.fernice.flare.style.properties.longhand.border.BorderTopWidthId
import org.fernice.flare.style.value.computed.BorderStyle
import org.fernice.flare.style.value.generic.Rect
import org.fernice.flare.style.value.specified.BorderCornerRadius
import org.fernice.flare.style.value.specified.BorderSideWidth
import org.fernice.flare.style.value.specified.Color
import org.fernice.flare.style.value.specified.LengthOrPercentage
import org.fernice.std.unwrap

private data class Longhands(
    val width: BorderSideWidth,
    val color: Color,
    val style: BorderStyle,
)

private fun parseBorder(context: ParserContext, input: Parser): Result<Longhands, ParseError> {
    var color: Color? = null
    var style: BorderStyle? = null
    var width: BorderSideWidth? = null
    var any = false

    while (true) {
        if (color == null) {
            val colorResult = input.tryParse { input -> Color.parse(context, input) }

            if (colorResult is Ok) {
                color = colorResult.value
                any = true
                continue
            }
        }

        if (style == null) {
            val styleResult = input.tryParse { input -> BorderStyle.parse(input) }

            if (styleResult is Ok) {
                style = styleResult.value
                any = true
                continue
            }
        }

        if (width == null) {
            val widthResult = input.tryParse { input -> BorderSideWidth.parse(context, input) }

            if (widthResult is Ok) {
                width = widthResult.value
                any = true
                continue
            }
        }

        break
    }

    return if (any) {
        Ok(
            Longhands(
                width ?: BorderSideWidth.Medium,
                color ?: Color.Transparent,
                style ?: BorderStyle.None,
            )
        )
    } else {
        Err(input.newError(ParseErrorKind.Unknown))
    }
}

object BorderId : ShorthandId(
    name = "border",
    longhands = listOf(
        BorderTopColorId,
        BorderTopStyleId,
        BorderTopWidthId,

        BorderRightColorId,
        BorderRightStyleId,
        BorderRightWidthId,

        BorderBottomColorId,
        BorderBottomStyleId,
        BorderBottomWidthId,

        BorderLeftColorId,
        BorderLeftStyleId,
        BorderLeftWidthId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { parseBorder(context, input) }

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderTopColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderTopStyleDeclaration(
                style
            )
        )
        declarations.add(
            BorderTopWidthDeclaration(
                width
            )
        )

        declarations.add(
            BorderRightColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderRightStyleDeclaration(
                style
            )
        )
        declarations.add(
            BorderRightWidthDeclaration(
                width
            )
        )

        declarations.add(
            BorderBottomColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderBottomStyleDeclaration(
                style
            )
        )
        declarations.add(
            BorderBottomWidthDeclaration(
                width
            )
        )

        declarations.add(
            BorderLeftColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderLeftStyleDeclaration(
                style
            )
        )
        declarations.add(
            BorderLeftWidthDeclaration(
                width
            )
        )

        return Ok()
    }
}

object BorderColorId : ShorthandId(
    name = "border-color",
    longhands = listOf(
        BorderTopColorId,
        BorderRightColorId,
        BorderBottomColorId,
        BorderLeftColorId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { Rect.parseWith(context, input, Color.Companion::parse) }

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderTopColorDeclaration(
                sides.top
            )
        )
        declarations.add(
            BorderRightColorDeclaration(
                sides.right
            )
        )
        declarations.add(
            BorderBottomColorDeclaration(
                sides.bottom
            )
        )
        declarations.add(
            BorderLeftColorDeclaration(
                sides.left
            )
        )

        return Ok()
    }
}

object BorderStyleId : ShorthandId(
    name = "border-style",
    longhands = listOf(
        BorderTopStyleId,
        BorderRightStyleId,
        BorderBottomStyleId,
        BorderLeftStyleId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { Rect.parseWith(context, input) { _, parser -> BorderStyle.parse(parser) } }

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderTopStyleDeclaration(
                sides.top
            )
        )
        declarations.add(
            BorderRightStyleDeclaration(
                sides.right
            )
        )
        declarations.add(
            BorderBottomStyleDeclaration(
                sides.bottom
            )
        )
        declarations.add(
            BorderLeftStyleDeclaration(
                sides.left
            )
        )

        return Ok()
    }
}

object BorderWidthId : ShorthandId(
    name = "border-width",
    longhands = listOf(
        BorderTopWidthId,
        BorderRightWidthId,
        BorderBottomWidthId,
        BorderLeftWidthId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { Rect.parseWith(context, input, BorderSideWidth.Companion::parse) }

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderTopWidthDeclaration(
                sides.top
            )
        )
        declarations.add(
            BorderRightWidthDeclaration(
                sides.right
            )
        )
        declarations.add(
            BorderBottomWidthDeclaration(
                sides.bottom
            )
        )
        declarations.add(
            BorderLeftWidthDeclaration(
                sides.left
            )
        )

        return Ok()
    }
}

object BorderRadiusId : ShorthandId(
    name = "border-radius",
    longhands = listOf(
        BorderTopLeftRadiusId,
        BorderTopRightRadiusId,
        BorderBottomRightRadiusId,
        BorderBottomLeftRadiusId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val (widths, heights) = input.parseEntirely { parseWidthsAndHeights(context, input) }.unwrap { return it }

        declarations.add(
            BorderTopLeftRadiusDeclaration(
                BorderCornerRadius(widths.top, heights.top)
            )
        )
        declarations.add(
            BorderTopRightRadiusDeclaration(
                BorderCornerRadius(widths.right, heights.right)
            )
        )
        declarations.add(
            BorderBottomRightRadiusDeclaration(
                BorderCornerRadius(widths.bottom, heights.bottom)
            )
        )
        declarations.add(
            BorderBottomLeftRadiusDeclaration(
                BorderCornerRadius(widths.left, heights.left)
            )
        )

        return Ok()
    }

    private fun parseWidthsAndHeights(context: ParserContext, input: Parser): Result<Pair<Rect<LengthOrPercentage>, Rect<LengthOrPercentage>>, ParseError> {
        val widths = Rect.parseWith(context, input, LengthOrPercentage.Companion::parse).unwrap { return it }

        val heights = if (input.tryParse { parser -> parser.expectSolidus() } is Ok) {
            Rect.parseWith(context, input, LengthOrPercentage.Companion::parse).unwrap { return it }
        } else {
            widths
        }

        return Ok(widths to heights)
    }
}

object BorderTopId : ShorthandId(
    name = "border-top",
    longhands = listOf(
        BorderTopWidthId,
        BorderTopColorId,
        BorderTopStyleId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { parseBorder(context, input) }

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderTopWidthDeclaration(
                width
            )
        )
        declarations.add(
            BorderTopColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderTopStyleDeclaration(
                style
            )
        )

        return Ok()
    }
}

object BorderRightId : ShorthandId(
    name = "border-right",
    longhands = listOf(
        BorderRightWidthId,
        BorderRightColorId,
        BorderRightStyleId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { parseBorder(context, input) }

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderRightWidthDeclaration(
                width
            )
        )
        declarations.add(
            BorderRightColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderRightStyleDeclaration(
                style
            )
        )

        return Ok()
    }
}

object BorderBottomId : ShorthandId(
    name = "border-bottom",
    longhands = listOf(
        BorderBottomWidthId,
        BorderBottomColorId,
        BorderBottomStyleId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { parseBorder(context, input) }

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderBottomWidthDeclaration(
                width
            )
        )
        declarations.add(
            BorderBottomColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderBottomStyleDeclaration(
                style
            )
        )

        return Ok()
    }
}

object BorderLeftId : ShorthandId(
    name = "border-left",
    longhands = listOf(
        BorderLeftWidthId,
        BorderLeftColorId,
        BorderLeftStyleId,
    ),
) {

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = input.parseEntirely { parseBorder(context, input) }

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(
            BorderLeftWidthDeclaration(
                width
            )
        )
        declarations.add(
            BorderLeftColorDeclaration(
                color
            )
        )
        declarations.add(
            BorderLeftStyleDeclaration(
                style
            )
        )

        return Ok()
    }
}
