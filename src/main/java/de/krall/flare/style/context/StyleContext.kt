package de.krall.flare.style.context

import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.style.StyleBloom
import de.krall.flare.style.Stylist

class StyleContext(
        val stylist: Stylist,
        val bloomFilter: StyleBloom,
        val fontMetricsProvider: FontMetricsProvider
) {

    companion object {
        fun new(stylist: Stylist,
                fontMetricsProvider: FontMetricsProvider): StyleContext {
            return StyleContext(
                    stylist,
                    StyleBloom.new(),
                    fontMetricsProvider
            )
        }
    }
}