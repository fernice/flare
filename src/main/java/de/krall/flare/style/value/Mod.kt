package de.krall.flare.style.value

import de.krall.flare.style.StyleBuilder
import de.krall.flare.style.value.computed.Au
import de.krall.flare.style.value.generic.Size2D
import de.krall.flare.dom.Device
import de.krall.flare.font.FontMetricsProvider

class Context(val rootElement: Boolean,
              val builder: StyleBuilder,
              val fontMetricsProvider: FontMetricsProvider) {

    fun isRootElement(): Boolean {
        return rootElement
    }

    fun viewportSizeForViewportUnitResolution(): Size2D<Au> {
        return builder.device.viewportSize()
    }

    fun device(): Device {
        return builder.device
    }

    fun style(): StyleBuilder {
        return builder
    }
}

sealed class FontBaseSize {

    abstract fun resolve(context: Context): Au

    class CurrentStyle : FontBaseSize() {
        override fun resolve(context: Context): Au {
            return context.style()
                    .getFont()
                    .fontSize
                    .size()
        }
    }

    class InheritStyle : FontBaseSize() {
        override fun resolve(context: Context): Au {
            return context.style()
                    .getParentFont()
                    .fontSize
                    .size()
        }
    }

    class InheritStyleButStripEmUnits : FontBaseSize() {
        override fun resolve(context: Context): Au {
            return context.style()
                    .getParentFont()
                    .fontSize
                    .size()
        }
    }
}

interface SpecifiedValue<C> {

    fun toComputedValue(context: Context): C
}

interface ComputedValue