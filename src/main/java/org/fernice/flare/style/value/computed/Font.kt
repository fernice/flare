/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.value.computed

import fernice.std.Err
import fernice.std.Ok
import fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.std.max
import org.fernice.flare.std.min
import org.fernice.flare.style.value.ComputedValue
import org.fernice.flare.style.value.specified.KeywordInfo
import java.io.Writer

data class FontWeight(val value: Float) {

    fun bolder(): FontWeight {
        return when {
            value < 350 -> FontWeight(400f)
            value < 550 -> FontWeight(700f)
            else -> FontWeight(value.max(900f))
        }
    }

    fun lighter(): FontWeight {
        return when {
            value < 550 -> FontWeight(value.min(100f))
            value < 750 -> FontWeight(400f)
            else -> FontWeight(value.max(700f))
        }
    }

    companion object {

        val Normal by lazy { FontWeight(400f) }
        val Bold by lazy { FontWeight(700f) }
    }
}

data class FontFamily(val values: FontFamilyList) : ComputedValue

sealed class SingleFontFamily : ToCss {

    data class FamilyName(val name: org.fernice.flare.style.value.computed.FamilyName) : SingleFontFamily()

    data class Generic(val name: String) : SingleFontFamily()

    final override fun toCss(writer: Writer) {
        when (this) {
            is FamilyName -> name.toCss(writer)
            is Generic -> writer.append(name)
        }
    }

    companion object Contract {

        fun parse(input: Parser): Result<SingleFontFamily, ParseError> {
            val stringResult = input.tryParse(Parser::expectString)

            if (stringResult is Ok) {
                return Ok(
                    FamilyName(
                        FamilyName(
                            stringResult.value
                        )
                    )
                )
            }

            val identifier = when (val identifierResult = input.expectIdentifier()) {
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
                val followup = when (val followupResult = input.expectIdentifier()) {
                    is Ok -> followupResult.value
                    is Err -> return followupResult
                }

                value += " $followup"
            }

            loop@
            while (true) {
                val next = when (val nextResult = input.tryParse(Parser::expectIdentifier)) {
                    is Ok -> nextResult.value
                    is Err -> break@loop
                }

                value += " $next"
            }

            return Ok(
                FamilyName(
                    FamilyName(
                        value
                    )
                )
            )
        }
    }
}

data class FontFamilyList(private val values: List<SingleFontFamily>) : Iterable<SingleFontFamily> {

    override fun iterator(): Iterator<SingleFontFamily> = values.iterator()
}

data class FamilyName(val value: String) : ToCss {

    override fun toCss(writer: Writer) {
        writer.append(value)
    }
}

data class FontSize(
    val size: NonNegativeLength,
    val keywordInfo: KeywordInfo?
) : ComputedValue {

    fun size(): Au {
        return size.into()
    }
}
