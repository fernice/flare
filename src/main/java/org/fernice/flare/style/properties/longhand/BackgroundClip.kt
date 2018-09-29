/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import java.io.Writer

object BackgroundClipId : LonghandId() {

    override val name: String = "background-clip"

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
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBackgroundClip()
                    }
                    CssWideKeyword.Inherit -> {
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
}

class BackgroundClipDeclaration(val clip: Clip) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundClipId
    }

    override fun toCssInternally(writer: Writer) {
        clip.toCss(writer)
    }

    companion object {

        val initialValue: Clip by lazy { Clip.BORDER_BOX }
    }
}

enum class Clip : ToCss {

    BORDER_BOX,

    PADDING_BOX,

    CONTENT_BOX;

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                BORDER_BOX -> "border-box"
                PADDING_BOX -> "padding-box"
                CONTENT_BOX -> "content-box"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<Clip, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "border-box" -> Ok(BORDER_BOX)
                "padding-box" -> Ok(PADDING_BOX)
                "content-box" -> Ok(CONTENT_BOX)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
