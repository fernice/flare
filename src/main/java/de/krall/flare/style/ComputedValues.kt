package de.krall.flare.style

import de.krall.flare.style.properties.stylestruct.*

data class ComputedValues(val font: Font,
                          val background: Background,
                          val border: Border,
                          val margin: Margin,
                          val padding: Padding) {

    companion object {

        val initial: ComputedValues by lazy {
            ComputedValues(
                    Font.initial,
                    Background.initial,
                    Border.initial,
                    Margin.initial,
                    Padding.initial
            )
        }
    }
}