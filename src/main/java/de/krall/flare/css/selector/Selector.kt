package de.krall.flare.css.selector

import de.krall.flare.cssparser.*
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result

interface NamespacePrefix {

    fun getPrefix(): String
}

interface NamespaceUrl {

    fun getUrl(): String
}

sealed class Component {

    class Combinator(val combinator: de.krall.flare.css.selector.Combinator) : Component()

    class DefaultNamespace(val namespace: NamespaceUrl) : Component()
    class ExplicitNoNamespace : Component()
    class ExplicitAnyNamespace : Component()
    class Namespace(val prefix: NamespacePrefix, val namespace: NamespaceUrl) : Component()

    class LocalName(val localName: String) : Component()
    class ExplicitUniversalType : Component()

    class Id(val id: String) : Component()
    class Class(val styleClass: String) : Component()

    class PseudoElement(val pseudoElement: de.krall.flare.css.selector.PseudoElement) : Component()
    class NonTSPseudoClass(val pseudoClass: de.krall.flare.css.selector.NonTSPseudoClass) : Component()

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

    class AttributeOther(val namespace: NamespaceConstraint,
                         val localName: String,
                         val localNameLower: String,
                         val opertation: AttributeSelectorOperation,
                         val neverMatches: Boolean) : Component()

    class AttributeInNoNamespaceExists(val localName: String,
                                       val localNameLower: String) : Component()

    class AttributeInNoNamespace(val localName: String,
                                 val localNameLower: String,
                                 val operator: AttributeSelectorOperator,
                                 val value: String,
                                 val caseSensitive: Boolean,
                                 val neverMatches: Boolean) : Component()
}

sealed class Combinator {

    class Child : Combinator()

    class NextSibling : Combinator()

    class LaterSibling : Combinator()

    class Descendant : Combinator()

    class PseudoElement : Combinator()
}

sealed class PseudoElement {

    class Before : PseudoElement()
    class After : PseudoElement()
    class Selection : PseudoElement()
    class FirstLetter : PseudoElement()
    class FirstLine : PseudoElement()
}

sealed class NonTSPseudoClass {

    class Active : NonTSPseudoClass()
    class Checked : NonTSPseudoClass()
    class Disabled : NonTSPseudoClass()
    class Enabled : NonTSPseudoClass()
    class Focus : NonTSPseudoClass()
    class Fullscreen : NonTSPseudoClass()
    class Hover : NonTSPseudoClass()
    class Indeterminate : NonTSPseudoClass()
    class Lang(val language: String) : NonTSPseudoClass()
    class Link : NonTSPseudoClass()
    class PlaceholderShown : NonTSPseudoClass()
    class ReadWrite : NonTSPseudoClass()
    class ReadOnly : NonTSPseudoClass()
    class Target : NonTSPseudoClass()
    class Visited : NonTSPseudoClass()
}

sealed class NamespaceConstraint {

    class Any : NamespaceConstraint()

    class Specific(val prefix: NamespacePrefix, val url: NamespaceUrl) : NamespaceConstraint()
}

sealed class AttributeSelectorOperation {

    class Exists : AttributeSelectorOperation()

    class WithValue(val operator: AttributeSelectorOperator, val caseSensitive: Boolean, val expectedValue: String) : AttributeSelectorOperation()
}

sealed class AttributeSelectorOperator {

    class Equal : AttributeSelectorOperator()
    class Includes : AttributeSelectorOperator()
    class DashMatch : AttributeSelectorOperator()
    class Prefix : AttributeSelectorOperator()
    class Substring : AttributeSelectorOperator()
    class Suffix : AttributeSelectorOperator()
}

class Selector(private val components: List<Component>) : Iterable<Component> {

    override fun iterator(): Iterator<Component> = components.iterator()
}

class SelectorList(private val selectors: List<Selector>) : Iterable<Selector> {

    override fun iterator(): Iterator<Selector> = selectors.iterator()

    companion object {

        fun parse(context: SelectorParserContext, input: Parser): Result<SelectorList, ParseError> {
            val selectors = mutableListOf<Selector>()

            lopp@
            while (true) {
                val selectorResult = input.parseUntilBefore(Delimiters.Comma, { input -> parseSelector(context, input) })

                when (selectorResult) {
                    is Ok -> selectors.add(selectorResult.value)
                    is Err -> return selectorResult
                }

                val tokenResult = input.next()

                val token = when (tokenResult) {
                    is Ok -> tokenResult.value
                    is Err -> return Ok(SelectorList(selectors))
                }

                when (token) {
                    is Token.Comma -> continue@lopp
                    else -> throw IllegalStateException("unreachable")
                }
            }
        }
    }
}