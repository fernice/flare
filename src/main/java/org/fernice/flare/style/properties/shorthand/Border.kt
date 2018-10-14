/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.shorthand

import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.unwrapOr
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.ShorthandId
import org.fernice.flare.style.properties.longhand.BorderBottomColorDeclaration
import org.fernice.flare.style.properties.longhand.BorderBottomColorId
import org.fernice.flare.style.properties.longhand.BorderBottomLeftRadiusDeclaration
import org.fernice.flare.style.properties.longhand.BorderBottomLeftRadiusId
import org.fernice.flare.style.properties.longhand.BorderBottomRightRadiusDeclaration
import org.fernice.flare.style.properties.longhand.BorderBottomRightRadiusId
import org.fernice.flare.style.properties.longhand.BorderBottomStyleDeclaration
import org.fernice.flare.style.properties.longhand.BorderBottomStyleId
import org.fernice.flare.style.properties.longhand.BorderBottomWidthDeclaration
import org.fernice.flare.style.properties.longhand.BorderBottomWidthId
import org.fernice.flare.style.properties.longhand.BorderLeftColorDeclaration
import org.fernice.flare.style.properties.longhand.BorderLeftColorId
import org.fernice.flare.style.properties.longhand.BorderLeftStyleDeclaration
import org.fernice.flare.style.properties.longhand.BorderLeftStyleId
import org.fernice.flare.style.properties.longhand.BorderLeftWidthDeclaration
import org.fernice.flare.style.properties.longhand.BorderLeftWidthId
import org.fernice.flare.style.properties.longhand.BorderRightColorDeclaration
import org.fernice.flare.style.properties.longhand.BorderRightColorId
import org.fernice.flare.style.properties.longhand.BorderRightStyleDeclaration
import org.fernice.flare.style.properties.longhand.BorderRightStyleId
import org.fernice.flare.style.properties.longhand.BorderRightWidthDeclaration
import org.fernice.flare.style.properties.longhand.BorderRightWidthId
import org.fernice.flare.style.properties.longhand.BorderTopColorDeclaration
import org.fernice.flare.style.properties.longhand.BorderTopColorId
import org.fernice.flare.style.properties.longhand.BorderTopLeftRadiusDeclaration
import org.fernice.flare.style.properties.longhand.BorderTopLeftRadiusId
import org.fernice.flare.style.properties.longhand.BorderTopRightRadiusDeclaration
import org.fernice.flare.style.properties.longhand.BorderTopRightRadiusId
import org.fernice.flare.style.properties.longhand.BorderTopStyleDeclaration
import org.fernice.flare.style.properties.longhand.BorderTopStyleId
import org.fernice.flare.style.properties.longhand.BorderTopWidthDeclaration
import org.fernice.flare.style.properties.longhand.BorderTopWidthId
import org.fernice.flare.style.value.computed.Style
import org.fernice.flare.style.value.generic.Rect
import org.fernice.flare.style.value.specified.BorderCornerRadius
import org.fernice.flare.style.value.specified.BorderSideWidth
import org.fernice.flare.style.value.specified.Color
import org.fernice.flare.style.value.specified.LengthOrPercentage

private data class Longhands(
    val width: BorderSideWidth,
    val color: Color,
    val style: Style
)

private fun parseBorder(context: ParserContext, input: Parser): Result<Longhands, ParseError> {
    var color: Option<Color> = None
    var style: Option<Style> = None
    var width: Option<BorderSideWidth> = None
    var any = false

    while (true) {
        if (color.isNone()) {
            val colorResult = input.tryParse { input -> Color.parse(context, input) }

            if (colorResult is Ok) {
                color = Some(colorResult.value)
                any = true
                continue
            }
        }

        if (style.isNone()) {
            val styleResult = input.tryParse { input -> Style.parse(input) }

            if (styleResult is Ok) {
                style = Some(styleResult.value)
                any = true
                continue
            }
        }

        if (width.isNone()) {
            val widthResult = input.tryParse { input -> BorderSideWidth.parse(context, input) }

            if (widthResult is Ok) {
                width = Some(widthResult.value)
                any = true
                continue
            }
        }

        break
    }

    return if (any) {
        Ok(
            Longhands(
                width.unwrapOr(BorderSideWidth.Medium),
                color.unwrapOr(Color.transparent()),
                style.unwrapOr(Style.None)
            )
        )
    } else {
        Err(input.newError(ParseErrorKind.Unknown))
    }
}

object BorderId : ShorthandId() {

    override val name: String = "border"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = parseBorder(context, input)

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
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
            BorderLeftWidthId
        )
    }
}

object BorderColorId : ShorthandId() {

    override val name: String = "border-color"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = Rect.parseWith(context, input, Color.Companion::parse)

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderTopColorId,
            BorderRightColorId,
            BorderBottomColorId,
            BorderLeftColorId
        )
    }
}

object BorderStyleId : ShorthandId() {

    override val name: String = "border-style"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = Rect.parseWith(context, input) { _, parser -> Style.parse(parser) }

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderTopStyleId,
            BorderRightStyleId,
            BorderBottomStyleId,
            BorderLeftStyleId
        )
    }
}

object BorderWidthId : ShorthandId() {

    override val name: String = "border-width"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = Rect.parseWith(context, input, BorderSideWidth.Companion::parse)

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderTopWidthId,
            BorderRightWidthId,
            BorderBottomWidthId,
            BorderLeftWidthId
        )
    }
}

object BorderRadiusId : ShorthandId() {

    override val name: String = "border-radius"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val widthsResult = Rect.parseWith(context, input, LengthOrPercentage.Companion::parse)

        val widths = when (widthsResult) {
            is Ok -> widthsResult.value
            is Err -> return widthsResult
        }

        val heights = if (input.tryParse { parser -> parser.expectSolidus() } is Ok) {
            val heightsResult = Rect.parseWith(context, input, LengthOrPercentage.Companion::parse)

            when (heightsResult) {
                is Ok -> heightsResult.value
                is Err -> return heightsResult
            }
        } else {
            widths
        }

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
            BorderBottomLeftRadiusDeclaration(
                BorderCornerRadius(widths.bottom, heights.bottom)
            )
        )
        declarations.add(
            BorderBottomRightRadiusDeclaration(
                BorderCornerRadius(widths.left, heights.left)
            )
        )

        return Ok()
    }

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderTopLeftRadiusId,
            BorderTopRightRadiusId,
            BorderBottomLeftRadiusId,
            BorderBottomRightRadiusId
        )
    }
}

object BorderTopId : ShorthandId() {

    override val name: String = "border-top"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = parseBorder(context, input)

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderTopWidthId,
            BorderTopColorId,
            BorderTopStyleId
        )
    }
}

object BorderRightId : ShorthandId() {

    override val name: String = "border-right"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = parseBorder(context, input)

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderRightWidthId,
            BorderRightColorId,
            BorderRightStyleId
        )
    }
}

object BorderBottomId : ShorthandId() {

    override val name: String = "border-bottom"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = parseBorder(context, input)

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


    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderBottomWidthId,
            BorderBottomColorId,
            BorderBottomStyleId
        )
    }
}

object BorderLeftId : ShorthandId() {

    override val name: String = "border-left"

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
        val result = parseBorder(context, input)

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

    override val longhands: List<LonghandId> by lazy {
        listOf(
            BorderLeftWidthId,
            BorderLeftColorId,
            BorderLeftStyleId
        )
    }
}
