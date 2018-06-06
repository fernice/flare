package de.krall.flare.dom

import de.krall.flare.selector.NamespaceUrl
import de.krall.flare.selector.NonTSPseudoClass
import de.krall.flare.selector.PseudoElement
import de.krall.flare.std.Option

interface Element {

    fun namespace(): NamespaceUrl

    fun localName(): String

    fun id(): Option<String>

    fun hasID(id: String): Boolean

    fun classes(): List<String>

    fun hasClass(id: String): Boolean

    fun matchPseudoElement(pseudoElement: PseudoElement): Boolean

    fun matchNonTSPseudoClass(pseudoClass: NonTSPseudoClass): Boolean

    fun isRoot(): Boolean

    fun isEmpty(): Boolean

    fun previousSibling(): Option<Element>

    fun laterSibling(): Option<Element>

    fun parent(): Option<Element>

    /**
     * Returns the owner of this element. This is the case for pseudo elements.
     */
    fun owner(): Option<Element>
}