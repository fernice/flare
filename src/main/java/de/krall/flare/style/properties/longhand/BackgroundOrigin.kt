package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.style.parser.Parse
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import modern.std.Err
import modern.std.Ok
import modern.std.Result

@PropertyEntryPoint(legacy = false)
object BackgroundOriginId : LonghandId() {

    override fun name(): String {
        return "background-origin"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { Origin.parse(context, it) }.map(::BackgroundOriginDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundOriginDeclaration -> {
                context.builder.setBackgroundOrigin(declaration.origin)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundOrigin()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundOrigin()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return false
    }

    override fun toString(): String {
        return "LonghandId::BackgroundOrigin"
    }
}

class BackgroundOriginDeclaration(val origin: List<Origin>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundOriginId
    }

    companion object {

        val initialValue: List<Origin> by lazy { listOf(Origin.Scroll) }
    }
}

sealed class Origin {

    object Scroll : Origin()

    object Fixed : Origin()

    object Local : Origin()

    companion object : Parse<Origin> {

        override fun parse(context: ParserContext, input: Parser): Result<Origin, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "scroll" -> Ok(Origin.Scroll)
                "fixed" -> Ok(Origin.Fixed)
                "local" -> Ok(Origin.Local)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}