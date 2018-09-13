/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result

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
                "scroll" -> Ok(Scroll)
                "fixed" -> Ok(Fixed)
                "local" -> Ok(Local)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
