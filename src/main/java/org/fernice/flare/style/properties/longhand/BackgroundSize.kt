/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties.longhand

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.CssWideKeyword
import org.fernice.flare.style.properties.LonghandId
import org.fernice.flare.style.properties.PropertyDeclaration
import org.fernice.flare.style.properties.PropertyEntryPoint
import org.fernice.flare.style.value.Context
import org.fernice.flare.style.value.specified.BackgroundSize
import org.fernice.flare.style.value.computed.BackgroundSize as ComputedBackgroundSize
import org.fernice.flare.style.value.toComputedValue
import fernice.std.Result

@PropertyEntryPoint
class BackgroundSizeId : LonghandId() {

    override fun name(): String {
        return "background-size"
    }

    override fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError> {
        return input.parseCommaSeparated { BackgroundSize.parse(context, it) }
                .map(::BackgroundSizeDeclaration)
    }

    override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        when (declaration) {
            is BackgroundSizeDeclaration -> {
                val computed = declaration.size.toComputedValue(context)

                context.builder.setBackgroundSize(computed)
            }
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.keyword) {
                    CssWideKeyword.UNSET,
                    CssWideKeyword.INITIAL -> {
                        context.builder.resetBackgroundSize()
                    }
                    CssWideKeyword.INHERIT -> {
                        context.builder.inheritBackgroundSize()
                    }
                }
            }
            else -> throw IllegalStateException("wrong cascade")
        }
    }

    override fun isEarlyProperty(): Boolean {
        return false
    }

    companion object {

        val instance: BackgroundSizeId by lazy { BackgroundSizeId() }
    }
}

class BackgroundSizeDeclaration(val size: List<BackgroundSize>) : PropertyDeclaration() {

    override fun id(): LonghandId {
        return BackgroundSizeId.instance
    }

    companion object {

        val initialValue: List<ComputedBackgroundSize> by lazy { listOf(ComputedBackgroundSize.auto()) }
    }
}
