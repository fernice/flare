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

class GradientItem {

    companion object {
        fun parseCommaSeparated(context: ParserContext, input: Parser): Result<List<GradientItem>, ParseError> {
            return Err(input.newError(ParseErrorKind.Unkown()))
        }
    }
}

sealed class GradientKind {

    companion object {
        fun parseLinear(context: ParserContext, input: Parser): Result<GradientKind, ParseError> {
            return Err(input.newError(ParseErrorKind.Unkown()))
        }

        fun parseRadial(context: ParserContext, input: Parser): Result<GradientKind, ParseError> {
            return Err(input.newError(ParseErrorKind.Unkown()))
        }
    }

    class Linear(lineDirection: LineDirection) : GradientKind()

    class Radial(endingShape: EndingShape, position: Position, angle: Option<Angle>) : GradientKind()
}

private typealias OuterAngle = Angle

sealed class LineDirection {

    class Angle(angle: OuterAngle) : LineDirection()

    class Horizontal(x: X) : LineDirection()

    class Vertical(y: Y) : LineDirection()

    class Corner(x: X, y: Y) : LineDirection()
}

sealed class EndingShape {

    class Circle(circle: OuterCircle) : EndingShape()

    class Ellipse(ellipse: OuterEllipse) : EndingShape()
}

private typealias OuterCircle = Circle

sealed class Circle {

    class Radius(length: Length) : Circle()

    class Extend(shapeExtend: ShapeExtend) : Circle()
}

private typealias OuterEllipse = Ellipse

sealed class Ellipse {

    class Radii(horizontal: LengthOrPercentage, vertical: LengthOrPercentage) : Ellipse()

    class Extend(shapeExtend: ShapeExtend) : Ellipse()
}

enum class ShapeExtend {

    ClosestSide,

    FarthestSide,

    ClosestCorner,

    FarthestCorner,

    Contain,

    Cover,
}
