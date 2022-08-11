/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.selector

import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.std.Err
import org.fernice.std.Ok
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SelectorParseTest {

    @Test
    fun localName() {
        parse("element",
                expectLocalNameMatching("element"))
    }

    @Test
    fun explicitUniversalType() {
        parse("*",
                expectExplicitUniversalType())
    }

    @Test
    fun id() {
        parse("#identifier",
                expectIdMatching("identifier"))
    }

    @Test
    fun styleClass() {
        parse(".identifier",
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorDescendant() {
        parse("element .identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.Descendant::class),
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorChild() {
        parse("element > .identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.Child::class),
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorChildTight() {
        parse("element>.identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.Child::class),
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorNextSibling() {
        parse("element + .identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.NextSibling::class),
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorNextSiblingTight() {
        parse("element+.identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.NextSibling::class),
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorLaterSibling() {
        parse("element ~ .identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.LaterSibling::class),
                expectClassMatching("identifier"))
    }

    @Test
    fun combinatorLaterSiblingTight() {
        parse("element~.identifier",
                expectLocalNameMatching("element"),
                expectCombinator(Combinator.LaterSibling::class),
                expectClassMatching("identifier"))
    }

    ///////////////////////////////////// TSPseudoClass /////////////////////////////////////

    @Test
    fun pseudoClassRoot() {
        parse(":root",
                expectComponent(Component.Root::class))
    }

    @Test
    fun pseudoClassFirstChild() {
        parse(":first-child",
                expectComponent(Component.FirstChild::class))
    }

    @Test
    fun pseudoClassLastChild() {
        parse(":last-child",
                expectComponent(Component.LastChild::class))
    }

    @Test
    fun pseudoClassOnlyChild() {
        parse(":only-child",
                expectComponent(Component.OnlyChild::class))
    }

    @Test
    fun pseudoClassFirstOfType() {
        parse(":first-of-type",
                expectComponent(Component.FirstOfType::class))
    }

    @Test
    fun pseudoClassLastOfType() {
        parse(":last-of-type",
                expectComponent(Component.LastOfType::class))
    }

    @Test
    fun pseudoClassOnlyType() {
        parse(":only-of-type",
                expectComponent(Component.OnlyOfType::class))
    }

    @Test
    fun pseudoClassEmpty() {
        parse(":empty",
                expectComponent(Component.Empty::class))
    }

    @Test
    fun pseudoClassScope() {
        parse(":scope",
                expectComponent(Component.Scope::class))
    }

    @Test
    fun pseudoClassHost() {
        parse(":host",
                expectComponent(Component.Host::class))
    }

    @Test
    fun pseudoClassNegation() {
        parse(":not(:root)",
                expectNegationPseudClass(
                        expectComponent(Component.Root::class)
                ))
    }

    ///////////////////////////////////// NonTSPseudoClass /////////////////////////////////////

    @Test
    fun nonTSPseudoClassActive() {
        parse(":active",
                expectNonTSPseudoClass(NonTSPseudoClass.Active::class))
    }

    @Test
    fun nonTSPseudoClassChecked() {
        parse(":checked",
                expectNonTSPseudoClass(NonTSPseudoClass.Checked::class))
    }

    @Test
    fun nonTSPseudoClassDisabled() {
        parse(":disabled",
                expectNonTSPseudoClass(NonTSPseudoClass.Disabled::class))
    }

    @Test
    fun nonTSPseudoClassEnabled() {
        parse(":enabled",
                expectNonTSPseudoClass(NonTSPseudoClass.Enabled::class))
    }

    @Test
    fun nonTSPseudoClassFocus() {
        parse(":focus",
                expectNonTSPseudoClass(NonTSPseudoClass.Focus::class))
    }

    @Test
    fun nonTSPseudoClassFullscreen() {
        parse(":fullscreen",
                expectNonTSPseudoClass(NonTSPseudoClass.Fullscreen::class))
    }

    @Test
    fun nonTSPseudoClassHover() {
        parse(":hover",
                expectNonTSPseudoClass(NonTSPseudoClass.Hover::class))
    }

    @Test
    fun nonTSPseudoClassIndeterminate() {
        parse(":indeterminate",
                expectNonTSPseudoClass(NonTSPseudoClass.Indeterminate::class))
    }

    @Test
    fun nonTSPseudoClassLang() {
        parse(":lang(DE)",
                expectLangNonTSPseudoClassMatching("DE"))
    }

    @Test
    fun nonTSPseudoClassLink() {
        parse(":link",
                expectNonTSPseudoClass(NonTSPseudoClass.Link::class))
    }

    @Test
    fun nonTSPseudoClassPlaceholderShown() {
        parse(":placeholder-shown",
                expectNonTSPseudoClass(NonTSPseudoClass.PlaceholderShown::class))
    }

    @Test
    fun nonTSPseudoClassReadWrite() {
        parse(":read-write",
                expectNonTSPseudoClass(NonTSPseudoClass.ReadWrite::class))
    }

    @Test
    fun nonTSPseudoClassReadOnly() {
        parse(":read-only",
                expectNonTSPseudoClass(NonTSPseudoClass.ReadOnly::class))
    }

    @Test
    fun nonTSPseudoClassTarget() {
        parse(":target",
                expectNonTSPseudoClass(NonTSPseudoClass.Target::class))
    }

    @Test
    fun nonTSPseudoClassVisited() {
        parse(":visited",
                expectNonTSPseudoClass(NonTSPseudoClass.Visited::class))
    }

    @Test
    fun pseudoClassNthChild() {
        parse(":nth-child(2n+5)",
                expectNth(Component.NthChild::class, 2, 5))
    }

    @Test
    fun pseudoClassNthLastChild() {
        parse(":nth-child(-n+0)",
                expectNth(Component.NthChild::class, -1, 0))
    }

    @Test
    fun pseudoClassNthOfType() {
        parse(":nth-of-type(+n-4)",
                expectNth(Component.NthChild::class, 1, -4))
    }

    @Test
    fun pseudoClassNthLastOfType() {
        parse(":nth-last-of-type(-n-3)",
                expectNth(Component.NthChild::class, -1, -3))
    }

    ///////////////////////////////////// PseudoElement /////////////////////////////////////

    @Test
    fun pseudoElementBefore() {
        parse("::before",
                expectPseudoElement(PseudoElement.Before::class))
    }

    @Test
    fun pseudoElementAfter() {
        parse("::after",
                expectPseudoElement(PseudoElement.After::class))
    }

    @Test
    fun pseudoElementSelection() {
        parse("::selection",
                expectPseudoElement(PseudoElement.Selection::class))
    }

    @Test
    fun pseudoElementFirstLetter() {
        parse("::first-letter",
                expectPseudoElement(PseudoElement.FirstLetter::class))
    }

    @Test
    fun pseudoElementFirstLine() {
        parse("::first-line",
                expectPseudoElement(PseudoElement.FirstLine::class))
    }

    @Test
    fun pseudoElementPlaceholder() {
        parse("::placeholder",
                expectPseudoElement(PseudoElement.Placeholder::class))
    }

    ///////////////////////////////////// AttributeSelector /////////////////////////////////////

    @Test
    fun attributeNoNamespaceExists() {
        parse("[exists]",
                expectAttributeExistsNoNamespace("exists"))
    }

    @Test
    fun attributeNoNamespaceEqual() {
        parse("[name=a]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceEqualString() {
        parse("[name=\"a\"]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceEqualStringAlt() {
        parse("[name='a']",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceEqualStringCI() {
        parse("[name=\"a\"i]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", false, false))
    }

    @Test
    fun attributeNoNamespaceEqualStringAltCI() {
        parse("[name='a'i]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", false, false))
    }

    @Test
    fun attributeNoNamespaceDashMatchIncludes() {
        parse("[name|=a]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.DashMatch::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceIncludes() {
        parse("[name~=a]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Includes::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceIncludesEmpty() {
        parse("[name~='']",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Includes::class, "", true, true))
    }

    @Test
    fun attributeNoNamespacePrefix() {
        parse("[name^=a]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Prefix::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespacePrefixEmpty() {
        parse("[name^='']",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Prefix::class, "", true, true))
    }

    @Test
    fun attributeNoNamespaceSubstring() {
        parse("[name*=a]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Substring::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceSubstringEmpty() {
        parse("[name*='']",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Substring::class, "", true, true))
    }

    @Test
    fun attributeNoNamespaceSuffix() {
        parse("[name$=a]",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Suffix::class, "a", true, false))
    }

    @Test
    fun attributeNoNamespaceSuffixEmpty() {
        parse("[name$='']",
                expectAttributeNoNamespace("name", AttributeSelectorOperator.Suffix::class, "", true, true))
    }

    private fun parse(text: String, vararg asserts: (Component) -> Unit) {
        val input = Parser.new(ParserInput(text))

        val parser = SelectorParser()

        val result = parseSelector(parser, input)

        val selector = when (result) {
            is Ok -> result.value
            is Err -> fail("parsing failed: ${result.value}")
        }

        var index = 0
        for (component in selector.rawIteratorTrueParseOrder()) {
            asserts[index++](component)
        }
    }

    private fun expectLocalNameMatching(localName: String): (Component) -> Unit {
        return {
            assertTrue(it is Component.LocalName)

            val component = it as Component.LocalName

            assertEquals(localName, component.localName)
        }
    }

    private fun expectExplicitUniversalType(): (Component) -> Unit {
        return {
            assertTrue(it is Component.ExplicitUniversalType)
        }
    }

    private fun expectIdMatching(id: String): (Component) -> Unit {
        return {
            assertTrue(it is Component.ID)

            val component = it as Component.ID

            assertEquals(id, component.id)
        }
    }

    private fun expectClassMatching(styleClass: String): (Component) -> Unit {
        return {
            assertTrue(it is Component.Class)

            val component = it as Component.Class

            assertEquals(styleClass, component.styleClass)
        }
    }

    private fun expectCombinator(kind: KClass<out Combinator>): (Component) -> Unit {
        return {
            assertTrue(it is Component.Combinator)

            val component = it as Component.Combinator

            assertTrue(kind.isInstance(component.combinator))
        }
    }

    ///////////////////////////////////// TSPseudoClass Asserts /////////////////////////////////////


    private fun expectComponent(kind: KClass<out Component>): (Component) -> Unit {
        return {
            assertTrue(kind.isInstance(it))
        }
    }

    private fun expectNegationPseudClass(vararg asserts: (Component) -> Unit): (Component) -> Unit {
        return {
            assertTrue(it is Component.Negation)

            val component = it as Component.Negation

            var index = 0
            for (innerComponent in component.simpleSelector) {
                asserts[index++](innerComponent)
            }
        }
    }

    ///////////////////////////////////// NonTSPseudoClass /////////////////////////////////////

    private fun expectNonTSPseudoClass(kind: KClass<out NonTSPseudoClass>): (Component) -> Unit {
        return {
            assertTrue(it is Component.NonTSPseudoClass)

            val component = it as Component.NonTSPseudoClass

            assertEquals(kind, component.pseudoClass::class)
        }
    }

    private fun expectLangNonTSPseudoClassMatching(lang: String): (Component) -> Unit {
        return {
            assertTrue(it is Component.NonTSPseudoClass)

            val component = it as Component.NonTSPseudoClass

            assertTrue(component.pseudoClass is NonTSPseudoClass.Lang)

            val pseudoClass = component.pseudoClass as NonTSPseudoClass.Lang

            assertEquals(lang, pseudoClass.language)
        }
    }

    private fun expectPseudoElement(kind: KClass<out PseudoElement>): (Component) -> Unit {
        return {
            assertTrue(it is Component.PseudoElement)

            val component = it as Component.PseudoElement

            assertEquals(kind, component.pseudoElement::class)
        }
    }

    private fun expectNth(kind: KClass<out Component>, a: Int, b: Int): (Component) -> Unit {
        return {
            expectComponent(kind)

            val nth = when (it) {
                is Component.NthChild -> it.nth
                is Component.NthLastChild -> it.nth
                is Component.NthLastOfType -> it.nth
                is Component.NthOfType -> it.nth
                else -> fail("not a nth component")
            }

            assertEquals(a, nth.a)
            assertEquals(b, nth.b)
        }
    }

    private fun expectAttributeExistsNoNamespace(name: String): (Component) -> Unit {
        return {
            assertTrue(it is Component.AttributeInNoNamespaceExists)

            val component = it as Component.AttributeInNoNamespaceExists

            assertTrue(component.localName.equals(name, true))
        }
    }

    private fun expectAttributeNoNamespace(name: String,
                                           operator: KClass<out AttributeSelectorOperator>,
                                           value: String,
                                           caseSensitive: Boolean,
                                           neverMatches: Boolean): (Component) -> Unit {
        return {
            assertTrue(it is Component.AttributeInNoNamespace)

            val component = it as Component.AttributeInNoNamespace

            assertEquals(name, component.localName)
            assertEquals(name.toLowerCase(), component.localNameLower)
            assertEquals(operator, component.operator::class)
            assertEquals(value, component.value)
            assertEquals(caseSensitive, component.caseSensitive)
            assertEquals(neverMatches, component.neverMatches)
        }
    }
}
