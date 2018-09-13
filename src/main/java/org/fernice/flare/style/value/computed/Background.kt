/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.style.value.ComputedValue
import org.fernice.flare.style.value.specified.BackgroundRepeatKeyword

sealed class BackgroundSize : ComputedValue {

    data class Explicit(val width: NonNegativeLengthOrPercentageOrAuto, val height: NonNegativeLengthOrPercentageOrAuto) : BackgroundSize()

    object Cover : BackgroundSize()

    object Contain : BackgroundSize()

    companion object {

        fun auto(): BackgroundSize {
            return BackgroundSize.Explicit(
                    NonNegativeLengthOrPercentageOrAuto.auto(),
                    NonNegativeLengthOrPercentageOrAuto.auto()
            )
        }
    }
}

data class BackgroundRepeat(val horizontal: BackgroundRepeatKeyword, val vertical: BackgroundRepeatKeyword) : ComputedValue {

    companion object {
        fun repeat(): BackgroundRepeat {
            return BackgroundRepeat(
                    BackgroundRepeatKeyword.Repeat,
                    BackgroundRepeatKeyword.Repeat
            )
        }
    }
}
