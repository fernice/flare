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
class BackgroundClipId : LonghandId() {

    override fun name(): String {
        return "background-clip"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return Clip.parse(input).map(::BackgroundClipDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundClipDeclaration -> {
                context.builder.setBackgroundClip(declaration.clip)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundClip()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundClip()
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
        return "LonghandId::BackgroundAttachment"
    }

    companion object {

        val instance: BackgroundClipId by lazy { BackgroundClipId() }
    }
}

class BackgroundClipDeclaration(val clip: Clip) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundClipId.instance
    }

    companion object {

        val initialValue: Clip by lazy { Clip.BORDER_BOX }
    }
}

enum class Clip {

    BORDER_BOX,

    PADDING_BOX,

    CONTENT_BOX;

    companion object {

        fun parse(input: Parser): Result<Clip, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "border-box" -> Ok(Clip.BORDER_BOX)
                "padding-box" -> Ok(Clip.PADDING_BOX)
                "content-box" -> Ok(Clip.CONTENT_BOX)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}