/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.font

import org.fernice.flare.style.properties.stylestruct.Font
import org.fernice.flare.style.value.computed.Au
import org.fernice.flare.dom.Device

interface FontMetricsProvider {

    fun query(
        font: Font,
        fontSize: Au,
        device: Device
    ): FontMetricsQueryResult
}

sealed class FontMetricsQueryResult {

    class Available(val metrics: FontMetrics) : FontMetricsQueryResult()

    class NotAvailable : FontMetricsQueryResult()
}

class FontMetrics(val xHeight: Au, val zeroAdvanceMeasure: Au)
