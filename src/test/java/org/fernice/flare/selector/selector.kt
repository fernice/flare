/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.selector

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ParserInput
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SelectorTest {

    @Test
    fun localName() {
        parse("Element")
            .components(
                Component.LocalName("Element", "element"),
            )
    }

    @Test
    fun explicitUniversalType() {
        parse("*")
            .components(
                Component.ExplicitUniversalType,
            )
    }

    @Test
    fun id() {
        parse("#identifier")
            .components(
                Component.ID("identifier"),
            )
    }

    @Test
    fun styleClass() {
        parse(".identifier")
            .components(
                Component.Class("identifier"),
            )
    }


    @Test
    fun `combinator descendant`() {
        parse("Element .identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.Descendant),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator child`() {
        parse("Element > .identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.Child),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator child no-spacing`() {
        parse("Element>.identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.Child),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator next-sibling`() {
        parse("Element + .identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.NextSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator next-sibling no-spacing`() {
        parse("Element+.identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.NextSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator later-sibling`() {
        parse("Element ~ .identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.LaterSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator later-sibling no-spacing`() {
        parse("Element~.identifier")
            .components(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.LaterSibling),
                Component.Class("identifier"),
            )
    }

    /// pseudo-class tree-structural

    @Test
    fun `pseudo-class root`() {
        parse(":root")
            .components(Component.Root)
    }

    @Test
    fun `pseudo-class empty`() {
        parse(":empty")
            .components(Component.Empty)
    }

    @Test
    fun `pseudo-class scope`() {
        parse(":scope")
            .components(Component.Scope)
    }

    @Test
    fun `pseudo-class not`() {
        parse(":not(Element#identifier.class)")
            .components(
                Component.Negation(
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        )
                    )
                )
            )

        parse(":not(Element#identifier.class, :hover)")
            .components(
                Component.Negation(
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        ),
                        Selector(
                            Component.NonTSPseudoClass(NonTSPseudoClass.Hover)
                        )
                    )
                )
            )
    }

    @Test
    fun `pseudo-class where`() {
        parse(":where(Element#identifier.class)")
            .components(
                Component.Where(
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        )
                    )
                )
            )
            .specificity(0)

        parse(":where(Element#identifier.class, :hover)")
            .components(
                Component.Where(
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        ),
                        Selector(
                            Component.NonTSPseudoClass(NonTSPseudoClass.Hover)
                        )
                    )
                )
            )
            .specificity(0)
    }

    @Test
    fun `pseudo-class is`() {
        parse(":is(Element#identifier.class)")
            .components(
                Component.Is(
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        )
                    )
                )
            )
            .specificity(1049601)

        parse(":is(Element#identifier.class, :hover)")
            .components(
                Component.Is(
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        ),
                        Selector(
                            Component.NonTSPseudoClass(NonTSPseudoClass.Hover)
                        )
                    )
                )
            )
            .specificity(1049601)
    }

    @Test
    fun `pseudo-class has`() {
        parse(":has(Element#identifier.class)")
            .components(
                Component.Has(
                    listOf(
                        RelativeSelector(
                            Component.Combinator(Combinator.Descendant),
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        )
                    )
                )
            )
            .specificity(1049601)

        parse(":has(Element#identifier.class, :hover)")
            .components(
                Component.Has(
                    listOf(
                        RelativeSelector(
                            Component.Combinator(Combinator.Descendant),
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        ),
                        RelativeSelector(
                            Component.Combinator(Combinator.Descendant),
                            Component.NonTSPseudoClass(NonTSPseudoClass.Hover),
                        )
                    )
                )
            )
            .specificity(1049601)

        parse(":has(+ Element#identifier.class, :hover)")
            .components(
                Component.Has(
                    listOf(
                        RelativeSelector(
                            Component.Combinator(Combinator.NextSibling),
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        ),
                        RelativeSelector(
                            Component.Combinator(Combinator.Descendant),
                            Component.NonTSPseudoClass(NonTSPseudoClass.Hover),
                        )
                    )
                )
            )
            .specificity(1049601)

    }

    /// pseudo-class non-tree-structural

    @Test
    fun `pseudo-class active`() {
        parse(":active")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Active))
    }

    @Test
    fun `pseudo-class checked`() {
        parse(":checked")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Checked))
    }

    @Test
    fun `pseudo-class autofilled`() {
        parse(":autofill")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Autofill))
    }

    @Test
    fun `pseudo-class disabled`() {
        parse(":disabled")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Disabled))
    }

    @Test
    fun `pseudo-class enabled`() {
        parse(":enabled")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Enabled))
    }

    @Test
    fun `pseudo-class defined`() {
        parse(":defined")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Defined))
    }

    @Test
    fun `pseudo-class focus`() {
        parse(":focus")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Focus))
    }

    @Test
    fun `pseudo-class focus-visible`() {
        parse(":focus-visible")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.FocusVisible))
    }

    @Test
    fun `pseudo-class focus-within`() {
        parse(":focus-within")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.FocusWithin))
    }

    @Test
    fun `pseudo-class hover`() {
        parse(":hover")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Hover))
    }

    @Test
    fun `pseudo-class target`() {
        parse(":target")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Target))
    }

    @Test
    fun `pseudo-class indeterminate`() {
        parse(":indeterminate")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Indeterminate))
    }

    @Test
    fun `pseudo-class fullscreen`() {
        parse(":fullscreen")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Fullscreen))
    }

    @Test
    fun `pseudo-class modal`() {
        parse(":modal")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Modal))
    }

    @Test
    fun `pseudo-class optional`() {
        parse(":optional")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Optional))
    }

    @Test
    fun `pseudo-class required`() {
        parse(":required")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Required))
    }

    @Test
    fun `pseudo-class valid`() {
        parse(":valid")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Valid))
    }

    @Test
    fun `pseudo-class invalid`() {
        parse(":invalid")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Invalid))
    }

    @Test
    fun `pseudo-class user-valid`() {
        parse(":user-valid")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.UserValid))
    }

    @Test
    fun `pseudo-class user-invalid`() {
        parse(":user-invalid")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.UserInvalid))
    }

    @Test
    fun `pseudo-class in-range`() {
        parse(":in-range")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.InRange))
    }

    @Test
    fun `pseudo-class out-of-range`() {
        parse(":out-of-range")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.OutOfRange))
    }

    @Test
    fun `pseudo-class read-write`() {
        parse(":read-write")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.ReadWrite))
    }

    @Test
    fun `pseudo-class read-only`() {
        parse(":read-only")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.ReadOnly))
    }

    @Test
    fun `pseudo-class default`() {
        parse(":default")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Default))
    }

    @Test
    fun `pseudo-class placeholder-shown`() {
        parse(":placeholder-shown")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.PlaceholderShown))
    }

    @Test
    fun `pseudo-class link`() {
        parse(":link")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Link))
    }

    @Test
    fun `pseudo-class any-link`() {
        parse(":any-link")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.AnyLink))
    }

    @Test
    fun `pseudo-class visited`() {
        parse(":visited")
            .components(Component.NonTSPseudoClass(NonTSPseudoClass.Visited))
    }

    /// pseudo-class nth

    @Test
    fun `pseudo-class first-child`() {
        parse(":first-child")
            .components(Component.Nth(NthData.first(ofType = false)))
    }

    @Test
    fun `pseudo-class last-child`() {
        parse(":last-child")
            .components(Component.Nth(NthData.last(ofType = false)))
    }

    @Test
    fun `pseudo-class only-child`() {
        parse(":only-child")
            .components(Component.Nth(NthData.only(ofType = false)))
    }

    @Test
    fun `pseudo-class first-of-type`() {
        parse(":first-of-type")
            .components(Component.Nth(NthData.first(ofType = true)))
    }

    @Test
    fun `pseudo-class last-of-type`() {
        parse(":last-of-type")
            .components(Component.Nth(NthData.last(ofType = true)))
    }

    @Test
    fun `pseudo-class only-of-type`() {
        parse(":only-of-type")
            .components(Component.Nth(NthData.only(ofType = true)))
    }

    @Test
    fun `pseudo-class nth-child`() {
        parse(":nth-child(1)")
            .components(Component.Nth(NthData(NthType.Child, 0, 1, isFunction = true)))
        parse(":nth-child(2n)")
            .components(Component.Nth(NthData(NthType.Child, 2, 0, isFunction = true)))
        parse(":nth-child(2n+1)")
            .components(Component.Nth(NthData(NthType.Child, 2, 1, isFunction = true)))
        parse(":nth-child(2n-1)")
            .components(Component.Nth(NthData(NthType.Child, 2, -1, isFunction = true)))

        parse(":nth-child(2n+1 of Element#identifier.class)")
            .components(
                Component.Nth(
                    NthData(NthType.Child, 2, 1, isFunction = true),
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        )
                    )
                )
            )
    }

    @Test
    fun `pseudo-class nth-last-child`() {
        parse(":nth-last-child(1)")
            .components(Component.Nth(NthData(NthType.LastChild, 0, 1, isFunction = true)))
        parse(":nth-last-child(2n)")
            .components(Component.Nth(NthData(NthType.LastChild, 2, 0, isFunction = true)))
        parse(":nth-last-child(2n+1)")
            .components(Component.Nth(NthData(NthType.LastChild, 2, 1, isFunction = true)))
        parse(":nth-last-child(2n-1)")
            .components(Component.Nth(NthData(NthType.LastChild, 2, -1, isFunction = true)))

        parse(":nth-last-child(2n+1 of Element#identifier.class)")
            .components(
                Component.Nth(
                    NthData(NthType.LastChild, 2, 1, isFunction = true),
                    listOf(
                        Selector(
                            Component.LocalName("Element", "element"),
                            Component.ID("identifier"),
                            Component.Class("class"),
                        )
                    )
                )
            )
    }

    @Test
    fun `pseudo-class nth-of-type`() {
        parse(":nth-of-type(1)")
            .components(Component.Nth(NthData(NthType.OfType, 0, 1, isFunction = true)))
        parse(":nth-of-type(2n)")
            .components(Component.Nth(NthData(NthType.OfType, 2, 0, isFunction = true)))
        parse(":nth-of-type(2n+1)")
            .components(Component.Nth(NthData(NthType.OfType, 2, 1, isFunction = true)))
        parse(":nth-of-type(2n-1)")
            .components(Component.Nth(NthData(NthType.OfType, 2, -1, isFunction = true)))

        parse(":nth-of-type(2n+1 of Element#identifier.class)")
            .invalid()
    }

    @Test
    fun `pseudo-class nth-last-of-type`() {
        parse(":nth-last-of-type(1)")
            .components(Component.Nth(NthData(NthType.LastOfType, 0, 1, isFunction = true)))
        parse(":nth-last-of-type(2n)")
            .components(Component.Nth(NthData(NthType.LastOfType, 2, 0, isFunction = true)))
        parse(":nth-last-of-type(2n+1)")
            .components(Component.Nth(NthData(NthType.LastOfType, 2, 1, isFunction = true)))
        parse(":nth-last-of-type(2n-1)")
            .components(Component.Nth(NthData(NthType.LastOfType, 2, -1, isFunction = true)))

        parse(":nth-last-of-type(2n+1 of Element#identifier.class)")
            .invalid()
    }

    /// pseudo-element

    @Test
    fun `pseudo-element before`() {
        parse("::before")
            .components(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.Before)
            )
    }

    @Test
    fun `pseudo-element after`() {
        parse("::after")
            .components(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.After)
            )
    }

    @Test
    fun `pseudo-element selection`() {
        parse("::selection")
            .components(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.Selection)
            )
    }

    @Test
    fun `pseudo-element first-letter`() {
        parse("::first-letter")
            .components(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.FirstLetter)
            )
    }

    @Test
    fun `pseudo-element first-line`() {
        parse("::first-line")
            .components(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.FirstLine)
            )
    }

    @Test
    fun `pseudo-element placeholder`() {
        parse("::placeholder")
            .components(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.Placeholder)
            )
    }

    @Test
    fun `pseudo-element part`() {
        parse("::part(test)")
            .components(
                Component.Combinator(Combinator.Part),
                Component.Part(listOf("test"))
            )
    }

    @Test
    fun `pseudo-element slotted`() {
        parse("::slotted(Element#identifier.class)")
            .components(
                Component.Combinator(Combinator.SlotAssignment),
                Component.Slotted(
                    Selector(
                        Component.LocalName("Element", "element"),
                        Component.ID("identifier"),
                        Component.Class("class"),
                    )
                )
            )
    }

    /// nested

    @Test
    fun `nested multi`() {
        parse("& Element~.identifier")
            .components(
                Component.ParentSelector,
                Component.Combinator(Combinator.Descendant),
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.LaterSibling),
                Component.Class("identifier"),
            )

        parse("&+Element~.identifier")
            .components(
                Component.ParentSelector,
                Component.Combinator(Combinator.NextSibling),
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.LaterSibling),
                Component.Class("identifier"),
            )
    }

    //    @Test
    //    fun attributeNoNamespaceExists() {
    //        parse(
    //            "[exists]",
    //            expectAttributeExistsNoNamespace("exists")
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceEqual() {
    //        parse(
    //            "[name=a]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceEqualString() {
    //        parse(
    //            "[name=\"a\"]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceEqualStringAlt() {
    //        parse(
    //            "[name='a']",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceEqualStringCI() {
    //        parse(
    //            "[name=\"a\"i]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", false, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceEqualStringAltCI() {
    //        parse(
    //            "[name='a'i]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Equal::class, "a", false, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceDashMatchIncludes() {
    //        parse(
    //            "[name|=a]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.DashMatch::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceIncludes() {
    //        parse(
    //            "[name~=a]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Includes::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceIncludesEmpty() {
    //        parse(
    //            "[name~='']",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Includes::class, "", true, true)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespacePrefix() {
    //        parse(
    //            "[name^=a]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Prefix::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespacePrefixEmpty() {
    //        parse(
    //            "[name^='']",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Prefix::class, "", true, true)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceSubstring() {
    //        parse(
    //            "[name*=a]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Substring::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceSubstringEmpty() {
    //        parse(
    //            "[name*='']",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Substring::class, "", true, true)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceSuffix() {
    //        parse(
    //            "[name$=a]",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Suffix::class, "a", true, false)
    //        )
    //    }
    //
    //    @Test
    //    fun attributeNoNamespaceSuffixEmpty() {
    //        parse(
    //            "[name$='']",
    //            expectAttributeNoNamespace("name", AttributeSelectorOperator.Suffix::class, "", true, true)
    //        )
    //    }

    private fun Selector(vararg components: Component): Selector {
        val builder = SelectorBuilder()
        components.forEach { component ->
            if (component is Component.Combinator) {
                builder.pushCombinator(component.combinator)
            } else {
                builder.pushSimpleSelector(component)
            }
        }
        return builder.build()
    }

    private fun RelativeSelector(vararg components: Component): RelativeSelector {
        return RelativeSelector.fromSelector(
            Selector(
                Component.RelativeSelectorAnchor,
                *components,
            )
        )
    }

    private fun parse(text: String, nested: Boolean = false): ParseResult {
        val input = Parser.from(ParserInput(text))

        val parser = SelectorParser()

        val parseRelative = when(nested){
            true -> ParseRelative.ForNesting
            false -> ParseRelative.No
        }

        val result = parseSelector(parser, input, SelectorParsingState.empty(), parseRelative)

        return ParseResult(text, result)
    }

    private class ParseResult(val input: String, val result: Result<Selector, ParseError>) {
        fun invalid(): ParseError {
            return when (result) {
                is Ok -> fail("input '$input' resulted in valid selector ${result.value}")
                is Err -> result.value
            }
        }

        fun valid(): Selector {
            return when (result) {
                is Ok -> result.value
                is Err -> fail("input '$input' result in invalid selector: ${result.value}")
            }
        }

        fun components(vararg components: Component): ParseResult {
            val selector = valid()

            assertEquals(components.size, selector.rawIteratorTrueParseOrder().asSequence().count(), message = "selector deviates in component count")

            for ((index, actual) in selector.rawIteratorTrueParseOrder().withIndex()) {
                val expected = components[index]

                assertEquals(actual, expected, message = "selector deviates in component at index $index")
            }

            return this
        }

        fun specificity(specificity: Int): ParseResult {
            val selector = valid()

            assertEquals(specificity, selector.specificity, message = "selector deviates in specificity")

            return this
        }
    }
}
