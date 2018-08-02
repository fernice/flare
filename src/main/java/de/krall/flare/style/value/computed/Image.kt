package de.krall.flare.style.value.computed

import de.krall.flare.std.Option
import de.krall.flare.style.value.ComputedValue
import de.krall.flare.style.value.specified.ShapeExtend
import de.krall.flare.style.value.specified.X
import de.krall.flare.style.value.specified.Y

sealed class Image : ComputedValue {

    class Url(val url: ComputedUrl) : Image()

    class Gradient(val gradient: ImageGradient) : Image()
}

private typealias ImageGradient = Gradient

typealias Repeating = Boolean

class Gradient(
        val items: List<GradientItem>,
        val repeating: Repeating,
        val kind: GradientKind
) : ComputedValue

sealed class GradientItem : ComputedValue {

    class InterpolationHint(val hint: LengthOrPercentage) : GradientItem()

    class ColorStop(val colorStop: ComputedColorStop) : GradientItem()
}

private typealias ComputedColorStop = ColorStop

class ColorStop(
        val color: RGBAColor,
        val position: Option<LengthOrPercentage>
) : ComputedValue

sealed class GradientKind : ComputedValue {

    class Linear(val lineDirection: LineDirection) : GradientKind()

    class Radial(val endingShape: EndingShape, val position: Position) : GradientKind()
}

private typealias ComputedAngle = Angle

sealed class LineDirection : ComputedValue {

    class Angle(val angle: ComputedAngle) : LineDirection()

    class Horizontal(val x: X) : LineDirection()

    class Vertical(val y: Y) : LineDirection()

    class Corner(val x: X, val y: Y) : LineDirection()
}

sealed class EndingShape : ComputedValue {

    class Circle(val circle: ComputedCircle) : EndingShape()

    class Ellipse(val ellipse: ComputedEllipse) : EndingShape()
}

private typealias ComputedCircle = Circle

sealed class Circle : ComputedValue {

    class Radius(val length: PixelLength) : Circle()

    class Extend(val shapeExtend: ShapeExtend) : Circle()
}

private typealias ComputedEllipse = Ellipse

sealed class Ellipse : ComputedValue {

    class Radii(val horizontal: LengthOrPercentage, val vertical: LengthOrPercentage) : Ellipse()

    class Extend(val shapeExtend: ShapeExtend) : Ellipse()
}

