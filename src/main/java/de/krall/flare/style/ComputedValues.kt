package de.krall.flare.style

import de.krall.flare.style.properties.stylestruct.Background
import de.krall.flare.style.properties.stylestruct.Font

data class ComputedValues(val background: Background,
                          val font: Font) {

    companion object {

        val initial: ComputedValues by lazy {
            ComputedValues(
                    Background.initial,
                    Font.initial
            )
        }
    }
}