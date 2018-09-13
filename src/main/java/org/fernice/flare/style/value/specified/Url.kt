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

class CssUrl(
        val original: Option<String>,
        val resolved: Option<Url>
) : SpecifiedValue<ComputedUrl> {

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

    companion object {

        fun parseFromString(string: String, context: ParserContext): CssUrl {
            val resolved = context.baseUrl.join(string).ok()

            return CssUrl(
                    Some(string),
                    resolved
            )
        }

        fun parse(context: ParserContext, input: Parser): Result<CssUrl, ParseError> {
            val urlResult = input.expectUrl()

            val url = when (urlResult) {
                is Ok -> urlResult.value
                is Err -> return urlResult
            }

            return Ok(parseFromString(url, context))
        }
    }
}

typealias ImageUrl = CssUrl

