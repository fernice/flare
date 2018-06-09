package de.krall.flare

import de.krall.flare.dom.Device
import de.krall.flare.dom.Element
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.style.ElementStyleResolver
import de.krall.flare.style.Stylist
import de.krall.flare.style.context.StyleContext
import de.krall.flare.style.parser.QuirksMode

class Engine(val stylist: Stylist,
             val fontMetricsProvider: FontMetricsProvider) {

    companion object {
        fun from(device: Device,
                 fontMetricsProvider: FontMetricsProvider): Engine {
            return Engine(
                    Stylist.new(device, QuirksMode.NO_QUIRKS),
                    fontMetricsProvider
            )
        }
    }

    fun createEngineContext(): EngineContext {
        return EngineContext(
                StyleContext.new(stylist, fontMetricsProvider)
        )
    }

    fun applyStyles(element: Element) {
        val context = createEngineContext()

        applyStyle(element, context)

        for (child in element.children()) {
            applyStyles(child)
        }
    }

    fun applyStyle(element: Element, context: EngineContext) {
        context.styleContext.bloomFilter.insertParent(element)

        val styleResolver = ElementStyleResolver(element, context.styleContext)

        val styles = styleResolver.resolvePrimaryStyleWithDefaultParentStyles()

        element.ensureData().setStyles(styles.style)
    }
}

class EngineContext(val styleContext: StyleContext)