/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import org.fernice.flare.cssparser.AtRuleParser
import org.fernice.flare.cssparser.DeclarationListParser
import org.fernice.flare.cssparser.DeclarationParser
import org.fernice.flare.cssparser.Delimiters
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.ParseErrorKind
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.parseImportant
import org.fernice.flare.std.iter.Iter
import org.fernice.flare.std.iter.iter
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.stylesheet.AtRulePrelude
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import java.util.BitSet
import java.util.stream.Stream

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

    fun stream(): Stream<PropertyDeclaration> {
        return declarations.reversed().stream()
    }

    fun reversedDeclarationImportanceIter(): DeclarationImportanceIter {
        return DeclarationImportanceIter.reversed(declarations, importance)
    }

    fun declarationImportanceIter(): DeclarationImportanceIter {
        return DeclarationImportanceIter.new(declarations, importance)
    }
}

class DeclarationImportanceIter(val iter: Iter<DeclarationAndImportance>) : Iter<DeclarationAndImportance> {

    companion object {
        fun new(declarations: List<PropertyDeclaration>, importances: BitSet): DeclarationImportanceIter {
            val items = mutableListOf<DeclarationAndImportance>()

            for (i in 0 until declarations.size) {
                val importance = if (importances.get(i)) {
                    Importance.IMPORTANT
                } else {
                    Importance.NORMAL
                }

                items.add(
                    DeclarationAndImportance(
                        declarations[i],
                        importance
                    )
                )
            }

            return DeclarationImportanceIter(items.iter())
        }

        fun reversed(declarations: List<PropertyDeclaration>, importances: BitSet): DeclarationImportanceIter {
            val reversed = mutableListOf<DeclarationAndImportance>()

            for (i in (declarations.size - 1) downTo 0) {
                val importance = if (importances.get(i)) {
                    Importance.IMPORTANT
                } else {
                    Importance.NORMAL
                }

                reversed.add(
                    DeclarationAndImportance(
                        declarations[i],
                        importance
                    )
                )
            }

            return DeclarationImportanceIter(reversed.iter())
        }
    }

    override fun next(): Option<DeclarationAndImportance> {
        return iter.next()
    }

    override fun clone(): Iter<DeclarationAndImportance> {
        return DeclarationImportanceIter(iter.clone())
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
            is Ok -> {
                block.expand(declarations, result.value)
            }
            is Err -> {
                println("declaration parse error: ${result.value.error} '${result.value.slice}'")
            }
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
