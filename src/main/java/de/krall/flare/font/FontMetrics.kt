package de.krall.flare.font

import de.krall.flare.style.properties.stylestruct.Font
import de.krall.flare.style.value.computed.Au
import de.krall.flare.dom.Device

interface FontMetricsProvider {

    fun query(font: Font,
              fontSize: Au,
              device: Device): FontMetricsQueryResult
}

sealed class FontMetricsQueryResult {

    class Available(val metrics: FontMetrics) : FontMetricsQueryResult()

    class NotAvailable : FontMetricsQueryResult()
}

class FontMetrics(val xHeight: Au,
                  val zeroAdvanceMeasure: Au)