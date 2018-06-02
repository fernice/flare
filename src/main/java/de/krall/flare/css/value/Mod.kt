package de.krall.flare.css.value

import de.krall.flare.css.StyleBuilder
import de.krall.flare.css.value.computed.Au
import de.krall.flare.css.value.generic.Size2D
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
                    .getFontSize()
                    .size()
        }
    }

    class InheritStyle : FontBaseSize() {
        override fun resolve(context: Context): Au {
            return context.style()
                    .getParentFont()
                    .getFontSize()
                    .size()
        }
    }

    class InheritStyleButStripEmUnits : FontBaseSize() {
        override fun resolve(context: Context): Au {
            return context.style()
                    .getParentFont()
                    .getFontSize()
                    .size()
        }
    }
}

interface SpecifiedValue<C : ComputedValue> {

    fun toComputedValue(context: Context): C
}

interface ComputedValue