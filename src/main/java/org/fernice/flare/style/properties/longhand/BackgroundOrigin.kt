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
import org.fernice.flare.cssparser.toCssJoining
import org.fernice.flare.style.parser.Parse
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.value.Context
import java.io.Writer

object BackgroundOriginId : LonghandId() {

    override val name: String = "background-origin"

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
                    CssWideKeyword.Unset,
                    CssWideKeyword.Initial -> {
                        context.builder.resetBackgroundOrigin()
                    }
                    CssWideKeyword.Inherit -> {
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

    override fun toCssInternally(writer: Writer) = origin.toCssJoining(writer, ", ")

    companion object {

        val initialValue: List<Origin> by lazy { listOf(Origin.Scroll) }
    }
}

sealed class Origin : ToCss {

    object Scroll : Origin()

    object Fixed : Origin()

    object Local : Origin()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                is Origin.Scroll -> "scroll"
                is Origin.Fixed -> "fixed"
                is Origin.Local -> "local"
            }
        )
    }

    companion object : Parse<Origin> {

        override fun parse(context: ParserContext, input: Parser): Result<Origin, ParseError> {
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
