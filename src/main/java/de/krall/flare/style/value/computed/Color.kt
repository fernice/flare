package de.krall.flare.style.value.computed

import de.krall.flare.cssparser.RGBA

sealed class Color {

    abstract fun toRGBA(currentColor: de.krall.flare.cssparser.RGBA): de.krall.flare.cssparser.RGBA

    class RGBA(val rgba: de.krall.flare.cssparser.RGBA) : Color() {
        override fun toRGBA(currentColor: de.krall.flare.cssparser.RGBA): de.krall.flare.cssparser.RGBA {
            return rgba
        }
    }

    class CurrentColor : Color() {
        override fun toRGBA(currentColor: de.krall.flare.cssparser.RGBA): de.krall.flare.cssparser.RGBA {
            return currentColor
        }
    }

    companion object {

        private val transparent: Color by lazy { Color.RGBA(de.krall.flare.cssparser.RGBA(0, 0, 0, 0)) }

        fun transparent(): Color {
            return transparent
        }
    }
}

typealias RGBAColor = RGBA

typealias ColorPropertyValue = RGBA