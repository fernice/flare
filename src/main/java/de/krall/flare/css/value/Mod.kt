package de.krall.flare.css.value

import de.krall.flare.css.StyleBuilder
import de.krall.flare.css.value.computed.Au
import de.krall.flare.css.value.generic.Size2D

class Context(val root: Boolean,
              val builder: StyleBuilder) {

    fun viewportSizeForViewportUnitResolution(): Size2D<Au> {
        return Size2D(Au(0), Au(0))
    }
}

sealed class FontBaseSize {

    abstract fun resolve(context: Context): Au

    class CurrentStyle : FontBaseSize() {
        override fun resolve(context: Context): Au {
            return Au(0)
        }
    }
}

interface SpecifiedValue<C : ComputedValue> {

    fun toComputedValue(context: Context): C
}

interface ComputedValue