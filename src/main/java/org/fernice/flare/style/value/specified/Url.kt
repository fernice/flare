/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.panic
import org.fernice.flare.style.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.ComputedUrl
import org.fernice.flare.url.Url
import java.io.Writer

class CssUrl(
    val original: String?,
    val resolved: Url?,
) : SpecifiedValue<ComputedUrl>, ToCss {

    override fun toComputedValue(context: Context): ComputedUrl {
        return when {
            resolved != null -> ComputedUrl.Valid(resolved)
            original != null -> ComputedUrl.Invalid(original)
            else -> panic("expected any value")
        }
    }

    override fun toCss(writer: Writer) {
        when {
            resolved != null -> resolved.toCss(writer)
            original != null -> writer.append(original)
            else -> writer.append("<missing-url>")
        }
    }

    companion object {

        fun parseFromString(string: String, context: ParserContext): CssUrl {
            val resolved = context.urlData.join(string).ok()

            return CssUrl(
                string,
                resolved
            )
        }

        fun parse(context: ParserContext, input: Parser): Result<CssUrl, ParseError> {
            val url = when (val url = input.expectUrl()) {
                is Ok -> url.value
                is Err -> return url
            }

            return Ok(parseFromString(url, context))
        }
    }
}

typealias ImageUrl = CssUrl

