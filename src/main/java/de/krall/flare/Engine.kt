package de.krall.flare

import de.krall.flare.dom.Device
import de.krall.flare.dom.Element
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.style.ElementStyleResolver
import de.krall.flare.style.Stylist
import de.krall.flare.style.context.StyleContext
import de.krall.flare.style.parser.QuirksMode

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