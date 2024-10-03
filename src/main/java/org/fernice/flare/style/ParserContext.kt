/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.SourceLocation
import org.fernice.flare.style.stylesheet.CssRuleType
import org.fernice.flare.style.stylesheet.CssRuleTypes
import org.fernice.flare.url.Url
import org.fernice.logging.FLogging
import org.fernice.std.Result
import org.fernice.std.with

class ParserContext(
    val origin: Origin,
    val urlData: Url,
    var ruleTypes: CssRuleTypes,
    val parseMode: ParseMode,
    val quirksMode: QuirksMode,
) {

    fun <R> nestForRule(ruleType: CssRuleType, block: (ParserContext) -> R): R {
        val previousRuleTypes = ruleTypes
        ruleTypes = ruleTypes.with(ruleType)
        val result = block(this)
        ruleTypes = previousRuleTypes
        return result
    }

    fun isErrorReportingEnabled(): Boolean = LOG.isDebugEnabled

    fun reportError(location: SourceLocation, error: ContextualError) {
        if (!isErrorReportingEnabled()) return

        LOG.warn("declaration parse error at $location: $error")
    }

    companion object {

        fun from(
            origin: Origin,
            urlData: Url,
            ruleType: CssRuleType?,
            parseMode: ParseMode,
            quirksMode: QuirksMode,
        ): ParserContext {
            return ParserContext(
                origin = origin,
                urlData = urlData,
                ruleTypes = ruleType?.let { CssRuleTypes.of(it) } ?: CssRuleTypes(),
                parseMode = parseMode,
                quirksMode = quirksMode,
            )
        }

        private val LOG = FLogging.logger { }
    }
}

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
            return quirksMode == QuirksMode.Quirks
        }
    }

    object No : AllowQuirks() {
        override fun allowed(quirksMode: QuirksMode): Boolean {
            return false
        }
    }
}

enum class QuirksMode {

    Quirks,

    LimitedQuirks,

    NoQuirks,
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
