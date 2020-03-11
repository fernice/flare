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
import org.fernice.flare.style.parser.QuirksMode

class Engine(
    val device: Device,
    private val shared: SharedEngine
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
    private val fontMetricsProvider: FontMetricsProvider
) {

    companion object {
        fun new(fontMetricsProvider: FontMetricsProvider): SharedEngine {
            return SharedEngine(
                Stylist.new(QuirksMode.NO_QUIRKS),
                fontMetricsProvider
            )
        }
    }

    fun createEngineContext(device: Device): EngineContext {
        return EngineContextImpl(
            StyleContext.new(
                device,
                stylist,
                fontMetricsProvider
            )
        )
    }

    fun style(device: Device, element: Element) {
        val context = EngineContextImpl(
            StyleContext.new(
                device,
                stylist,
                fontMetricsProvider
            )
        )

        style(element, context)
    }

    private fun style(element: Element, context: EngineContext) {
        styleInternal(element, context)

        for (child in element.children) {
            style(child, context)
        }
    }

    private fun styleInternal(element: Element, context: EngineContext) {
        context.styleContext.bloomFilter.insertParent(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)
        val styles = styleResolver.resolvePrimaryStyleWithDefaultParentStyles()

        val data = element.getData()

        element.finishRestyle(context.styleContext, data, styles)
    }

    fun matchStyle(device: Device, element: Element): MatchingResult {
        val context = EngineContextImpl(
            StyleContext.new(
                device,
                stylist,
                fontMetricsProvider
            )
        )

        context.styleContext.bloomFilter.insertParent(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)

        return styleResolver.matchPrimary()
    }
}

interface EngineContext {
    val styleContext: StyleContext
}

private class EngineContextImpl(
    override val styleContext: StyleContext
) : EngineContext
