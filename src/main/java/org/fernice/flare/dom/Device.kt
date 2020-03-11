/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.dom

import org.fernice.flare.style.ComputedValues
import org.fernice.flare.style.value.computed.Au
import org.fernice.flare.style.value.generic.Size2D

interface Device {

    fun defaultComputedValues(): ComputedValues {
        return ComputedValues.initial
    }

    val viewportSize: Size2D<Au>
    var rootFontSize: Au

    fun invalidate()
}
