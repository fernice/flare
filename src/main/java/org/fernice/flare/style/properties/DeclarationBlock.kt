/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Result
import fernice.std.Some
import org.fernice.flare.cssparser.AtRuleParser
import org.fernice.flare.cssparser.DeclarationListParser
import org.fernice.flare.cssparser.DeclarationParser
import org.fernice.flare.cssparser.Delimiters
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.parseImportant
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.stylesheet.AtRulePrelude
import org.fernice.logging.FLogging
import java.util.BitSet

enum class Importance {

    NORMAL,

    IMPORTANT
}

data class PropertyDeclarationBlock(
    private val declarations: MutableList<PropertyDeclaration> = mutableListOf(),
    private val importance: BitSet = BitSet()
) {

    fun expand(declarations: List<PropertyDeclaration>, importance: Importance) {
        val index = this.declarations.size

        if (importance == Importance.IMPORTANT) {
            this.importance.set(index, index + declarations.size)
        } else {
            this.importance.clear(index, index + declarations.size)
        }

        this.declarations.addAll(declarations)
    }

    fun hasImportant(): Boolean {
        return !importance.isEmpty
    }

    fun reversedDeclarationImportanceSequence(): DeclarationImportanceSequence {
        return DeclarationImportanceSequence.reversed(declarations, importance)
    }

    fun declarationImportanceSequence(): DeclarationImportanceSequence {
        return DeclarationImportanceSequence.new(declarations, importance)
    }

    val count: Int get() = declarations.size
}

class DeclarationImportanceSequence(private val sequence: Sequence<DeclarationAndImportance>) : Sequence<DeclarationAndImportance> by sequence {

    companion object {
        fun new(declarations: List<PropertyDeclaration>, importances: BitSet): DeclarationImportanceSequence {
            val sequence = declarations.withIndex()
                .map { (index, declaration) -> DeclarationAndImportance(declaration, importances.toImportance(index)) }
                .asSequence()

            return DeclarationImportanceSequence(sequence)
        }

        fun reversed(declarations: List<PropertyDeclaration>, importances: BitSet): DeclarationImportanceSequence {
            val sequence = declarations.withIndex()
                .reversed()
                .map { (index, declaration) -> DeclarationAndImportance(declaration, importances.toImportance(index)) }
                .asSequence()

            return DeclarationImportanceSequence(sequence)
        }

        private fun BitSet.toImportance(index: Int): Importance = if (get(index)) Importance.IMPORTANT else Importance.NORMAL
    }
}

data class DeclarationAndImportance(val declaration: PropertyDeclaration, val importance: Importance)

fun parsePropertyDeclarationList(context: ParserContext, input: Parser): PropertyDeclarationBlock {
    val declarations = mutableListOf<PropertyDeclaration>()

    val parser = PropertyDeclarationParser(context, declarations)
    val iter = DeclarationListParser(input, parser)

    val block = PropertyDeclarationBlock()

    loop@
    while (true) {
        val result = when (val next = iter.next()) {
            is Some -> next.value
            is None -> break@loop
        }

        when (result) {
            is Ok -> block.expand(declarations, result.value)
            is Err -> LOG.warn("declaration parse error: ${result.value.error} '${result.value.slice}'")
        }

        declarations.clear()
    }

    return block
}

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
            is Ok -> Importance.IMPORTANT
            is Err -> Importance.NORMAL
        }

        val exhausted = input.expectExhausted()

        if (exhausted is Err) {
            return exhausted
        }

        return Ok(importance)
    }
}

private val LOG = FLogging.logger { }