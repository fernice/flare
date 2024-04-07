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
import org.fernice.flare.style.MatchingResult
import org.fernice.flare.style.Stylist
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.QuirksMode

class Engine(
    val device: Device,
    private val shared: SharedEngine,
) {
    fun createEngineContext(): EngineContext {
        return shared.createEngineContext(device)
    }

    fun style(element: Element) {
        shared.style(device, element)
    }

    fun matchStyles(element: Element): MatchingResult {
        return shared.matchStyle(device, element)
    }
}

class SharedEngine(
    val stylist: Stylist,
    private val fontMetricsProvider: FontMetricsProvider,
) {

    companion object {
        fun new(fontMetricsProvider: FontMetricsProvider): SharedEngine {
            return SharedEngine(
                Stylist(QuirksMode.NoQuirks),
                fontMetricsProvider
            )
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

    fun style(device: Device, element: Element) {
        val context = createEngineContext(device)

        style(element, context)
    }

    private fun style(element: Element, context: EngineContext) {
        applyStyles(element, context)

        for (child in element.children) {
            style(child, context)
        }
    }

    private fun applyStyles(element: Element, context: EngineContext) {
        context.styleContext.prepare(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)
        val styles = styleResolver.resolveStyleWithDefaultParentStyles()

        val previousStyles = element.styles

        element.finishRestyle(context.styleContext, previousStyles, styles)
    }

    fun matchStyle(device: Device, element: Element): MatchingResult {
        val context = createEngineContext(device)

        context.styleContext.prepare(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)

        return styleResolver.matchPrimaryStyle()
    }
}

interface EngineContext {
    val styleContext: StyleContext
}

private class EngineContextImpl(
    override val styleContext: StyleContext,
) : EngineContext
