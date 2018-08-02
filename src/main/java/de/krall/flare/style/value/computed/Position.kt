package de.krall.flare.style.value.computed

class Position(
        val horizontal: HorizontalPosition,
        val vertical: VerticalPosition
) {

    fun center(): Position {
        return Position(
                HorizontalPosition.fifty(),
                VerticalPosition.fifty()
        )
    }
}

typealias HorizontalPosition = LengthOrPercentage

typealias VerticalPosition = LengthOrPercentage

