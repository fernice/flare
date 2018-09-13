/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.toComputedValue
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import fernice.std.unwrapOr
import fernice.std.unwrapOrElse
import org.fernice.flare.style.value.computed.Image as ComputedImage
import org.fernice.flare.style.value.computed.Gradient as ComputedGradient
import org.fernice.flare.style.value.computed.GradientItem as ComputedGradientItem
import org.fernice.flare.style.value.computed.GradientKind as ComputedGradientKind
import org.fernice.flare.style.value.computed.ColorStop as ComputedColorStop
import org.fernice.flare.style.value.computed.LineDirection as ComputedLineDirection
import org.fernice.flare.style.value.computed.EndingShape as ComputedEndingShape
import org.fernice.flare.style.value.computed.Circle as ComputedCircle
import org.fernice.flare.style.value.computed.Ellipse as ComputedEllipse

sealed class Image : SpecifiedValue<ComputedImage> {

    data class Url(val url: ImageUrl) : Image()

    data class Gradient(val gradient: SpecifiedGradient) : Image()

    override fun toComputedValue(context: Context): ComputedImage {
        return when (this) {
            is Image.Url -> ComputedImage.Url(url.toComputedValue(context))
            is Image.Gradient -> ComputedImage.Gradient(gradient.toComputedValue(context))
        }
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<Image, ParseError> {
            val url = input.tryParse { nestedParser -> ImageUrl.parse(context, nestedParser) }

            if (url is Ok) {
                return Ok(Url(url.value))
            }

            val gradient = SpecifiedGradient.parse(context, input)

            return when (gradient) {
                is Ok -> Ok(Gradient(gradient.value))
                is Err -> gradient
            }
        }
    }
}

typealias Repeating = Boolean

private typealias SpecifiedGradient = Gradient

data class Gradient(
        val items: List<GradientItem>,
        val repeating: Repeating,
        val kind: GradientKind
) : SpecifiedValue<ComputedGradient> {

    override fun toComputedValue(context: Context): ComputedGradient {
        return ComputedGradient(
                items.toComputedValue(context),
                repeating,
                kind.toComputedValue(context)
        )
    }

    companion object {
        private enum class Shape {
            Linear,
            Radial
        }

        fun parse(context: ParserContext, input: Parser): Result<Gradient, ParseError> {
            val location = input.sourceLocation()
            val functionResult = input.expectFunction()

            val function = when (functionResult) {
                is Ok -> functionResult.value
                is Err -> return functionResult
            }

            val result = when (function) {
                "linear-gradient" -> Some(Pair(Shape.Linear, false))
                "repeating-linear-gradient" -> Some(Pair(Shape.Linear, true))
                "radial-gradient" -> Some(Pair(Shape.Radial, false))
                "repeating-radial-gradient" -> Some(Pair(Shape.Radial, true))
                else -> None
            }

            val (shape, repeating) = when (result) {
                is Some -> result.value
                is None -> return Err(location.newUnexpectedTokenError(Token.Function(function)))
            }

            val parseResult = input.parseNestedBlock<Pair<GradientKind, List<GradientItem>>> { nestedParser ->
                val shapeResult = when (shape) {
                    Shape.Linear -> GradientKind.parseLinear(context, nestedParser)
                    Shape.Radial -> GradientKind.parseRadial(context, nestedParser)
                }

                val shape = when (shapeResult) {
                    is Ok -> shapeResult.value
                    is Err -> return@parseNestedBlock shapeResult
                }

                val itemsResult = GradientItem.parseCommaSeparated(context, nestedParser)

                val items = when (itemsResult) {
                    is Ok -> itemsResult.value
                    is Err -> return@parseNestedBlock itemsResult
                }

                Ok(Pair(shape, items))
            }

            val (kind, items) = when (parseResult) {
                is Ok -> parseResult.value
                is Err -> return parseResult
            }

            if (items.size < 2) {
                return Err(input.newError(ParseErrorKind.Unknown))
            }

            return Ok(Gradient(
                    items,
                    repeating,
                    kind
            ))
        }
    }
}

sealed class GradientItem : SpecifiedValue<ComputedGradientItem> {

    data class InterpolationHint(val hint: LengthOrPercentage) : GradientItem()

    data class ColorStop(val colorStop: SpecifiedColorStop) : GradientItem()

    override fun toComputedValue(context: Context): ComputedGradientItem {
        return when (this) {
            is GradientItem.InterpolationHint -> ComputedGradientItem.InterpolationHint(hint.toComputedValue(context))
            is GradientItem.ColorStop -> ComputedGradientItem.ColorStop(colorStop.toComputedValue(context))
        }
    }

    companion object {
        fun parseCommaSeparated(context: ParserContext, input: Parser): Result<List<GradientItem>, ParseError> {
            var seenStop = false
            val itemsResult = input.parseCommaSeparated parse@{ nestedParser ->
                if (seenStop) {
                    val hint = nestedParser.tryParse { parser -> LengthOrPercentage.parse(context, parser) }

                    if (hint is Ok) {
                        seenStop = false
                        return@parse Ok(InterpolationHint(hint.value))
                    }
                }

                seenStop = true
                org.fernice.flare.style.value.specified.ColorStop.parse(context, nestedParser).map(GradientItem::ColorStop)
            }

            val items = when (itemsResult) {
                is Ok -> itemsResult.value
                is Err -> return itemsResult
            }

            if (!seenStop || items.size < 2) {
                return Err(input.newError(ParseErrorKind.Unknown))
            }

            return Ok(items)
        }
    }
}

private typealias SpecifiedColorStop = ColorStop

data class ColorStop(
        val color: RGBAColor,
        val position: Option<LengthOrPercentage>
) : SpecifiedValue<ComputedColorStop> {

    override fun toComputedValue(context: Context): ComputedColorStop {
        return ComputedColorStop(
                color.toComputedValue(context),
                position.toComputedValue(context)
        )
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<ColorStop, ParseError> {
            val colorResult = RGBAColor.parse(context, input)

            val color = when (colorResult) {
                is Ok -> colorResult.value
                is Err -> return colorResult
            }

            val position = input.tryParse { nestedParser -> LengthOrPercentage.parse(context, nestedParser) }.ok()

            return Ok(ColorStop(
                    color,
                    position
            ))
        }
    }
}

sealed class GradientKind : SpecifiedValue<ComputedGradientKind> {

    data class Linear(val lineDirection: LineDirection) : GradientKind()

    data class Radial(val endingShape: EndingShape, val position: Position) : GradientKind()

    override fun toComputedValue(context: Context): ComputedGradientKind {
        return when (this) {
            is GradientKind.Linear -> ComputedGradientKind.Linear(lineDirection.toComputedValue(context))
            is GradientKind.Radial -> ComputedGradientKind.Radial(endingShape.toComputedValue(context), position.toComputedValue(context))
        }
    }

    companion object {
        fun parseLinear(context: ParserContext, input: Parser): Result<GradientKind, ParseError> {
            val result = input.tryParse { nestedParser -> LineDirection.parse(context, nestedParser) }

            val direction = if (result is Ok) {
                val comma = input.expectComma()

                if (comma is Err) {
                    return comma
                }

                result.value
            } else {
                LineDirection.Vertical(Y.Top)
            }

            return Ok(Linear(direction))
        }

        fun parseRadial(context: ParserContext, input: Parser): Result<GradientKind, ParseError> {
            val shapeResult = input.tryParse { nestedParser -> EndingShape.parse(context, nestedParser) }

            val positionResult = input.tryParse { nestedParser ->
                val atIdent = nestedParser.expectIdentifierMatching("at")

                if (atIdent is Err) {
                    return atIdent
                }

                Position.parse(context, nestedParser)
            }.ok()

            if (shapeResult.isOk() || positionResult.isSome()) {
                val comma = input.expectComma()

                if (comma is Err) {
                    return comma
                }
            }

            val shape = shapeResult.unwrapOrElse { EndingShape.Ellipse(Ellipse.Extend(ShapeExtend.FarthestCorner)) }

            val position = positionResult.unwrapOr() { Position.center() }

            return Ok(Radial(shape, position))
        }
    }
}

private typealias ImageAngle = Angle

sealed class LineDirection : SpecifiedValue<ComputedLineDirection> {

    data class Angle(val angle: ImageAngle) : LineDirection()

    data class Horizontal(val x: X) : LineDirection()

    data class Vertical(val y: Y) : LineDirection()

    data class Corner(val x: X, val y: Y) : LineDirection()

    override fun toComputedValue(context: Context): ComputedLineDirection {
        return when (this) {
            is LineDirection.Angle -> ComputedLineDirection.Angle(angle.toComputedValue(context))
            is LineDirection.Horizontal -> ComputedLineDirection.Horizontal(x)
            is LineDirection.Vertical -> ComputedLineDirection.Vertical(y)
            is LineDirection.Corner -> ComputedLineDirection.Corner(x, y)
        }
    }

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<LineDirection, ParseError> {
            val angle = input.tryParse { nestedParser -> org.fernice.flare.style.value.specified.Angle.parseAllowingUnitless(context, nestedParser) }

            if (angle is Ok) {
                return Ok(Angle(angle.value))
            }

            return input.tryParse { nestedParser ->
                val toIdent = nestedParser.expectIdentifierMatching("to")

                if (toIdent is Err) {
                    return toIdent
                }

                val x = nestedParser.tryParse(X.Companion::parse)

                if (x is Ok) {
                    val y = nestedParser.tryParse(Y.Companion::parse)

                    if (y is Ok) {
                        return Ok(Corner(x.value, y.value))
                    }

                    return Ok(Horizontal(x.value))
                }

                val y = Y.parse(nestedParser)

                return when (y) {
                    is Ok -> {
                        val lateX = nestedParser.tryParse(X.Companion::parse)

                        if (lateX is Ok) {
                            return Ok(Corner(lateX.value, y.value))
                        }

                        Ok(Vertical(y.value))
                    }
                    is Err -> y
                }
            }
        }
    }
}

sealed class EndingShape : SpecifiedValue<ComputedEndingShape> {

    data class Circle(val circle: SpecifiedCircle) : EndingShape()

    data class Ellipse(val ellipse: SpecifiedEllipse) : EndingShape()

    override fun toComputedValue(context: Context): ComputedEndingShape {
        return when (this) {
            is EndingShape.Circle -> ComputedEndingShape.Circle(circle.toComputedValue(context))
            is EndingShape.Ellipse -> ComputedEndingShape.Ellipse(ellipse.toComputedValue(context))
        }
    }

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<EndingShape, ParseError> {
            val extend = input.tryParse(ShapeExtend.Companion::parse)

            if (extend is Ok) {
                val circleIdent = input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("circle") }

                if (circleIdent is Ok) {
                    return Ok(Circle(org.fernice.flare.style.value.specified.Circle.Extend(extend.value)))
                }

                input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("ellipse") }

                return Ok(Ellipse(org.fernice.flare.style.value.specified.Ellipse.Extend(extend.value)))
            }

            val circleIdent = input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("circle") }

            if (circleIdent is Ok) {
                val lateExtend = input.tryParse(ShapeExtend.Companion::parse)

                if (lateExtend is Ok) {
                    return Ok(Circle(org.fernice.flare.style.value.specified.Circle.Extend(lateExtend.value)))
                }

                val length = input.tryParse { nestedParser -> Length.parse(context, nestedParser) }

                if (length is Ok) {
                    return Ok(Circle(org.fernice.flare.style.value.specified.Circle.Radius(length.value)))
                }

                return Ok(Circle(org.fernice.flare.style.value.specified.Circle.Extend(ShapeExtend.FarthestCorner)))
            }

            val ellipseIdent = input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("ellipse") }

            if (ellipseIdent is Ok) {
                val lateExtend = input.tryParse(ShapeExtend.Companion::parse)

                if (lateExtend is Ok) {
                    return Ok(Ellipse(org.fernice.flare.style.value.specified.Ellipse.Extend(lateExtend.value)))
                }

                val pair = input.tryParse<Pair<LengthOrPercentage, LengthOrPercentage>> { nestedParser ->
                    val xResult = LengthOrPercentage.parse(context, nestedParser)

                    val x = when (xResult) {
                        is Ok -> xResult.value
                        is Err -> return@tryParse xResult
                    }

                    val yResult = LengthOrPercentage.parse(context, nestedParser)

                    val y = when (yResult) {
                        is Ok -> yResult.value
                        is Err -> return@tryParse yResult
                    }

                    Ok(Pair(x, y))
                }

                if (pair is Ok) {
                    val (x, y) = pair.value

                    return Ok(Ellipse(org.fernice.flare.style.value.specified.Ellipse.Radii(x, y)))
                }

                return Ok(Ellipse(org.fernice.flare.style.value.specified.Ellipse.Extend(ShapeExtend.FarthestCorner)))
            }

            return input.tryParse<EndingShape> { nestedParser ->
                val xResult = Percentage.parse(context, nestedParser)

                val x = when (xResult) {
                    is Ok -> xResult.value
                    is Err -> return@tryParse xResult
                }

                val yResult = nestedParser.tryParse { parser -> LengthOrPercentage.parse(context, parser) }

                val y = if (yResult is Ok) {
                    input.tryParse { parser -> parser.expectIdentifierMatching("ellipse") }

                    yResult.value
                } else {
                    input.tryParse { parser -> parser.expectIdentifierMatching("ellipse") }

                    val lateYResult = nestedParser.tryParse { parser -> LengthOrPercentage.parse(context, parser) }

                    when (lateYResult) {
                        is Ok -> lateYResult.value
                        is Err -> return@tryParse lateYResult
                    }
                }

                Ok(Ellipse(org.fernice.flare.style.value.specified.Ellipse.Radii(x.intoLengthOrPercentage(), y)))
            }
        }
    }
}

private typealias SpecifiedCircle = Circle

sealed class Circle : SpecifiedValue<ComputedCircle> {

    data class Radius(val length: Length) : Circle()

    data class Extend(val shapeExtend: ShapeExtend) : Circle()

    override fun toComputedValue(context: Context): ComputedCircle {
        return when (this) {
            is Circle.Radius -> ComputedCircle.Radius(length.toComputedValue(context))
            is Circle.Extend -> ComputedCircle.Extend(shapeExtend)
        }
    }
}

private typealias  SpecifiedEllipse = Ellipse

sealed class Ellipse : SpecifiedValue<ComputedEllipse> {

    data class Radii(val horizontal: LengthOrPercentage, val vertical: LengthOrPercentage) : Ellipse()

    data class Extend(val shapeExtend: ShapeExtend) : Ellipse()

    override fun toComputedValue(context: Context): ComputedEllipse {
        return when (this) {
            is Ellipse.Radii -> ComputedEllipse.Radii(horizontal.toComputedValue(context), vertical.toComputedValue(context))
            is Ellipse.Extend -> ComputedEllipse.Extend(shapeExtend)
        }
    }
}

sealed class ShapeExtend {

    object ClosestSide : ShapeExtend()

    object FarthestSide : ShapeExtend()

    object ClosestCorner : ShapeExtend()

    object FarthestCorner : ShapeExtend()

    companion object {

        fun parse(input: Parser): Result<ShapeExtend, ParseError> {
            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "closest-side" -> Ok(ClosestSide)
                "farthest-side" -> Ok(FarthestSide)
                "closest-corner" -> Ok(ClosestCorner)
                "farthest-corner" -> Ok(FarthestCorner)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}
