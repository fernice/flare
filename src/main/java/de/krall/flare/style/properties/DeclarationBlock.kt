package de.krall.flare.style.properties

import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.stylesheet.AtRulePrelude
import de.krall.flare.cssparser.*
import de.krall.flare.std.*
import java.util.*
import java.util.stream.Stream

enum class Importance {

    NORMAL,

    IMPORTANT
}

class PropertyDeclarationBlock : Iterable<PropertyDeclaration> {

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

    override fun iterator(): Iterator<PropertyDeclaration> = declarations.iterator()

    fun stream(): Stream<PropertyDeclaration> {
        return declarations.stream()
    }
}

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

        val parseResult = input.parseUntilBefore(Delimiters.Bang, { input ->
            PropertyDeclaration.parseInto(declarations, id, context, input)
        })

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