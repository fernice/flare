package de.krall.flare.css

import de.krall.flare.css.properties.stylestruct.Background
import de.krall.flare.css.properties.stylestruct.Font

data class ComputedValues(private val background: Background,
                          private val font: Font) {

    fun getBackground(): Background {
        return background
    }

    fun getFont(): Font {
        return font
    }

    companion object {

        val initial: ComputedValues by lazy {
            ComputedValues(
                    Background.initial,
                    Font.initial
            )
        }
    }
}