package de.krall.flare.style.value.computed

import de.krall.flare.style.value.ComputedValue
import de.krall.flare.style.value.specified.KeywordInfo
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result

class FontFamily(val values: FontFamilyList) : ComputedValue

sealed class SingleFontFamily {

    companion object Contract {

        fun parse(input: Parser): Result<SingleFontFamily, ParseError> {
            val stringResult = input.tryParse { input -> input.expectIdentifier() }

            if (stringResult is Ok) {
                return Ok(SingleFontFamily.FamilyName(de.krall.flare.style.value.computed.FamilyName(
                        stringResult.value
                )))
            }

            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            val cssWideKeyword = when (identifier.toLowerCase()) {
                "serif" -> return Ok(SingleFontFamily.Generic("serif"))
                "sans-serif" -> return Ok(SingleFontFamily.Generic("sans-serif"))
                "cursive" -> return Ok(SingleFontFamily.Generic("cursive"))
                "fantasy" -> return Ok(SingleFontFamily.Generic("fantasy"))
                "monospace" -> return Ok(SingleFontFamily.Generic("monospace"))

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
                val nextResult = input.tryParse { input -> input.expectIdentifier() }

                val next = when (nextResult) {
                    is Ok -> nextResult.value
                    is Err -> break@loop
                }

                value += " $next"
            }

            return Ok(SingleFontFamily.FamilyName(de.krall.flare.style.value.computed.FamilyName(
                    value
            )))
        }
    }

    class FamilyName(val name: de.krall.flare.style.value.computed.FamilyName) : SingleFontFamily()

    class Generic(val name: String) : SingleFontFamily()
}

class FontFamilyList(private val values: List<SingleFontFamily>) : Iterable<SingleFontFamily> {

    override fun iterator(): Iterator<SingleFontFamily> = values.iterator()
}

class FamilyName(val value: String)

class FontSize(val size: NonNegativeLength,
               val keywordInfo: Option<KeywordInfo>) : ComputedValue {

    fun size(): Au {
        return size.into()
    }
}
