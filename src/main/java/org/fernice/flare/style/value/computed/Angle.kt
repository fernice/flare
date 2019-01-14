/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.style.value.ComputedValue

data class Angle(val value: Float) : ComputedValue {

    fun degrees(): Float {
        return value
    }

    fun radians(): Float {
        return radians64().toFloat()
    }

    fun radians64(): Double {
        return Math.toRadians(value.toDouble())
    }
}