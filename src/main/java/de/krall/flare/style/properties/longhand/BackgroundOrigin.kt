package de.krall.flare.style.properties.longhand

import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result

@PropertyEntryPoint
class BackgroundOriginId : LonghandId() {

    override fun name(): String {
        return "background-origin"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { Attachment.parse(context, it) }.map { BackgroundAttachmentDeclaration(it) }
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

    companion object {

        val instance: BackgroundOriginId by lazy { BackgroundOriginId() }
    }
}

class BackgroundOriginDeclaration(val origin: List<Origin>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundOriginId.instance
    }

    companion object {

        val initialValue: List<Origin> by lazy { listOf(Origin.SCROLL) }
    }
}

enum class Origin {

    SCROLL,

    FIXED,

    LOCAL;

    companion object {

        @Suppress("UNUSED_PARAMETER")
        fun parse(context: ParserContext, input: Parser): Result<Attachment, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "scroll" -> Ok(Attachment.SCROLL)
                "fixed" -> Ok(Attachment.FIXED)
                "local" -> Ok(Attachment.LOCAL)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}