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

interface Element {

    val namespace: NamespaceUrl?
    val localName: String
    val id: String?
    val classes: Set<String>

    fun hasID(id: String): Boolean
    fun hasClass(styleClass: String): Boolean

    fun matchPseudoElement(pseudoElement: PseudoElement): Boolean
    fun matchNonTSPseudoClass(pseudoClass: NonTSPseudoClass): Boolean

    fun isRoot(): Boolean
    fun isEmpty(): Boolean

    /**
     * Returns the owner of this element. This is the case for pseudos elements.
     */
    val owner: Element?

    val parent: Element?
    val traversalParent: Element?
    val inheritanceParent: Element?

    val previousSibling: Element?
    val nextSibling: Element?
    val children: List<Element>

    val styleAttribute: PropertyDeclarationBlock?
    val pseudoElement: PseudoElement?

    fun getData(): ElementData

    fun getDataOrNull(): ElementData?

    fun clearData()

    fun finishRestyle(context: StyleContext, data: ElementData, elementStyles: ResolvedElementStyles)
}

class ElementData(var styles: ElementStyles) {

    fun setStyles(resolvedStyles: ResolvedElementStyles): ElementStyles {
        val oldStyles = styles

        styles = resolvedStyles.toElementStyles()

        return oldStyles
    }

    fun hasStyles(): Boolean {
        return styles.primary != null
    }
}

class ElementStyles(val primary: ComputedValues?, val pseudos: PerPseudoElementMap<ComputedValues>) {

    /**
     * Returns the primary style, panics if unavailable.
     */
    fun primary(): ComputedValues {
        return primary ?: error("expected primary styles to be present")
    }
}

fun ResolvedElementStyles.toElementStyles(): ElementStyles {
    return ElementStyles(
        this.primary.style(),
        this.pseudos
    )
}
