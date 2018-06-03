package de.krall.flare.style.parser

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.std.Result

class ParserContext(val parseMode: ParseMode, val quirksMode: QuirksMode)

interface Parse<T> {

    fun parse(context: ParserContext, input: Parser): Result<T, ParseError>
}

interface ParseQuirky<T> {

    fun parseQuirky(context: ParserContext, input: Parser, allowQuirks: AllowQuirks): Result<T, ParseError>
}

sealed class ParseMode {

    abstract fun allowsUnitlessNumbers(): Boolean

    class Default : ParseMode() {
        override fun allowsUnitlessNumbers(): Boolean {
            return false
        }
    }

    class UnitlessLength : ParseMode() {
        override fun allowsUnitlessNumbers(): Boolean {
            return true
        }
    }
}

sealed class AllowQuirks {

    abstract fun allowed(quirksMode: QuirksMode): Boolean

    class Yes : AllowQuirks() {
        override fun allowed(quirksMode: QuirksMode): Boolean {
            return quirksMode == QuirksMode.QUIRKS
        }
    }

    class No : AllowQuirks() {
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

    class All : ClampingMode() {
        override fun isAllowed(mode: ParseMode, value: Float): Boolean {
            return true
        }

        override fun clamp(value: Float): Float {
            return value
        }
    }

    class NonNegative : ClampingMode() {
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