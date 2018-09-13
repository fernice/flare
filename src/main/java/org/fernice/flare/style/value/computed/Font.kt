/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import org.fernice.flare.style.value.ComputedValue
import org.fernice.flare.style.value.specified.KeywordInfo
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import fernice.std.Err
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result

data class FontFamily(val values: FontFamilyList) : ComputedValue

sealed class SingleFontFamily {

    data class FamilyName(val name: org.fernice.flare.style.value.computed.FamilyName) : SingleFontFamily()

    data class Generic(val name: String) : SingleFontFamily()

    companion object Contract {

        fun parse(input: Parser): Result<SingleFontFamily, ParseError> {
            val stringResult = input.tryParse(Parser::expectString)

            if (stringResult is Ok) {
                return Ok(FamilyName(FamilyName(
                        stringResult.value
                )))
            }

            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            val cssWideKeyword = when (identifier.toLowerCase()) {
                "serif" -> return Ok(Generic("serif"))
                "sans-serif" -> return Ok(Generic("sans-serif"))
                "cursive" -> return Ok(Generic("cursive"))
                "fantasy" -> return Ok(Generic("fantasy"))
                "monospace" -> return Ok(Generic("monospace"))

                "unset" -> true
                "initial" -> true
                "inherit" -> true
                "default" -> true

                else -> false
            }

            var value = identifier

            if (cssWideKeyword) {
                val followupResult = input.expectIdentifier()

                val followup = when (followupResult) {
                    is Ok -> followupResult.value
                    is Err -> return followupResult
                }

                value += " $followup"
            }

            loop@
            while (true) {
                val nextResult = input.tryParse(Parser::expectIdentifier)

                val next = when (nextResult) {
                    is Ok -> nextResult.value
                    is Err -> break@loop
                }

                value += " $next"
            }

            return Ok(FamilyName(FamilyName(
                    value
            )))
        }
    }
}

data class FontFamilyList(private val values: List<SingleFontFamily>) : Iterable<SingleFontFamily> {

    override fun iterator(): Iterator<SingleFontFamily> = values.asReversed().iterator()
}

data class FamilyName(val value: String)

data class FontSize(
        val size: NonNegativeLength,
        val keywordInfo: Option<KeywordInfo>
) : ComputedValue {

    fun size(): Au {
        return size.into()
    }
}
