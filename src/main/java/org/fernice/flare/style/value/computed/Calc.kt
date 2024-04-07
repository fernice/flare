/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.style.ClampingMode
import org.fernice.flare.style.value.ComputedValue

data class CalcLengthOrPercentage(
    val clampingMode: ClampingMode,
    val length: PixelLength,
    val percentage: Percentage?,
) : ComputedValue {

    fun length(): PixelLength {
        if (percentage == null) error("has percentage portion")

        return lengthComponent()
    }

    fun lengthComponent(): PixelLength {
        return PixelLength((clampingMode.clamp(length.px())))
    }

    fun unclampedLength(): PixelLength {
        return length
    }

    fun toUsedValue(containingLength: Au?): Au? {
        return toPixelLength(containingLength)?.let(Au.Companion::from)
    }

    fun toPixelLength(containingLength: Au?): PixelLength? {
        return if (percentage != null && containingLength != null) {
            val value = length.px() + containingLength.scaleBy(percentage.value).toFloat()

            PixelLength(clampingMode.clamp(value))
        } else if (percentage == null) {
            length
        } else {
            null
        }
    }

    companion object {

        fun new(length: Length, percentage: Percentage?): CalcLengthOrPercentage {
            return withClampingMode(length, percentage, ClampingMode.All)
        }

        fun withClampingMode(
            length: Length,
            percentage: Percentage?,
            clampingMode: ClampingMode,
        ): CalcLengthOrPercentage {
            return CalcLengthOrPercentage(
                clampingMode,
                length,
                percentage
            )
        }
    }
}
