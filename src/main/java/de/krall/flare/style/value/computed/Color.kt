package de.krall.flare.style.value.computed

import de.krall.flare.cssparser.RGBA

sealed class Color {

    data class RGBA(val rgba: RGBAColor) : Color()

    object CurrentColor : Color()

    fun toRGBA(currentColor: RGBAColor): RGBAColor {
        return when (this) {
            is Color.RGBA -> rgba
            is Color.CurrentColor -> currentColor
        }
    }

    companion object {

        private val transparent: Color by lazy { Color.RGBA(RGBAColor(0, 0, 0, 0)) }

        fun transparent(): Color {
            return transparent
        }
    }
}

typealias RGBAColor = RGBA

typealias ColorPropertyValue = RGBA