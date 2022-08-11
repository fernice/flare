/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.font.WritingMode
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.style.ComputedValues
import org.fernice.flare.style.StyleBuilder
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.longhand.font.FontFamilyDeclaration
import org.fernice.flare.style.properties.longhand.font.FontFamilyId
import org.fernice.flare.style.properties.longhand.font.FontSizeDeclaration
import org.fernice.flare.style.properties.longhand.font.FontSizeId
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.ruletree.RuleNode
import org.fernice.flare.style.value.Context
import org.fernice.logging.FLogging
import java.io.Writer
import java.util.ServiceLoader

private val LOG = FLogging.logger { }

abstract class PropertyDeclaration : ToCss {

    class CssWideKeyword(val id: LonghandId, val keyword: org.fernice.flare.style.properties.CssWideKeyword) : PropertyDeclaration() {

        override fun id(): LonghandId {
            return id
        }

        override fun toCssInternally(writer: Writer) = keyword.toCss(writer)
    }

    /**
     * Returns the name of the property
     */
    abstract fun id(): LonghandId

    protected abstract fun toCssInternally(writer: Writer)

    final override fun toCss(writer: Writer) {
        writer.append(id().name)
        writer.append(": ")
        toCssInternally(writer)
        writer.append(';')
    }

    companion object {

        fun parseInto(
            declarations: MutableList<PropertyDeclaration>,
            id: PropertyId,
            context: ParserContext,
            input: Parser,
        ): Result<Unit, ParseError> {
            return id.parseInto(declarations, context, input)
        }
    }
}

private val REGISTERED_PROPERTIES: MutableMap<String, PropertyId> by lazy {
    val propertyRegistryContainer = PropertyContainer()

    val classLoader = PropertyId::class.java.classLoader
    val containerContributorLoader = ServiceLoader.load(PropertyContainerContributor::class.java, classLoader)

    for (containerContributor in containerContributorLoader) {
        containerContributor.contribute(propertyRegistryContainer)
    }

    propertyRegistryContainer.getRegisteredProperties()
}


abstract class LonghandId {

    /**
     * Returns the name of the property.
     */
    abstract val name: String

    abstract fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError>

    abstract fun cascadeProperty(declaration: PropertyDeclaration, context: Context)

    abstract fun isEarlyProperty(): Boolean
}

abstract class ShorthandId {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError>

    abstract val longhands: List<LonghandId>

    abstract val name: String
}

sealed class PropertyId {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError>

    class Longhand(private val id: LonghandId) : PropertyId() {

        override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
            return when (val keyword = input.tryParse { CssWideKeyword.parse(it) }) {
                is Ok -> {
                    declarations.add(PropertyDeclaration.CssWideKeyword(id, keyword.value))

                    Ok()
                }
                is Err -> {
                    when (val declaration = input.parseEntirely { id.parseValue(context, input) }) {
                        is Ok -> {
                            declarations.add(declaration.value)

                            Ok()
                        }
                        is Err -> {
                            declaration
                        }
                    }
                }
            }
        }

        override fun toString(): String {
            return "PropertyId::Longhand($id)"
        }
    }

    class Shorthand(private val id: ShorthandId) : PropertyId() {

        override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError> {
            return when (val keyword = input.tryParse { CssWideKeyword.parse(it) }) {
                is Ok -> {
                    for (longhand in id.longhands) {
                        declarations.add(PropertyDeclaration.CssWideKeyword(longhand, keyword.value))
                    }

                    Ok()
                }
                is Err -> {
                    input.parseEntirely { id.parseInto(declarations, context, input) }
                }
            }
        }

        override fun toString(): String {
            return "PropertyId::Longhand($id)"
        }
    }

    companion object {

        fun parse(name: String): Result<PropertyId, Unit> {
            val result = REGISTERED_PROPERTIES[name.lowercase()]

            return if (result != null) {
                Ok(result)
            } else {
                Err()
            }
        }
    }
}

/**
 * Represents the three universally definable property value keywords.
 */
sealed class CssWideKeyword : ToCss {

    /**
     * The `inherit` CSS keyword causes the element for which it is specified to take the computed value of the property
     * from its parent element. It can be applied to any CSS property, including the CSS shorthand `all`.
     *
     * For inherited properties this keyword behaves the same as `unset`, on non-inherited properties it causes an
     * explicit inheritance.
     */
    object Unset : CssWideKeyword()

    /**
     * The `unset` CSS keyword resets a property to its inherited value if it inherits from its parent, and to its initial
     * value if not. In other words, it behaves like the inherit keyword in the first case, and like the initial keyword
     * in the second case. It can be applied to any CSS property, including the CSS shorthand `all`.
     */
    object Initial : CssWideKeyword()

    /**
     * The `initial` CSS keyword applies the initial (or default) value of a property to an element. It can be applied to
     * any CSS property. This includes the CSS shorthand `all`, with which `initial` can be used to restore all CSS
     * properties to their initial state.
     */
    object Inherit : CssWideKeyword()

    override fun toCss(writer: Writer) {
        writer.write(
            when (this) {
                Unset -> "unset"
                Initial -> "initial"
                Inherit -> "inherit"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<CssWideKeyword, ParseError> {
            val location = input.sourceLocation()

            val identifier = when (val identifier = input.expectIdentifier()) {
                is Ok -> identifier.value
                is Err -> return identifier
            }

            return when (identifier.lowercase()) {
                "unset" -> Ok(Unset)
                "initial" -> Ok(Initial)
                "inherit" -> Ok(Inherit)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}

data class DeclarationAndCascadeLevel(val declaration: PropertyDeclaration, val cascadeLevel: CascadeLevel)

fun cascade(
    device: Device,
    element: Element?,
    pseudoElement: PseudoElement?,
    ruleNode: RuleNode,
    parentStyle: ComputedValues?,
    parentStyleIgnoringFirstLine: ComputedValues?,
    layoutStyle: ComputedValues?,
    fontMetricsProvider: FontMetricsProvider,
): ComputedValues {

    val sequence = ruleNode.selfAndAncestors().flatMap { node ->
        val level = node.level

        val declarations = when (val source = node.source) {
            null -> DeclarationImportanceSequence(emptySequence())
            else -> source.declarations().reversedDeclarationImportanceSequence()
        }

        val nodeImportance = node.importance

        declarations.filter { (_, importance) -> importance == nodeImportance }
            .map { (declaration, _) -> DeclarationAndCascadeLevel(declaration, level) }
    }

    return applyDeclarations(
        device,
        element,
        pseudoElement,
        sequence,
        parentStyle,
        parentStyleIgnoringFirstLine,
        layoutStyle,
        fontMetricsProvider
    )
}

fun applyDeclarations(
    device: Device,
    element: Element?,
    pseudoElement: PseudoElement?,
    declarations: Sequence<DeclarationAndCascadeLevel>,
    parentStyle: ComputedValues?,
    parentStyleIgnoringFirstLine: ComputedValues?,
    layoutStyle: ComputedValues?,
    fontMetricsProvider: FontMetricsProvider,
): ComputedValues {
    val context = Context(
        false,

        StyleBuilder.new(
            device,
            WritingMode(0),
            parentStyle,
            parentStyleIgnoringFirstLine
        ),
        fontMetricsProvider
    )

    val seen = mutableSetOf<LonghandId>()

    var fontFamily: FontFamilyDeclaration? = null
    var fontSize: FontSizeDeclaration? = null

    for ((declaration, _) in declarations) {
        val longhandId = declaration.id()

        if (!longhandId.isEarlyProperty()) {
            continue
        }

        if (!seen.add(longhandId)) {
            continue
        }

        if (longhandId is FontFamilyId) {
            fontFamily = declaration as FontFamilyDeclaration
            continue
        }

        if (longhandId is FontSizeId) {
            fontSize = declaration as FontSizeDeclaration
            continue
        }

        longhandId.cascadeProperty(declaration, context)
    }

    if (fontFamily != null) {
        val longhandId = FontFamilyId

        longhandId.cascadeProperty(fontFamily, context)
    }

    if (fontSize != null) {
        val longhandId = FontSizeId

        longhandId.cascadeProperty(fontSize, context)
    }

    for ((declaration, _) in declarations) {
        val longhandId = declaration.id()

        if (longhandId.isEarlyProperty()) {
            continue
        }

        if (!seen.add(longhandId)) {
            continue
        }

        longhandId.cascadeProperty(declaration, context)
    }

    return context.builder.build()
}
