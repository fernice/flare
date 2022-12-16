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
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.std.map
import java.io.Writer

object BackgroundOriginId : AbstractLonghandId<BackgroundOriginDeclaration>(
    name = "background-origin",
    declarationType = BackgroundOriginDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundOriginDeclaration, ParseError> {
        return input.parseCommaSeparated { Origin.parse(it) }.map { BackgroundOriginDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundOriginDeclaration) {
        context.builder.setBackgroundOrigin(declaration.origin)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundOrigin()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundOrigin()
    }
}

class BackgroundOriginDeclaration(val origin: List<Origin>) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundOriginId),
) {

    override fun toCssInternally(writer: Writer) = origin.toCssJoining(writer, ", ")

    companion object {

        val InitialValue: List<Origin> by lazy { listOf(Origin.BorderBox) }
        val InitialSingleValue by lazy { Origin.BorderBox }
    }
}

sealed class Origin : ToCss {

    object BorderBox : Origin()
    object PaddingBox : Origin()
    object ContentBox : Origin()

    override fun toCss(writer: Writer) {
        writer.append(
            when (this) {
                BorderBox -> "border-box"
                PaddingBox -> "padding-box"
                ContentBox -> "content-box"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<Origin, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.lowercase()) {
                "border-box" -> Ok(BorderBox)
                "padding-box" -> Ok(PaddingBox)
                "content-box" -> Ok(ContentBox)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}