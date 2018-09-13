/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.context

import org.fernice.flare.dom.Device
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.style.StyleBloom
import org.fernice.flare.style.Stylist

class StyleContext(
        val device: Device,
        val stylist: Stylist,
        val bloomFilter: StyleBloom,
        val fontMetricsProvider: FontMetricsProvider
) {

    companion object {
        fun new(
                device: Device,
                stylist: Stylist,
                fontMetricsProvider: FontMetricsProvider
        ): StyleContext {
            return StyleContext(
                    device,
                    stylist,
                    StyleBloom.new(),
                    fontMetricsProvider
            )
        }
    }
}
