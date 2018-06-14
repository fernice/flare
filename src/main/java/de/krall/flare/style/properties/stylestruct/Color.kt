package de.krall.flare.style.properties.stylestruct

import de.krall.flare.cssparser.RGBA
import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.value.computed.Color as ComputedColor

interface Color : StyleStruct<MutColor> {

    val color: RGBA

    override fun clone(): MutColor {
        return MutColor(
                color
        )
    }

    companion object {
        val initial: Color by lazy {
            StaticColor(
                    RGBA(0, 0, 0, 255)
            )
        }
    }
}

class StaticColor(override val color: RGBA) : Color

class MutColor(override var color: RGBA) : Color, MutStyleStruct