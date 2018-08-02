package de.krall.flare.style.value

import de.krall.flare.style.StyleBuilder
import de.krall.flare.style.value.computed.Au
import de.krall.flare.style.value.generic.Size2D
import de.krall.flare.dom.Device
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some

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

/**
 * Marks a value to has a computed representation for itself. Whereas the specified value should be
 * designed in a way that the original input can be restored, converting a specified value into
 * a computed value may cause a loss in unnecessary information.
 */
interface SpecifiedValue<C> {

    /**
     * Turns this object into its computed representation.
     */
    fun toComputedValue(context: Context): C
}

/**
 * Marks a value that has specified representation for itself. A computed value is a concrete from
 * of the generic specified value specific to the context it has been created with.
 */
interface ComputedValue

fun <E : SpecifiedValue<C>, C> List<E>.toComputedValue(context: Context): List<C> {
    return this.map { item -> item.toComputedValue(context) }
}

fun <E : SpecifiedValue<C>, C> Option<E>.toComputedValue(context: Context): Option<C> {
    return when (this) {
        is Some -> Some(this.value.toComputedValue(context))
        is None -> None()
    }
}