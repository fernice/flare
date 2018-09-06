package de.krall.flare.style.value.computed

import de.krall.flare.style.value.ComputedValue
import de.krall.flare.style.value.specified.ShapeExtend
import de.krall.flare.style.value.specified.X
import de.krall.flare.style.value.specified.Y
import modern.std.Option

sealed class Image : ComputedValue {

    data class Url(val url: ComputedUrl) : Image()

    data class Gradient(val gradient: ImageGradient) : Image()
}

private typealias ImageGradient = Gradient

typealias Repeating = Boolean

data class Gradient(
        val items: List<GradientItem>,
        val repeating: Repeating,
        val kind: GradientKind
) : ComputedValue

sealed class GradientItem : ComputedValue {

    data class InterpolationHint(val hint: LengthOrPercentage) : GradientItem()

    data class ColorStop(val colorStop: ComputedColorStop) : GradientItem()
}

private typealias ComputedColorStop = ColorStop

data class ColorStop(
        val color: RGBAColor,
        val position: Option<LengthOrPercentage>
) : ComputedValue

sealed class GradientKind : ComputedValue {

    data class Linear(val lineDirection: LineDirection) : GradientKind()

    data class Radial(val endingShape: EndingShape, val position: Position) : GradientKind()
}

private typealias ComputedAngle = Angle

sealed class LineDirection : ComputedValue {

    data class Angle(val angle: ComputedAngle) : LineDirection()

    data class Horizontal(val x: X) : LineDirection()

    data class Vertical(val y: Y) : LineDirection()

    data class Corner(val x: X, val y: Y) : LineDirection()
}

sealed class EndingShape : ComputedValue {

    data class Circle(val circle: ComputedCircle) : EndingShape()

    data class Ellipse(val ellipse: ComputedEllipse) : EndingShape()
}

private typealias ComputedCircle = Circle

sealed class Circle : ComputedValue {

    data class Radius(val length: PixelLength) : Circle()

    data class Extend(val shapeExtend: ShapeExtend) : Circle()
}

private typealias ComputedEllipse = Ellipse

sealed class Ellipse : ComputedValue {

    data class Radii(val horizontal: LengthOrPercentage, val vertical: LengthOrPercentage) : Ellipse()

    data class Extend(val shapeExtend: ShapeExtend) : Ellipse()
}

