/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.dom

import org.fernice.flare.selector.NamespaceUrl
import org.fernice.flare.selector.NonTSPseudoClass
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.style.ComputedValues
import org.fernice.flare.style.PerPseudoElementMap
import org.fernice.flare.style.ResolvedElementStyles
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import fernice.std.Option
import fernice.std.Some
import fernice.std.unwrap

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
