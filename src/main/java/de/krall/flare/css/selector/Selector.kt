package de.krall.flare.css.selector

import de.krall.flare.cssparser.Nth
import de.krall.flare.css.selector.Combinator as SelectorCombinator
import de.krall.flare.css.selector.NonTSPseudoClass as SelectorNonTSPseudoClass
import de.krall.flare.css.selector.PseudoElement as SelectorPseudoElement


sealed class Component {

    class Combinator(val combinator: SelectorCombinator) : Component()

    class DefaultNamespace(val namespace: NamespaceUrl) : Component()
    class ExplicitNoNamespace : Component()
    class ExplicitAnyNamespace : Component()
    class Namespace(val prefix: NamespacePrefix, val namespace: NamespaceUrl) : Component()

    class LocalName(val localName: String) : Component()
    class ExplicitUniversalType : Component()

    class Id(val id: String) : Component()
    class Class(val styleClass: String) : Component()

    class PseudoElement(val pseudoElement: SelectorPseudoElement) : Component()
    class NonTSPseudoClass(val pseudoClass: SelectorNonTSPseudoClass) : Component()

    class Negation(val simpleSelector: List<Component>) : Component()

    class FirstChild : Component()
    class LastChild : Component()
    class OnlyChild : Component()
    class FirstOfType : Component()
    class LastOfType : Component()
    class OnlyType : Component()

    class Root : Component()
    class Empty : Component()
    class Scope : Component()
    class Host : Component()

    class NthChild(val nth: Nth) : Component()
    class NthOfType(val nth: Nth) : Component()
    class NthLastChild(val nth: Nth) : Component()
    class NthLastOfType(val nth: Nth) : Component()
}

sealed class Combinator {

    class Child : SelectorCombinator()

    class NextSibling : SelectorCombinator()

    class LaterSibling : SelectorCombinator()

    class Descendant : SelectorCombinator()

    class PseudoElement : SelectorCombinator()
}

sealed class PseudoElement {

    class Before : SelectorPseudoElement()
    class After : SelectorPseudoElement()
    class Selection : SelectorPseudoElement()
    class FirstLetter : SelectorPseudoElement()
    class FirstLine : SelectorPseudoElement()
}

sealed class NonTSPseudoClass {

    class Active : SelectorNonTSPseudoClass()
    class Checked : SelectorNonTSPseudoClass()
    class Disabled : SelectorNonTSPseudoClass()
    class Enabled : SelectorNonTSPseudoClass()
    class Focus : SelectorNonTSPseudoClass()
    class Fullscreen : SelectorNonTSPseudoClass()
    class Hover : SelectorNonTSPseudoClass()
    class Indeterminate : SelectorNonTSPseudoClass()
    class Lang(val language: String) : SelectorNonTSPseudoClass()
    class Link : SelectorNonTSPseudoClass()
    class PlaceholderShown : SelectorNonTSPseudoClass()
    class ReadWrite : SelectorNonTSPseudoClass()
    class ReadOnly : SelectorNonTSPseudoClass()
    class Target : SelectorNonTSPseudoClass()
    class Visited : SelectorNonTSPseudoClass()
}

class Selector(private val components: List<Component>) : Iterable<Component> {

    override fun iterator(): Iterator<Component> = components.iterator()
}