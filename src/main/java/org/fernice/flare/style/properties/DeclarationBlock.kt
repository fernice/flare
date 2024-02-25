/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import org.fernice.flare.cssparser.*
import org.fernice.flare.selector.SelectorList
import org.fernice.flare.style.ContextualError
import org.fernice.flare.style.Importance
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.StyleParseErrorKind
import org.fernice.std.*
import java.util.*

class PropertyDeclarationBlock {

    private val declarations: MutableList<PropertyDeclaration> = mutableListOf()
    private val importances: BitSet = BitSet()

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

    val size: Int get() = declarations.size

    override fun toString(): String = "PropertyDeclarationBlock[${declarations.size} declarations]"

    companion object {

        fun parsePropertyDeclarationList(
            context: ParserContext,
            input: Parser,
            selectors: List<SelectorList>,
        ): PropertyDeclarationBlock {
            val state = DeclarationParserState()

            val parser = PropertyDeclarationParser(context, state)
            val iter = RuleBodyParser(input, parser)

            while (true) {
                when (val result = iter.next() ?: break) {
                    is Ok -> {}
                    is Err -> state.didError(context, result.value.error, result.value.slice)
                }
            }
            state.flushErrors(context, selectors)

            return state.takeDeclarations()
        }
    }
}

private fun BitSet.toImportance(index: Int): Importance = if (get(index)) Importance.Important else Importance.Normal

class PropertyDeclarationParser(
    private val context: ParserContext,
    private val state: DeclarationParserState,
) : RuleBodyItemParser<Unit, Unit, Unit> {

    override fun parseValue(name: String, input: Parser): Result<Unit, ParseError> {
        return state.parseValue(context, name, input)
    }

    override fun shouldParseDeclarations(): Boolean = true
    override fun shouldParseQualifiedRule(): Boolean = false
}

class DeclarationParserState {
    private var declarationBlock = PropertyDeclarationBlock()
    private val declarations = mutableListOf<PropertyDeclaration>()
    private var lastParsedPropertyId: PropertyId? = null
    private val errors = mutableListOf<PropertyParseError>()

    fun parseValue(
        context: ParserContext,
        name: String,
        input: Parser,
    ): Result<Unit, ParseError> {
        val id = when (val id = PropertyId.parse(name)) {
            is Ok -> id.value
            is Err -> return Err(input.newError(StyleParseErrorKind.UnknownProperty(name)))
        }

        lastParsedPropertyId = id

        input.parseUntilBefore(Delimiters.Bang) { nestedInput ->
            PropertyDeclaration.parseInto(declarations, id, context, nestedInput)
        }.propagate { return it }

        val importance = when (input.tryParse(::parseImportant)) {
            is Ok -> Importance.Important
            is Err -> Importance.Normal
        }

        input.expectExhausted().propagate { return it }

        declarationBlock.expand(declarations.drain(), importance)

        lastParsedPropertyId = null

        return Ok()
    }

    fun didError(context: ParserContext, error: ParseError, slice: String) {
        if (!context.isErrorReportingEnabled()) return

        errors.add(PropertyParseError(error, slice, lastParsedPropertyId))
    }

    fun flushErrors(context: ParserContext, selectors: List<SelectorList>) {
        if (errors.isEmpty()) return

        for ((error, slice, propertyId) in errors) {
            context.reportError(selectors, error, slice, propertyId)
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun ParserContext.reportError(selectors: List<SelectorList>, error: ParseError, slice: String, propertyId: PropertyId?) {
        var error = error

        if (propertyId != null) {
            val name = when (propertyId) {
                is PropertyId.Longhand -> propertyId.id.name
                is PropertyId.Shorthand -> propertyId.id.name
                is PropertyId.Custom -> propertyId.name.value
            }
            error = when (error.kind) {
                is StyleParseErrorKind.UnknownProperty -> error
                else -> ParseError(
                    StyleParseErrorKind.InvalidPropertyValue(name, error),
                    error.location,
                )
            }
        }

        reportError(
            error.location,
            ContextualError.UnsupportedPropertyDeclaration(slice, error, selectors),
        )
    }

    fun hasDeclarations(): Boolean {
        return declarationBlock.size > 0
    }

    fun takeDeclarations(): PropertyDeclarationBlock {
        val declarations = declarationBlock
        declarationBlock = PropertyDeclarationBlock()
        return declarations
    }

    private data class PropertyParseError(
        val error: ParseError,
        val slice: String,
        val propertyId: PropertyId?,
    )
}
