package de.krall.flare.style.properties.shorthand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.*
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.properties.ShorthandId
import de.krall.flare.style.properties.longhand.*
import de.krall.flare.style.value.computed.Style
import de.krall.flare.style.value.generic.Rect
import de.krall.flare.style.value.specified.BorderCornerRadius
import de.krall.flare.style.value.specified.BorderSideWidth
import de.krall.flare.style.value.specified.Color
import de.krall.flare.style.value.specified.LengthOrPercentage

private data class Longhands(val width: BorderSideWidth,
                             val color: Color,
                             val style: Style)

private fun parseBorder(context: ParserContext, input: Parser): Result<Longhands, ParseError> {
    var color: Option<Color> = None()
    var style: Option<Style> = None()
    var width: Option<BorderSideWidth> = None()
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
        Ok(Longhands(
                width.unwrapOr(BorderSideWidth.Medium()),
                color.unwrapOr(Color.transparent()),
                style.unwrapOr(Style.NONE)
        ))
    } else {
        Err(input.newError(ParseErrorKind.Unkown()))
    }
}

@PropertyEntryPoint
class BorderId : ShorthandId() {

    override fun name(): String {
        return "border"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = parseBorder(context, input)

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderTopColorDeclaration(
                color
        ))
        declarations.add(BorderTopStyleDeclaration(
                style
        ))
        declarations.add(BorderTopWidthDeclaration(
                width
        ))

        declarations.add(BorderRightColorDeclaration(
                color
        ))
        declarations.add(BorderRightStyleDeclaration(
                style
        ))
        declarations.add(BorderRightWidthDeclaration(
                width
        ))

        declarations.add(BorderBottomColorDeclaration(
                color
        ))
        declarations.add(BorderBottomStyleDeclaration(
                style
        ))
        declarations.add(BorderBottomWidthDeclaration(
                width
        ))

        declarations.add(BorderLeftColorDeclaration(
                color
        ))
        declarations.add(BorderLeftStyleDeclaration(
                style
        ))
        declarations.add(BorderLeftWidthDeclaration(
                width
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderTopColorId.instance,
                    BorderTopStyleId.instance,
                    BorderTopWidthId.instance,

                    BorderRightColorId.instance,
                    BorderRightStyleId.instance,
                    BorderRightWidthId.instance,

                    BorderBottomColorId.instance,
                    BorderBottomStyleId.instance,
                    BorderBottomWidthId.instance,

                    BorderLeftColorId.instance,
                    BorderLeftStyleId.instance,
                    BorderLeftWidthId.instance
            )
        }

        val instance: BorderId by lazy { BorderId() }
    }
}

@PropertyEntryPoint
class BorderColorId : ShorthandId() {

    override fun name(): String {
        return "border-color"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = Rect.parseWith(context, input, Color.Companion::parse)

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderTopColorDeclaration(
                sides.top
        ))
        declarations.add(BorderRightColorDeclaration(
                sides.right
        ))
        declarations.add(BorderBottomColorDeclaration(
                sides.bottom
        ))
        declarations.add(BorderLeftColorDeclaration(
                sides.left
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderTopColorId.instance,
                    BorderRightColorId.instance,
                    BorderBottomColorId.instance,
                    BorderLeftColorId.instance
            )
        }

        val instance: BorderColorId by lazy { BorderColorId() }
    }
}

@PropertyEntryPoint
class BorderStyleId : ShorthandId() {

    override fun name(): String {
        return "border-style"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = Rect.parseWith(context, input, { _, input -> Style.parse(input) })

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderTopStyleDeclaration(
                sides.top
        ))
        declarations.add(BorderRightStyleDeclaration(
                sides.right
        ))
        declarations.add(BorderBottomStyleDeclaration(
                sides.bottom
        ))
        declarations.add(BorderLeftStyleDeclaration(
                sides.left
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderTopStyleId.instance,
                    BorderRightStyleId.instance,
                    BorderBottomStyleId.instance,
                    BorderLeftStyleId.instance
            )
        }

        val instance: BorderStyleId by lazy { BorderStyleId() }
    }
}

@PropertyEntryPoint
class BorderWidthId : ShorthandId() {

    override fun name(): String {
        return "border-width"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = Rect.parseWith(context, input, BorderSideWidth.Companion::parse)

        val sides = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderTopWidthDeclaration(
                sides.top
        ))
        declarations.add(BorderRightWidthDeclaration(
                sides.right
        ))
        declarations.add(BorderBottomWidthDeclaration(
                sides.bottom
        ))
        declarations.add(BorderLeftWidthDeclaration(
                sides.left
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderTopWidthId.instance,
                    BorderRightWidthId.instance,
                    BorderBottomWidthId.instance,
                    BorderLeftWidthId.instance
            )
        }

        val instance: BorderWidthId by lazy { BorderWidthId() }
    }
}

@PropertyEntryPoint
class BorderRadiusId : ShorthandId() {

    override fun name(): String {
        return "border-radius"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val widthsResult = Rect.parseWith(context, input, LengthOrPercentage.Companion::parse)

        val widths = when (widthsResult) {
            is Ok -> widthsResult.value
            is Err -> return widthsResult
        }

        val heights = if (input.tryParse { input -> input.expectSolidus() } is Ok) {
            val heightsResult = Rect.parseWith(context, input, LengthOrPercentage.Companion::parse)

            when (heightsResult) {
                is Ok -> heightsResult.value
                is Err -> return heightsResult
            }
        } else {
            widths
        }

        declarations.add(BorderTopLeftRadiusDeclaration(
                BorderCornerRadius(widths.top, heights.top)
        ))
        declarations.add(BorderTopRightRadiusDeclaration(
                BorderCornerRadius(widths.right, heights.right)
        ))
        declarations.add(BorderBottomLeftRadiusDeclaration(
                BorderCornerRadius(widths.bottom, heights.bottom)
        ))
        declarations.add(BorderBottomRightRadiusDeclaration(
                BorderCornerRadius(widths.left, heights.left)
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderTopLeftRadiusId.instance,
                    BorderTopRightRadiusId.instance,
                    BorderBottomLeftRadiusId.instance,
                    BorderBottomRightRadiusId.instance
            )
        }

        val instance: BorderRadiusId by lazy { BorderRadiusId() }
    }
}

@PropertyEntryPoint
class BorderTopId : ShorthandId() {

    override fun name(): String {
        return "border-top"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = parseBorder(context, input)

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderTopWidthDeclaration(
                width
        ))
        declarations.add(BorderTopColorDeclaration(
                color
        ))
        declarations.add(BorderTopStyleDeclaration(
                style
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderTopWidthId.instance,
                    BorderTopColorId.instance,
                    BorderTopStyleId.instance
            )
        }

        val instance: BorderTopId by lazy { BorderTopId() }
    }
}

@PropertyEntryPoint
class BorderRightId : ShorthandId() {

    override fun name(): String {
        return "border-right"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = parseBorder(context, input)

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderRightWidthDeclaration(
                width
        ))
        declarations.add(BorderRightColorDeclaration(
                color
        ))
        declarations.add(BorderRightStyleDeclaration(
                style
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderRightWidthId.instance,
                    BorderRightColorId.instance,
                    BorderRightStyleId.instance
            )
        }

        val instance: BorderRightId by lazy { BorderRightId() }
    }
}

@PropertyEntryPoint
class BorderBottomId : ShorthandId() {

    override fun name(): String {
        return "border-bottom"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = parseBorder(context, input)

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderBottomWidthDeclaration(
                width
        ))
        declarations.add(BorderBottomColorDeclaration(
                color
        ))
        declarations.add(BorderBottomStyleDeclaration(
                style
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderBottomWidthId.instance,
                    BorderBottomColorId.instance,
                    BorderBottomStyleId.instance
            )
        }

        val instance: BorderBottomId by lazy { BorderBottomId() }
    }
}

@PropertyEntryPoint
class BorderLeftId : ShorthandId() {

    override fun name(): String {
        return "border-left"
    }

    override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
        val result = parseBorder(context, input)

        val (width, color, style) = when (result) {
            is Ok -> result.value
            is Err -> return result
        }

        declarations.add(BorderLeftWidthDeclaration(
                width
        ))
        declarations.add(BorderLeftColorDeclaration(
                color
        ))
        declarations.add(BorderLeftStyleDeclaration(
                style
        ))

        return Ok()
    }

    override fun getLonghands(): List<LonghandId> {
        return longhands
    }

    companion object {

        private val longhands: List<LonghandId> by lazy {
            listOf(
                    BorderLeftWidthId.instance,
                    BorderLeftColorId.instance,
                    BorderLeftStyleId.instance
            )
        }

        val instance: BorderLeftId by lazy { BorderLeftId() }
    }
}