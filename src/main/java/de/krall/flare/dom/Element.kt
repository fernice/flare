package de.krall.flare.dom

import de.krall.flare.selector.NamespaceUrl
import de.krall.flare.selector.NonTSPseudoClass
import de.krall.flare.selector.PseudoElement
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.krall.flare.std.unwrap
import de.krall.flare.style.ComputedValues
import de.krall.flare.style.PerPseudoElementMap
import de.krall.flare.style.ResolvedElementStyles
import de.krall.flare.style.context.StyleContext
import de.krall.flare.style.properties.PropertyDeclarationBlock

interface Element {

    fun namespace(): Option<NamespaceUrl>

    fun localName(): String

    fun id(): Option<String>

    fun hasID(id: String): Boolean

    fun classes(): List<String>

    fun hasClass(styleClass: String): Boolean

    fun matchPseudoElement(pseudoElement: PseudoElement): Boolean

    fun matchNonTSPseudoClass(pseudoClass: NonTSPseudoClass): Boolean

    fun isRoot(): Boolean

    fun isEmpty(): Boolean

    fun parent(): Option<Element>

    /**
     * Returns the owner of this element. This is the case for pseudos elements.
     */
    fun owner(): Option<Element>

    fun traversalParent(): Option<Element>

    fun inheritanceParent(): Option<Element>

    fun previousSibling(): Option<Element>

    fun nextSibling(): Option<Element>

    fun children(): List<Element>

    fun styleAttribute(): Option<PropertyDeclarationBlock>

    fun pseudoElement(): Option<PseudoElement>

    fun ensureData(): ElementData

    fun getData(): Option<ElementData>

    fun clearData()

    fun finishRestyle(context: StyleContext, data: ElementData, elementStyles: ResolvedElementStyles)
}

class ElementData(var styles: ElementStyles) {

    fun setStyles(resolvedStyles: ResolvedElementStyles): ElementStyles {
        val oldStyles = styles

        styles = resolvedStyles.into()

        return oldStyles
    }

    fun hasStyles(): Boolean {
        return styles.primary.isSome()
    }
}

class ElementStyles(val primary: Option<ComputedValues>,
                    val pseudos: PerPseudoElementMap<ComputedValues>) {

    /**
     * Returns the primary style, panics if unavailable.
     */
    fun primary(): ComputedValues {
        return primary.unwrap()
    }
}

fun ResolvedElementStyles.into(): ElementStyles {
    return ElementStyles(
            Some(this.primary.style()),
            this.pseudos
    )
}