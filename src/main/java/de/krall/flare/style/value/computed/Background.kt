package de.krall.flare.style.value.computed

import de.krall.flare.style.value.ComputedValue
import de.krall.flare.style.value.specified.BackgroundRepeatKeyword

sealed class BackgroundSize : ComputedValue {

    class Explicit(val width: NonNegativeLengthOrPercentageOrAuto, val height: NonNegativeLengthOrPercentageOrAuto) : BackgroundSize()

    class Cover : BackgroundSize()

    class Contain : BackgroundSize()

    companion object  {

        fun auto(): BackgroundSize {
            return BackgroundSize.Explicit(
                    NonNegativeLengthOrPercentageOrAuto.auto(),
                    NonNegativeLengthOrPercentageOrAuto.auto()
            )
        }
    }
}

class BackgroundRepeat(val horizontal: BackgroundRepeatKeyword, val vertical: BackgroundRepeatKeyword) : ComputedValue {

    companion object {
        fun repeat(): BackgroundRepeat {
            return BackgroundRepeat(
                    BackgroundRepeatKeyword.Repeat,
                    BackgroundRepeatKeyword.Repeat
            )
        }
    }
}
