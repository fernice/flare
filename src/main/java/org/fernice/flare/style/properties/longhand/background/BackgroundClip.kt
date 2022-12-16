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
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.AbstractLonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyDeclarationId
import org.fernice.flare.style.value.Context
import org.fernice.std.unwrap
import org.fernice.std.map
import java.io.Writer

object BackgroundClipId : AbstractLonghandId<BackgroundClipDeclaration>(
    name = "background-clip",
    declarationType = BackgroundClipDeclaration::class,
    isInherited = false,
) {

    override fun parseValue(context: ParserContext, input: Parser): Result<BackgroundClipDeclaration, ParseError> {
        return Clip.parse(input).map { BackgroundClipDeclaration(it) }
    }

    override fun cascadeProperty(context: Context, declaration: BackgroundClipDeclaration) {
        context.builder.setBackgroundClip(declaration.clip)
    }

    override fun resetProperty(context: Context) {
        context.builder.resetBackgroundClip()
    }

    override fun inheritProperty(context: Context) {
        context.builder.inheritBackgroundClip()
    }
}

class BackgroundClipDeclaration(val clip: Clip) : PropertyDeclaration(
    id = PropertyDeclarationId.Longhand(BackgroundClipId),
) {

    override fun toCssInternally(writer: Writer) {
        clip.toCss(writer)
    }

    companion object {

        val InitialValue: Clip = Clip.BorderBox
        val InitialSingleValue: Clip = Clip.BorderBox
    }
}

enum class Clip : ToCss {

    BorderBox,
    PaddingBox,
    ContentBox;

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

        fun parse(input: Parser): Result<Clip, ParseError> {
            val location = input.sourceLocation()
            val identifier = input.expectIdentifier()
                .unwrap { return it }

            return when (identifier.lowercase()) {
                "border-box" -> Ok(BorderBox)
                "padding-box" -> Ok(PaddingBox)
                "content-box" -> Ok(ContentBox)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}
