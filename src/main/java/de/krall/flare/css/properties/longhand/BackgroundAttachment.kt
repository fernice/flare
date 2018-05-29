package de.krall.flare.css.properties.longhand

import de.krall.flare.css.Context
import de.krall.flare.css.ParserContext
import de.krall.flare.css.properties.*
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result

private fun parse(context: ParserContext, input: Parser): Result<List<Attachment>, ParseError> {
    return input.parseCommaSeparated { parseKeyword(context, it) }
}

@Suppress("UNUSED_PARAMETER")
private fun parseKeyword(context: ParserContext, input: Parser): Result<Attachment, ParseError> {
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

private fun getInitialValue(): List<Attachment> {
    return listOf(Attachment.SCROLL)
}

class BackgroundAttachment(val attachment: List<Attachment>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundAttachmentId.instance
    }
} 

@PropertyEntryPoint
class BackgroundAttachmentId : LonghandId() {

    override fun name(): String {
        return "background-attachment"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return parse(context, input).map { BackgroundAttachment(it) }
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundAttachment -> {

            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {

                    }
                    CssWideKeyword.INHERIT -> {

                    }
                }
            }
        }
    }

    override fun toString(): String {
        return "LonghandId::BackgroundAttachment"
    }

    companion object {

        val instance: BackgroundAttachmentId by lazy { BackgroundAttachmentId() }
    }
}

enum class Attachment {

    SCROLL,

    FIXED,

    LOCAL
}