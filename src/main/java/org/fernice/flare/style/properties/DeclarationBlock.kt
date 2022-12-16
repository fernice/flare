/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.AtRuleParser
import org.fernice.flare.cssparser.DeclarationListParser
import org.fernice.flare.cssparser.DeclarationParser
import org.fernice.flare.cssparser.Delimiters
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.parseImportant
import org.fernice.flare.style.Importance
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.stylesheet.AtRulePrelude
import org.fernice.logging.FLogging
import org.fernice.std.asReversedSequence
import java.util.BitSet

data class PropertyDeclarationBlock(
    private val declarations: MutableList<PropertyDeclaration> = mutableListOf(),
    private val importances: BitSet = BitSet(),
) {

    fun expand(declarations: List<PropertyDeclaration>, importance: Importance) {
        val index = this.declarations.size

        if (importance == Importance.Important) {
            this.importances.set(index, index + declarations.size)
        } else {
            this.importances.clear(index, index + declarations.size)
        }

        this.declarations.addAll(declarations)
    }

    fun hasImportant(): Boolean {
        return !importances.isEmpty
    }

    fun asSequence(): Sequence<PropertyDeclaration> {
        return declarations.asSequence()
    }

    fun asSequence(importance: Importance, reversed: Boolean = false): Sequence<PropertyDeclaration> {
        return if (reversed) {
            declarations
                .asReversedSequence()
                .filterIndexed { index, _ -> importances.toImportance(index) == importance }
        } else {
            declarations
                .asSequence()
                .filterIndexed { index, _ -> importances.toImportance(index) == importance }
        }
    }

    val count: Int get() = declarations.size

    companion object {

        fun parse(context: ParserContext, input: Parser): PropertyDeclarationBlock {
            val declarations = mutableListOf<PropertyDeclaration>()

            val parser = PropertyDeclarationParser(context, declarations)
            val iter = DeclarationListParser(input, parser)

            val block = PropertyDeclarationBlock()

            loop@
            while (true) {
                val result = iter.next() ?: break@loop

                when (result) {
                    is Ok -> block.expand(declarations, result.value)
                    is Err -> LOG.warn("declaration parse error: ${result.value.error} '${result.value.slice}'")
                }

                declarations.clear()
            }

            return block
        }
    }
}

private fun BitSet.toImportance(index: Int): Importance = if (get(index)) Importance.Important else Importance.Normal

sealed class PropertyParseErrorKind : ParseErrorKind() {

    object UnknownProperty : PropertyParseErrorKind()
}

class PropertyDeclarationParser(private val context: ParserContext, private val declarations: MutableList<PropertyDeclaration>) :
    AtRuleParser<AtRulePrelude, Importance>, DeclarationParser<Importance> {

    override fun parseValue(input: Parser, name: String): Result<Importance, ParseError> {
        val id = when (val id = PropertyId.parse(name)) {
            is Ok -> id.value
            is Err -> return Err(input.newError(PropertyParseErrorKind.UnknownProperty))
        }

        val parseResult = input.parseUntilBefore(Delimiters.Bang) { parser ->
            PropertyDeclaration.parseInto(declarations, id, context, parser)
        }

        if (parseResult is Err) {
            return parseResult
        }

        val importance = when (input.tryParse(::parseImportant)) {
            is Ok -> Importance.Important
            is Err -> Importance.Normal
        }

        val exhausted = input.expectExhausted()

        if (exhausted is Err) {
            return exhausted
        }

        return Ok(importance)
    }
}

private val LOG = FLogging.logger { }