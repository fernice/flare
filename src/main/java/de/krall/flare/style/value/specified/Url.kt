package de.krall.flare.style.value.specified

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.panic
import de.krall.flare.std.Err
import de.krall.flare.std.None
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result
import de.krall.flare.std.Some
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.value.Context
import de.krall.flare.style.value.SpecifiedValue
import de.krall.flare.style.value.computed.ComputedUrl
import de.krall.flare.url.Url

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

