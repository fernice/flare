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
import org.fernice.flare.style.StyleRoot
import org.fernice.flare.style.context.StyleContext
import org.fernice.flare.style.source.StyleAttribute

interface Element {

    val namespace: NamespaceUrl?
    val localName: String
    val id: String?
    val classes: Set<String>

    fun hasID(id: String): Boolean
    fun hasClass(styleClass: String): Boolean

    fun hasPseudoElement(pseudoElement: PseudoElement): Boolean
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

    val pseudoElement: PseudoElement?

    val styleAttribute: StyleAttribute?
    val styleRoot: StyleRoot?

    val styles: ElementStyles?

    fun finishRestyle(context: StyleContext, previousStyles: ElementStyles?, styles: ElementStyles)
}

data class ElementStyles(val primary: ComputedValues, val pseudos: PerPseudoElementMap<ComputedValues>)
