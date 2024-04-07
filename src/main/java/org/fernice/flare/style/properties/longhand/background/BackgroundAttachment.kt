/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand.background

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.std.map
import java.io.Writer

object BackgroundAttachmentId : AbstractLonghandId<BackgroundAttachmentDeclaration>(
    name = "background-attachment",
    declarationType = BackgroundAttachmentDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundAttachmentDeclaration, ParseError> {
        return input.parseCommaSeparated { scopedInput -> Attachment.parse(context, scopedInput) }
            .map { BackgroundAttachmentDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundAttachmentDeclaration) {
        context.builder.setBackgroundAttachment(declaration.attachment)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundAttachment()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundAttachment()
    }
}

class BackgroundAttachmentDeclaration(val attachment: List<Attachment>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundAttachmentId),
) {

    override fun toCssInternally(writer: Writer) = attachment.toCssJoining(writer, ", ")

    companion object {

        val InitialValue: List<Attachment> by lazy { listOf(Attachment.Scroll) }
        val InitialSingleValue by lazy { Attachment.Scroll }
    }
}

sealed class Attachment : ToCss {

    object Scroll : Attachment()

    object Fixed : Attachment()

    object Local : Attachment()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is Scroll -> "scroll"
                is Fixed -> "fixed"
                is Local -> "local"
            }
        )
    }

    companion object {

        @Suppress("UNUSED_PARAMETER")
        fun parse(context: ParserContext, input: Parser): Result<Attachment, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.lowercase()) {
                "scroll" -> Ok(Scroll)
                "fixed" -> Ok(Fixed)
                "local" -> Ok(Local)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
