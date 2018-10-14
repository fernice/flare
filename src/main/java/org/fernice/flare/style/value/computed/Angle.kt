/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.std.atMost
import org.fernice.flare.std.atLeast
import org.fernice.flare.style.value.ComputedValue

private const val RAD_PER_DEG = Math.PI / 180.0
private const val RAD_PER_GRAD = Math.PI / 200.0
private const val RAD_PER_TURN = Math.PI * 2.0

sealed class Angle : ComputedValue {

    data class Deg(val value: Float) : Angle()

    data class Grad(val value: Float) : Angle()

    data class Rad(val value: Float) : Angle()

    data class Turn(val value: Float) : Angle()

    fun radians(): Float {
        return radians64().toFloat()
    }

    fun radians64(): Double {
        val radians = when (this) {
            is Angle.Deg -> this.value.toDouble() * RAD_PER_DEG
            is Angle.Grad -> this.value.toDouble() * RAD_PER_GRAD
            is Angle.Turn -> this.value.toDouble() * RAD_PER_TURN
            is Angle.Rad -> this.value.toDouble()
        }

        return radians.atLeast(Double.MIN_VALUE).atMost(Double.MAX_VALUE)
    }

    fun degrees(): Float {
        return (radians64() * 360.0 / (2.0 * Math.PI)).toFloat()
    }
}
