/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.parser

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.url.Url
import fernice.std.Result
import java.net.URI

class ParserContext(
    val parseMode: ParseMode,
    val quirksMode: QuirksMode,
    val baseUrl: Url,
    val source: URI? = null
)

interface Parse<T> {

    fun parse(context: ParserContext, input: Parser): Result<T, ParseError>
}

interface ParseQuirky<T> {

    fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<T, ParseError>
}

sealed class ParseMode {

    abstract fun allowsUnitlessNumbers(): Boolean

    object Default : ParseMode() {
        override fun allowsUnitlessNumbers(): Boolean {
            return false
        }
    }

    object UnitlessLength : ParseMode() {
        override fun allowsUnitlessNumbers(): Boolean {
            return true
        }
    }
}

sealed class AllowQuirks {

    abstract fun allowed(quirksMode: QuirksMode): Boolean

    object Yes : AllowQuirks() {
        override fun allowed(quirksMode: QuirksMode): Boolean {
            return quirksMode == QuirksMode.QUIRKS
        }
    }

    object No : AllowQuirks() {
        override fun allowed(quirksMode: QuirksMode): Boolean {
            return false
        }
    }
}

enum class QuirksMode {

    QUIRKS,

    LIMITED_QUIRKS,

    NO_QUIRKS
}

sealed class ClampingMode {

    abstract fun isAllowed(mode: ParseMode, value: Float): Boolean

    abstract fun clamp(value: Float): Float

    object All : ClampingMode() {
        override fun isAllowed(mode: ParseMode, value: Float): Boolean {
            return true
        }

        override fun clamp(value: Float): Float {
            return value
        }
    }

    object NonNegative : ClampingMode() {
        override fun isAllowed(mode: ParseMode, value: Float): Boolean {
            return value >= 0
        }

        override fun clamp(value: Float): Float {
            return if (value >= 0) {
                value
            } else {
                0f
            }
        }
    }
}
