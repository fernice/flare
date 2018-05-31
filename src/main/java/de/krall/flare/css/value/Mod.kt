package de.krall.flare.css.value

import de.krall.flare.css.value.computed.Au
import de.krall.flare.css.value.generic.Size2D

class Context {

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

class ParseMode

sealed class ClampingMode {

    abstract fun isAllowed(mode: ParseMode, value: Float): Boolean

    abstract fun clamp(value: Float): Float

    class All : ClampingMode() {
        override fun isAllowed(mode: ParseMode, value: Float): Boolean {
            return true
        }

        override fun clamp(value: Float): Float {
            return value
        }
    }

    class NonNegative : ClampingMode() {
        override fun isAllowed(mode: ParseMode, value: Float): Boolean {
            return value >= 0
        }

        override fun clamp(value: Float): Float {
            return if (value >= 0) {
                value
            } else {
                0f
            }
        }
    }
}