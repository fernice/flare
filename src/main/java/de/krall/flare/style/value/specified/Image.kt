package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Err
import de.krall.flare.std.None
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result
import de.krall.flare.std.Some
import de.krall.flare.std.unwrapOr
import de.krall.flare.std.unwrapOrElse
import de.krall.flare.style.parser.ParserContext

sealed class Image {

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<Image, ParseError> {
            val url = input.tryParse { nestedParser -> ImageUrl.parse(context, nestedParser) }

            if (url is Ok) {
                return Ok(Image.Url(url.value))
            }

            val gradient = ImageGradient.parse(context, input)

            return when (gradient) {
                is Ok -> Ok(Image.Gradient(gradient.value))
                is Err -> gradient
            }
        }
    }

    class Url(url: ImageUrl) : Image()

    class Gradient(gradient: ImageGradient) : Image()
}

private typealias ImageGradient = Gradient

typealias Repeating = Boolean

class Gradient(val items: List<GradientItem>,
               val repeating: Repeating,
               val kind: GradientKind) {

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
                else -> None<Pair<Shape, Boolean>>()
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
                return Err(input.newError(ParseErrorKind.Unkown()))
            }

            return Ok(Gradient(
                    items,
                    repeating,
                    kind
            ))
        }
    }
}

sealed class GradientItem {

    companion object {
        fun parseCommaSeparated(context: ParserContext, input: Parser): Result<List<GradientItem>, ParseError> {
            var seenStop = false
            val itemsResult = input.parseCommaSeparated parse@{ nestedParser ->
                if (seenStop) {
                    val hint = nestedParser.tryParse { parser -> LengthOrPercentage.parse(context, parser) }

                    if (hint is Ok) {
                        seenStop = false
                        return@parse Ok(GradientItem.InterpolationHint(hint.value))
                    }
                }

                seenStop = true
                de.krall.flare.style.value.specified.ColorStop.parse(context, nestedParser).map(GradientItem::ColorStop)
            }

            val items = when (itemsResult) {
                is Ok -> itemsResult.value
                is Err -> return itemsResult
            }

            if (!seenStop || items.size < 2) {
                return Err(input.newError(ParseErrorKind.Unkown()))
            }

            return Ok(items)
        }
    }

    class InterpolationHint(hint: LengthOrPercentage) : GradientItem()

    class ColorStop(colorStop: de.krall.flare.style.value.specified.ColorStop) : GradientItem()
}

class ColorStop(val color: RGBAColor,
                val position: Option<LengthOrPercentage>) {

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

sealed class GradientKind {

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

            return Ok(GradientKind.Linear(direction))
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

            val position = positionResult.unwrapOr { Position.center() }

            return Ok(GradientKind.Radial(shape, position))
        }
    }

    class Linear(lineDirection: LineDirection) : GradientKind()

    class Radial(endingShape: EndingShape, position: Position) : GradientKind()
}

private typealias OuterAngle = Angle

sealed class LineDirection {

    companion object {
        fun parse(context: ParserContext, input: Parser): Result<LineDirection, ParseError> {
            val angle = input.tryParse { nestedParser -> de.krall.flare.style.value.specified.Angle.parseAllowingUnitless(context, nestedParser) }

            if (angle is Ok) {
                return Ok(LineDirection.Angle(angle.value))
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
                        return Ok(LineDirection.Corner(x.value, y.value))
                    }

                    return Ok(LineDirection.Horizontal(x.value))
                }

                val y = Y.parse(nestedParser)

                return when (y) {
                    is Ok -> {
                        val lateX = nestedParser.tryParse(X.Companion::parse)

                        if (lateX is Ok) {
                            return Ok(LineDirection.Corner(lateX.value, y.value))
                        }

                        Ok(LineDirection.Vertical(y.value))
                    }
                    is Err -> y
                }
            }
        }
    }

    class Angle(angle: OuterAngle) : LineDirection()

    class Horizontal(x: X) : LineDirection()

    class Vertical(y: Y) : LineDirection()

    class Corner(x: X, y: Y) : LineDirection()
}

sealed class EndingShape {

    companion object {

        fun parse(context: ParserContext, input: Parser): Result<EndingShape, ParseError> {
            val extend = input.tryParse(ShapeExtend.Companion::parse)

            if (extend is Ok) {
                val circleIdent = input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("circle") }

                if (circleIdent is Ok) {
                    return Ok(EndingShape.Circle(de.krall.flare.style.value.specified.Circle.Extend(extend.value)))
                }

                input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("ellipse") }

                return Ok(EndingShape.Ellipse(de.krall.flare.style.value.specified.Ellipse.Extend(extend.value)))
            }

            val circleIdent = input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("circle") }

            if (circleIdent is Ok) {
                val lateExtend = input.tryParse(ShapeExtend.Companion::parse)

                if (lateExtend is Ok) {
                    return Ok(EndingShape.Circle(de.krall.flare.style.value.specified.Circle.Extend(lateExtend.value)))
                }

                val length = input.tryParse { nestedParser -> Length.parse(context, nestedParser) }

                if (length is Ok) {
                    return Ok(EndingShape.Circle(de.krall.flare.style.value.specified.Circle.Radius(length.value)))
                }

                return Ok(EndingShape.Circle(de.krall.flare.style.value.specified.Circle.Extend(ShapeExtend.FarthestCorner)))
            }

            val ellipseIdent = input.tryParse { nestedParser -> nestedParser.expectIdentifierMatching("ellipse") }

            if (ellipseIdent is Ok) {
                val lateExtend = input.tryParse(ShapeExtend.Companion::parse)

                if (lateExtend is Ok) {
                    return Ok(EndingShape.Ellipse(de.krall.flare.style.value.specified.Ellipse.Extend(lateExtend.value)))
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

                    return Ok(EndingShape.Ellipse(de.krall.flare.style.value.specified.Ellipse.Radii(x, y)))
                }

                return Ok(EndingShape.Ellipse(de.krall.flare.style.value.specified.Ellipse.Extend(ShapeExtend.FarthestCorner)))
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

                Ok(EndingShape.Ellipse(de.krall.flare.style.value.specified.Ellipse.Radii(x.intoLengthOrPercentage(), y)))
            }
        }
    }

    class Circle(circle: de.krall.flare.style.value.specified.Circle) : EndingShape()

    class Ellipse(ellipse: de.krall.flare.style.value.specified.Ellipse) : EndingShape()
}

sealed class Circle {

    class Radius(length: Length) : Circle()

    class Extend(shapeExtend: ShapeExtend) : Circle()
}

sealed class Ellipse {

    class Radii(horizontal: LengthOrPercentage, vertical: LengthOrPercentage) : Ellipse()

    class Extend(shapeExtend: ShapeExtend) : Ellipse()
}

enum class ShapeExtend {

    ClosestSide,

    FarthestSide,

    ClosestCorner,

    FarthestCorner;

    companion object {

        fun parse(input: Parser): Result<ShapeExtend, ParseError> {
            val location = input.sourceLocation()
            val identResult = input.expectIdentifier()

            val ident = when (identResult) {
                is Ok -> identResult.value
                is Err -> return identResult
            }

            return when (ident.toLowerCase()) {
                "closest-side" -> Ok(ShapeExtend.ClosestSide)
                "farthest-side" -> Ok(ShapeExtend.FarthestSide)
                "closest-corner" -> Ok(ShapeExtend.ClosestCorner)
                "farthest-corner" -> Ok(ShapeExtend.FarthestCorner)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(ident)))
            }
        }
    }
}
