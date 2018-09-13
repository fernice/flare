/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.style.parser.ClampingMode
import org.fernice.flare.style.value.ComputedValue
import fernice.std.None
import fernice.std.Option
import fernice.std.Some
import fernice.std.map

data class CalcLengthOrPercentage(val clampingMode: ClampingMode,
                             val length: PixelLength,
                             val percentage: Option<Percentage>) : ComputedValue {

    fun length(): PixelLength {
        if (percentage is Some) {
            throw IllegalStateException()
        }

        return lengthComponent()
    }

    fun lengthComponent(): PixelLength {
        return PixelLength((clampingMode.clamp(length.px())))
    }

    fun unclampedLength(): PixelLength {
        return length
    }

    fun toUsedValue(containingLength: Option<Au>): Option<Au> {
        return toPixelLength(containingLength).map(Au.Companion::from)
    }

    fun toPixelLength(containingLength: Option<Au>): Option<PixelLength> {
        return if (percentage is Some && containingLength is Some) {
            val value = length.px() + containingLength.value.scaleBy(percentage.value.value).toFloat()

            Some(PixelLength(clampingMode.clamp(value)))
        } else if (percentage.isNone()) {
            Some(length)
        } else {
            None
        }
    }

    companion object {

        fun new(length: Length, percentage: Option<Percentage>): CalcLengthOrPercentage {
            return withClampingMode(length, percentage, ClampingMode.All)
        }

        fun withClampingMode(
                length: Length,
                percentage: Option<Percentage>,
                clampingMode: ClampingMode
        ): CalcLengthOrPercentage {
            return CalcLengthOrPercentage(
                    clampingMode,
                    length,
                    percentage
            )
        }
    }
}
