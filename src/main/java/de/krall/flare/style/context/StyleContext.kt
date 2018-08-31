package de.krall.flare.style.context

import de.krall.flare.dom.Device
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.style.StyleBloom
import de.krall.flare.style.Stylist

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