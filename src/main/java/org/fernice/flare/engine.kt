/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare

import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.dom.ElementStyles
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.style.ElementStyleResolver
import org.fernice.flare.style.MatchingResult
import org.fernice.flare.style.Stylist
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.QuirksMode

class Engine(
    val device: Device,
    val stylist: Stylist,
    private val fontMetricsProvider: FontMetricsProvider,
) {

    companion object {
        fun new(device: Device, fontMetricsProvider: FontMetricsProvider): Engine {
            return Engine(
                device,
                Stylist(device, QuirksMode.NoQuirks),
                fontMetricsProvider
            )
        }
    }

    fun createEngineInstance(deviceFactory: (Device) -> Device): EngineInstance {
        val derivedDevice = deviceFactory(device)
        return EngineInstance(
            derivedDevice,
            this,
        )
    }

    fun restyle(device: Device, element: Element) {
        val context = createEngineContext(device)

        restyle(element, context)
    }

    private fun restyle(element: Element, context: EngineContext) {
        style(element, context)

        for (child in element.children) {
            restyle(child, context)
        }
    }

    fun createEngineContext(device: Device): EngineContext {
        return EngineContextImpl(
            StyleContext(
                device,
                stylist,
                fontMetricsProvider
            )
        )
    }

    fun style(element: Element, context: EngineContext): ElementStyles {
        context.styleContext.prepare(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)
        val styles = styleResolver.resolveStyleWithDefaultParentStyles()

        val previousStyles = element.styles

        element.finishRestyle(context.styleContext, previousStyles, styles)

        return styles
    }

    fun matchStyle(device: Device, element: Element): MatchingResult {
        val context = createEngineContext(device)

        context.styleContext.prepare(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)

        return styleResolver.matchPrimaryStyle()
    }
}

class EngineInstance(
    val device: Device,
    private val engine: Engine,
) {

    fun restyle(element: Element) {
        engine.restyle(device, element)
    }

    fun createEngineContext(): EngineContext {
        return engine.createEngineContext(device)
    }

    fun style(element: Element, context: EngineContext): ElementStyles {
        return engine.style(element, context)
    }

    fun matchStyles(element: Element): MatchingResult {
        return engine.matchStyle(device, element)
    }
}

interface EngineContext {
    val styleContext: StyleContext
}

private class EngineContextImpl(
    override val styleContext: StyleContext,
) : EngineContext
