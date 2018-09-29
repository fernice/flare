/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import fernice.std.Empty
import fernice.std.Err
import fernice.std.None
import fernice.std.Ok
import fernice.std.Option
import fernice.std.Result
import fernice.std.Some
import mu.KotlinLogging
import org.fernice.flare.Experimental
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
import org.fernice.flare.std.iter.Iter
import org.fernice.flare.std.iter.iter
import org.fernice.flare.style.ComputedValues
import org.fernice.flare.style.StyleBuilder
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.longhand.FontFamilyDeclaration
import org.fernice.flare.style.properties.longhand.FontFamilyId
import org.fernice.flare.style.properties.longhand.FontSizeDeclaration
import org.fernice.flare.style.properties.longhand.FontSizeId
import org.fernice.flare.style.properties.module.PropertyModule
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.ruletree.RuleNode
import org.fernice.flare.style.value.Context
import java.io.Writer

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
            input: Parser
        ): Result<Empty, ParseError> {
            return id.parseInto(declarations, context, input)
        }
    }
}

private val LOG = KotlinLogging.logger { }
private val REGISTERED_PROPERTIES: MutableMap<String, PropertyId> = mutableMapOf()

fun register(vararg modules: PropertyModule) {
    for (module in modules) {
        register(module)
    }
}

fun register(module: PropertyModule) {
    val experimental = module::class.java.isAnnotationPresent(Experimental::class.java)
    val hint = if (experimental) " experimental" else ""

    LOG.info { "loading$hint property module: ${module.name}" }

    for (longhand in module.longhands) {
        val property = PropertyId.Longhand(longhand)

        if (REGISTERED_PROPERTIES.containsKey(longhand.name)) {
            throw IllegalStateException("Property '${longhand.name}' has already been registered")
        }

        LOG.debug { "${longhand.name.padEnd(30)} Longhand ${longhand::class.qualifiedName}" }

        REGISTERED_PROPERTIES[longhand.name] = property
    }

    for (shorthand in module.shorthands) {
        val property = PropertyId.Shorthand(shorthand)

        if (REGISTERED_PROPERTIES.containsKey(shorthand.name)) {
            throw IllegalStateException("Property '${shorthand.name}' has already been registered")
        }

        LOG.debug { "${shorthand.name.padEnd(30)} Shorthand ${shorthand::class.qualifiedName}" }

        REGISTERED_PROPERTIES[shorthand.name] = property
    }
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

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError>

    abstract val longhands: List<LonghandId>

    abstract val name: String
}

sealed class PropertyId {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError>

    class Longhand(private val id: LonghandId) : PropertyId() {

        override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
            val keyword = input.tryParse { CssWideKeyword.parse(it) }

            return when (keyword) {
                is Ok -> {
                    declarations.add(PropertyDeclaration.CssWideKeyword(id, keyword.value))

                    Ok()
                }
                is Err -> {
                    val declaration = input.parseEntirely { id.parseValue(context, input) }

                    when (declaration) {
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

        override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
            val keyword = input.tryParse { CssWideKeyword.parse(it) }

            return when (keyword) {
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

        fun parse(name: String): Result<PropertyId, Empty> {
            val result = REGISTERED_PROPERTIES[name.toLowerCase()]

            return if (result != null) {
                Ok(result)
            } else {
                Err()
            }
        }
    }
}

sealed class CssWideKeyword : ToCss {

    object Unset : CssWideKeyword()

    object Initial : CssWideKeyword()

    object Inherit : CssWideKeyword()

    override fun toCss(writer: Writer) {
        writer.write(
            when (this) {
                CssWideKeyword.Unset -> "unset"
                CssWideKeyword.Initial -> "initial"
                CssWideKeyword.Inherit -> "inherit"
            }
        )
    }

    companion object {

        fun parse(input: Parser): Result<CssWideKeyword, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "unset" -> Ok(CssWideKeyword.Unset)
                "initial" -> Ok(CssWideKeyword.Initial)
                "inherit" -> Ok(CssWideKeyword.Inherit)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}

data class DeclarationAndCascadeLevel(val declaration: PropertyDeclaration, val cascadeLevel: CascadeLevel)

fun cascade(
    device: Device,
    element: Option<Element>,
    pseudoElement: Option<PseudoElement>,
    ruleNode: RuleNode,
    parentStyle: Option<ComputedValues>,
    parentStyleIgnoringFirstLine: Option<ComputedValues>,
    layoutStyle: Option<ComputedValues>,
    fontMetricsProvider: FontMetricsProvider
): ComputedValues {

    val iter = {
        ruleNode.selfAndAncestors().flatMap { node ->
            val level = node.cascadeLevel()
            val source = node.styleSource()

            val declarations = if (source is Some) {
                source.value.declarations().reversedDeclarationImportanceIter()
            } else {
                DeclarationImportanceIter(listOf<DeclarationAndImportance>().iter())
            }

            val nodeImportance = node.importance()

            declarations.filterMap { (declaration, importance) ->
                if (importance == nodeImportance) {
                    Some(DeclarationAndCascadeLevel(declaration, level))
                } else {
                    None
                }
            }
        }
    }

    return applyDeclarations(
        device,
        element,
        pseudoElement,
        iter,
        parentStyle,
        parentStyleIgnoringFirstLine,
        layoutStyle,
        fontMetricsProvider
    )
}

fun applyDeclarations(
    device: Device,
    element: Option<Element>,
    pseudoElement: Option<PseudoElement>,
    declarations: () -> Iter<DeclarationAndCascadeLevel>,
    parentStyle: Option<ComputedValues>,
    parentStyleIgnoringFirstLine: Option<ComputedValues>,
    layoutStyle: Option<ComputedValues>,
    fontMetricsProvider: FontMetricsProvider
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

    var fontFamily: Option<FontFamilyDeclaration> = None
    var fontSize: Option<FontSizeDeclaration> = None

    for ((declaration, _) in declarations()) {
        val longhandId = declaration.id()

        if (!longhandId.isEarlyProperty()) {
            continue
        }

        if (!seen.add(longhandId)) {
            continue
        }

        if (longhandId is FontFamilyId) {
            fontFamily = Some(declaration as FontFamilyDeclaration)
            continue
        }

        if (longhandId is FontSizeId) {
            fontSize = Some(declaration as FontSizeDeclaration)
            continue
        }

        longhandId.cascadeProperty(declaration, context)
    }

    if (fontFamily is Some) {
        val longhandId = FontFamilyId

        longhandId.cascadeProperty(fontFamily.value, context)
    }

    if (fontSize is Some) {
        val longhandId = FontSizeId

        longhandId.cascadeProperty(fontSize.value, context)
    }

    for ((declaration, _) in declarations()) {
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
