package de.krall.flare.style.properties.longhand

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.CssWideKeyword
import de.krall.flare.style.properties.LonghandId
import de.krall.flare.style.properties.PropertyDeclaration
import de.krall.flare.style.properties.PropertyEntryPoint
import de.krall.flare.style.value.Context
import modern.std.Err
import modern.std.Ok
import modern.std.Result

@PropertyEntryPoint
class BackgroundAttachmentId : LonghandId() {

    override fun name(): String {
        return "background-attachment"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { Attachment.parse(context, it) }.map { BackgroundAttachmentDeclaration(it) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundAttachmentDeclaration -> {
                context.builder.setBackgroundAttachment(declaration.attachment)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundAttachment()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundAttachment()
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

        val instance: BackgroundAttachmentId by lazy { BackgroundAttachmentId() }
    }
}

class BackgroundAttachmentDeclaration(val attachment: List<Attachment>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundAttachmentId.instance
    }

    companion object {

        val initialValue: List<Attachment> by lazy { listOf(Attachment.Scroll) }
    }
}

sealed class Attachment {

    object Scroll : Attachment()

    object Fixed : Attachment()

    object Local : Attachment()

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
                "scroll" -> Ok(Attachment.Scroll)
                "fixed" -> Ok(Attachment.Fixed)
                "local" -> Ok(Attachment.Local)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}