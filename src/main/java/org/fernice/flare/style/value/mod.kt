/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value

import org.fernice.flare.style.StyleBuilder
import org.fernice.flare.style.value.computed.Au
import org.fernice.flare.style.value.generic.Size2D
import org.fernice.flare.dom.Device
import org.fernice.flare.font.FontMetricsProvider
import fernice.std.None
import fernice.std.Option
import fernice.std.Some

class Context(
        val rootElement: Boolean,
        val builder: StyleBuilder,
        val fontMetricsProvider: FontMetricsProvider
) {

    fun isRootElement(): Boolean {
        return rootElement
    }

    fun viewportSizeForViewportUnitResolution(): Size2D<Au> {
        return builder.device.viewportSize
    }

    fun device(): Device {
        return builder.device
    }

    fun style(): StyleBuilder {
        return builder
    }
}

sealed class FontBaseSize {

    object CurrentStyle : FontBaseSize()
    object InheritStyle : FontBaseSize()
    object InheritStyleButStripEmUnits : FontBaseSize()

    fun resolve(context: Context): Au {
        return when (this) {
            is FontBaseSize.CurrentStyle -> {
                context.style()
                        .getFont()
                        .fontSize
                        .size()
            }
            is FontBaseSize.InheritStyle -> {
                context.style()
                        .getParentFont()
                        .fontSize
                        .size()
            }
            is InheritStyleButStripEmUnits -> {
                context.style()
                        .getParentFont()
                        .fontSize
                        .size()
            }
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
        is None -> None
    }
}