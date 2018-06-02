package de.krall.flare.css.value.computed

import de.krall.flare.css.value.ComputedValue

sealed class Color : ComputedValue {

    class RGBA(val rgba: de.krall.flare.cssparser.RGBA) : Color()

    class CurrentColor : Color()

    companion object {

        private val transparent: Color by lazy { Color.RGBA(de.krall.flare.cssparser.RGBA(0, 0, 0, 0)) }

        fun transparent(): Color {
            return transparent
        }
    }
}