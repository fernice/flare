/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare

import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.style.ElementStyleResolver
import org.fernice.flare.style.Stylist
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.parser.QuirksMode

class Engine(
        val device: Device,
        val shared: SharedEngine
) {

    private fun createEngineContext(): EngineContext {
        return EngineContext(
                StyleContext.new(
                        device,
                        shared.stylist,
                        shared.fontMetricsProvider
                )
        )
    }

    fun applyStyles(element: Element) {
        val context = createEngineContext()

        applyStyles(element, context)
    }

    private fun applyStyles(element: Element, context: EngineContext) {
        applyStyle(element, context)

        for (child in element.children()) {
            applyStyles(child, context)
        }
    }

    private fun applyStyle(element: Element, context: EngineContext) {
        context.styleContext.bloomFilter.insertParent(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)

        val styles = styleResolver.resolvePrimaryStyleWithDefaultParentStyles()

        val data = element.ensureData()

        element.finishRestyle(context.styleContext, data, styles)
    }
}

class SharedEngine(
        val stylist: Stylist,
        val fontMetricsProvider: FontMetricsProvider
) {

    companion object {
        fun new(fontMetricsProvider: FontMetricsProvider): SharedEngine {
            return SharedEngine(
                    Stylist.new(QuirksMode.NO_QUIRKS),
                    fontMetricsProvider
            )
        }
    }
}


class EngineContext(
        val styleContext: StyleContext
)
