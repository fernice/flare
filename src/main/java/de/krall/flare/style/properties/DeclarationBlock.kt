package de.krall.flare.style.properties

import de.krall.flare.cssparser.AtRuleParser
import de.krall.flare.cssparser.DeclarationListParser
import de.krall.flare.cssparser.DeclarationParser
import de.krall.flare.cssparser.Delimiters
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.ParseErrorKind
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.parseImportant
import de.krall.flare.std.iter.Iter
import de.krall.flare.std.iter.iter
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.stylesheet.AtRulePrelude
import modern.std.Err
import modern.std.None
import modern.std.Ok
import modern.std.Option
import modern.std.Result
import modern.std.Some
import java.util.BitSet
import java.util.stream.Stream

enum class Importance {

    NORMAL,

    IMPORTANT
}

class PropertyDeclarationBlock {

    private val declarations = mutableListOf<PropertyDeclaration>()
    private val importance = BitSet()

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
}

class DeclarationImportanceIter(val iter: Iter<DeclarationAndImportance>) : Iter<DeclarationAndImportance> {

    companion object {
        fun reversed(declarations: List<PropertyDeclaration>, importances: BitSet): DeclarationImportanceIter {
            val reversed = mutableListOf<DeclarationAndImportance>()

            for (i in (declarations.size - 1) downTo 0) {
                val importance = if (importances.get(i)) {
                    Importance.IMPORTANT
                } else {
                    Importance.NORMAL
                }

                reversed.add(DeclarationAndImportance(
                        declarations[i],
                        importance
                ))
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
        val next = iter.next()

        val result = when (next) {
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

    class UnknownProperty : PropertyParseErrorKind()
}

class PropertyDeclarationParser(private val context: ParserContext, private val declarations: MutableList<PropertyDeclaration>) : AtRuleParser<AtRulePrelude, Importance>, DeclarationParser<Importance> {

    override fun parseValue(input: Parser, name: String): Result<Importance, ParseError> {
        val idResult = PropertyId.parse(name)

        val id = when (idResult) {
            is Ok -> idResult.value
            is Err -> return Err(input.newError(PropertyParseErrorKind.UnknownProperty()))
        }

        val parseResult = input.parseUntilBefore(Delimiters.Bang) { input ->
            PropertyDeclaration.parseInto(declarations, id, context, input)
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