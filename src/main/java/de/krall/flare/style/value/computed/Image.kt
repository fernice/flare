package de.krall.flare.style.value.computed

import de.krall.flare.std.Option
import de.krall.flare.style.value.specified.X
import de.krall.flare.style.value.specified.Y

sealed class Image {

    class Url(val url: ComputedUrl) : Image()

    class Gradient(val gradient: ImageGradient) : Image()
}

private typealias ImageGradient = Gradient

typealias Repeating = Boolean

class Gradient(
        val items: List<GradientItem>,
        val repeating: Repeating,
        val kind: GradientKind
)

sealed class GradientItem {

    class InterpolationHint(val hint: LengthOrPercentage) : GradientItem()

    class ColorStop(val colorStop: de.krall.flare.style.value.specified.ColorStop) : GradientItem()
}

class ColorStop(
        val color: RGBAColor,
        val position: Option<LengthOrPercentage>
)

sealed class GradientKind {

    class Linear(val lineDirection: LineDirection) : GradientKind()

    class Radial(val endingShape: EndingShape, val position: Position) : GradientKind()
}

private typealias OuterAngle = Angle

sealed class LineDirection {

    class Angle(val angle: OuterAngle) : LineDirection()

    class Horizontal(val x: X) : LineDirection()

    class Vertical(val y: Y) : LineDirection()

    class Corner(val x: X, val y: Y) : LineDirection()
}

sealed class EndingShape {

    class Circle(val circle: de.krall.flare.style.value.specified.Circle) : EndingShape()

    class Ellipse(val ellipse: de.krall.flare.style.value.specified.Ellipse) : EndingShape()
}

sealed class Circle {

    class Radius(val length: PixelLength) : Circle()

    class Extend(val shapeExtend: ShapeExtend) : Circle()
}

sealed class Ellipse {

    class Radii(val horizontal: LengthOrPercentage, val vertical: LengthOrPercentage) : Ellipse()

    class Extend(val shapeExtend: ShapeExtend) : Ellipse()
}

enum class ShapeExtend {

    ClosestSide,

    FarthestSide,

    ClosestCorner,

    FarthestCorner;
}
