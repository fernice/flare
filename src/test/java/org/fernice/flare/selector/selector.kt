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
import org.fernice.std.unwrapOrNull
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SelectorTest {

    @Test
    fun localName() {
        parse("Element")
            .valid(
                Component.LocalName("Element", "element"),
            )
    }

    @Test
    fun explicitUniversalType() {
        parse("*")
            .valid(
                Component.ExplicitUniversalType,
            )
    }

    @Test
    fun id() {
        parse("#identifier")
            .valid(
                Component.ID("identifier"),
            )
    }

    @Test
    fun styleClass() {
        parse(".identifier")
            .valid(
                Component.Class("identifier"),
            )
    }


    @Test
    fun `combinator descendant`() {
        parse("Element .identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.Descendant),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator child`() {
        parse("Element > .identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.Child),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator child no-spacing`() {
        parse("Element>.identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.Child),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator next-sibling`() {
        parse("Element + .identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.NextSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator next-sibling no-spacing`() {
        parse("Element+.identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.NextSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator later-sibling`() {
        parse("Element ~ .identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.LaterSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `combinator later-sibling no-spacing`() {
        parse("Element~.identifier")
            .valid(
                Component.LocalName("Element", "element"),
                Component.Combinator(Combinator.LaterSibling),
                Component.Class("identifier"),
            )
    }

    @Test
    fun `pseudo-class root`() {
        parse(":root").valid(Component.Root)
    }

    @Test
    fun `pseudo-class first-child`() {
        parse(":first-child")
            .valid(Component.Nth(NthData.first(ofType = false)))
    }

    @Test
    fun `pseudo-class last-child`() {
        parse(":last-child")
            .valid(Component.Nth(NthData.last(ofType = false)))
    }

    @Test
    fun `pseudo-class only-child`() {
        parse(":only-child")
            .valid(Component.Nth(NthData.only(ofType = false)))
    }

    @Test
    fun `pseudo-class first-of-type`() {
        parse(":first-of-type")
            .valid(Component.Nth(NthData.first(ofType = true)))
    }

    @Test
    fun `pseudo-class last-of-type`() {
        parse(":last-of-type")
            .valid(Component.Nth(NthData.last(ofType = true)))
    }

    @Test
    fun `pseudo-class only-of-type`() {
        parse(":only-of-type")
            .valid(Component.Nth(NthData.only(ofType = true)))
    }

    @Test
    fun `pseudo-class nth-child`() {
        parse(":nth-child(1)")
            .valid(Component.Nth(NthData(NthType.Child, 0, 1, isFunction = true)))
        parse(":nth-child(2n)")
            .valid(Component.Nth(NthData(NthType.Child, 2, 0, isFunction = true)))
        parse(":nth-child(2n+1)")
            .valid(Component.Nth(NthData(NthType.Child, 2, 1, isFunction = true)))
        parse(":nth-child(2n-1)")
            .valid(Component.Nth(NthData(NthType.Child, 2, -1, isFunction = true)))

        // todo nth-child(2n+1 of Element#identifier.class)
    }

    @Test
    fun `pseudo-class nth-last-child`() {
        parse(":nth-last-child(1)")
            .valid(Component.Nth(NthData(NthType.LastChild, 0, 1, isFunction = true)))
        parse(":nth-last-child(2n)")
            .valid(Component.Nth(NthData(NthType.LastChild, 2, 0, isFunction = true)))
        parse(":nth-last-child(2n+1)")
            .valid(Component.Nth(NthData(NthType.LastChild, 2, 1, isFunction = true)))
        parse(":nth-last-child(2n-1)")
            .valid(Component.Nth(NthData(NthType.LastChild, 2, -1, isFunction = true)))
    }

    @Test
    fun `pseudo-class nth-of-type`() {
        parse(":nth-of-type(1)")
            .valid(Component.Nth(NthData(NthType.OfType, 0, 1, isFunction = true)))
        parse(":nth-of-type(2n)")
            .valid(Component.Nth(NthData(NthType.OfType, 2, 0, isFunction = true)))
        parse(":nth-of-type(2n+1)")
            .valid(Component.Nth(NthData(NthType.OfType, 2, 1, isFunction = true)))
        parse(":nth-of-type(2n-1)")
            .valid(Component.Nth(NthData(NthType.OfType, 2, -1, isFunction = true)))
    }

    @Test
    fun `pseudo-class nth-last-of-type`() {
        parse(":nth-last-of-type(1)")
            .valid(Component.Nth(NthData(NthType.LastOfType, 0, 1, isFunction = true)))
        parse(":nth-last-of-type(2n)")
            .valid(Component.Nth(NthData(NthType.LastOfType, 2, 0, isFunction = true)))
        parse(":nth-last-of-type(2n+1)")
            .valid(Component.Nth(NthData(NthType.LastOfType, 2, 1, isFunction = true)))
        parse(":nth-last-of-type(2n-1)")
            .valid(Component.Nth(NthData(NthType.LastOfType, 2, -1, isFunction = true)))
    }

    @Test
    fun `pseudo-element before`() {
        parse("::before")
            .valid(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.Before)
            )
    }

    @Test
    fun `pseudo-element after`() {
        parse("::after")
            .valid(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.After)
            )
    }

    @Test
    fun `pseudo-element selection`() {
        parse("::selection")
            .valid(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.Selection)
            )
    }

    @Test
    fun `pseudo-element first-letter`() {
        parse("::first-letter")
            .valid(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.FirstLetter)
            )
    }

    @Test
    fun `pseudo-element first-line`() {
        parse("::first-line")
            .valid(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.FirstLine)
            )
    }

    @Test
    fun `pseudo-element placeholder`() {
        parse("::placeholder")
            .valid(
                Component.Combinator(Combinator.PseudoElement),
                Component.PseudoElement(PseudoElement.Placeholder)
            )
    }

    @Test
    fun `pseudo-element part`() {
        parse("::part(test)")
            .valid(
                Component.Combinator(Combinator.Part),
                Component.Part(listOf("test"))
            )
    }

    @Test
    fun `pseudo-element slotted`() {
        parse("::slotted(Element#Identifier.class)")
        // todo selector comparison
        //    .valid(
        //        Component.Combinator(Combinator.Part),
        //        Component.Part(listOf("test"))
        //    )
    }

    private fun parse(text: String): ParseResult {
        val input = Parser.from(ParserInput(text))

        val parser = SelectorParser()

        val result = parseSelector(parser, input, SelectorParsingState.empty(), ParseRelative.No)

        return ParseResult(text, result)
    }

    private class ParseResult(val input: String, val result: Result<Selector, ParseError>) {
        fun invalid() {
            val selector = result.unwrapOrNull()
            if (selector != null) fail("input '$input' resulted in valid selector $selector")
        }

        fun valid(vararg components: Component) {
            val selector = when (result) {
                is Ok -> result.value
                is Err -> fail("input '$input' result in invalid selector: ${result.value}")
            }

            assertEquals(components.size, selector.rawIteratorTrueParseOrder().asSequence().count(), message = "selector deviates in component count")

            for ((index, actual) in selector.rawIteratorTrueParseOrder().withIndex()) {
                val expected = components[index]

                assertEquals(actual, expected, message = "selector deviates in component at index $index")
            }
        }
    }
}
