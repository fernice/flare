/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.specified

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.panic
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.SpecifiedValue
import org.fernice.flare.style.value.computed.ComputedUrl
import org.fernice.flare.url.Url
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import org.fernice.flare.cssparser.ToCss
import java.io.Writer

class CssUrl(
    val original: Option<String>,
    val resolved: Option<Url>
) : SpecifiedValue<ComputedUrl>, ToCss {

    override fun toComputedValue(context: Context): ComputedUrl {
        return when (resolved) {
            is Some -> ComputedUrl.Valid(resolved.value)
            is None -> {
                when (original) {
                    is Some -> ComputedUrl.Invalid(original.value)
                    is None -> panic("expected any value")
                }
            }
        }
    }

    override fun toCss(writer: Writer) {
        when {
            original is Some -> writer.append(original.value)
            resolved is Some -> resolved.value.toCss(writer)
            else -> writer.append("<missing-url>")
        }
    }

    companion object {

        fun parseFromString(string: String, context: ParserContext): CssUrl {
            val resolved = context.baseUrl.join(string).ok()

            return CssUrl(
                Some(string),
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

