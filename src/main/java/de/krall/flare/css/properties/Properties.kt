package de.krall.flare.css.properties

import de.krall.flare.css.ComputedValues
import de.krall.flare.css.StyleBuilder
import de.krall.flare.css.parser.ParserContext
import de.krall.flare.css.properties.longhand.FontFamilyId
import de.krall.flare.css.properties.longhand.FontSizeId
import de.krall.flare.css.value.Context
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.dom.Device
import de.krall.flare.dom.Element
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.font.WritingMode
import de.krall.flare.std.*
import org.reflections.Reflections
import kotlin.reflect.full.companionObjectInstance

/**
 * Marks an entry point to a property declaration. Currently only [LonghandId] and [ShorthandId]
 * are valid entry points.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyEntryPoint

abstract class PropertyDeclaration {

    /**
     * Returns the name of the property
     */
    abstract fun id(): LonghandId

    class CssWideKeyword(val id: LonghandId, val keyword: de.krall.flare.css.properties.CssWideKeyword) : PropertyDeclaration() {

        override fun id(): LonghandId {
            return id
        }
    }

    companion object {

        fun parseInto(declarations: MutableList<PropertyDeclaration>,
                      id: PropertyId,
                      context: ParserContext,
                      input: Parser): Result<Empty, ParseError> {
            return id.parseInto(declarations, context, input)
        }
    }
}

abstract class LonghandId {

    /**
     * Returns the name of the property.
     */
    abstract fun name(): String

    abstract fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError>

    abstract fun cascadeProperty(declaration: PropertyDeclaration, context: Context)

    abstract fun isEarlyProperty(): Boolean
}

abstract class ShorthandId {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError>

    abstract fun getLonghands(): List<LonghandId>

    abstract fun name(): String
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
                    for (longhand in id.getLonghands()) {
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

        val ids: Map<String, PropertyId> by lazy { indexProperties() }

        private fun indexProperties(): Map<String, PropertyId> {
            val ids = mutableMapOf<String, PropertyId>()

            val reflections = Reflections("de.krall.flare.css.properties")

            val types = reflections.getTypesAnnotatedWith(PropertyEntryPoint::class.java)

            for (type in types) {
                val companionType = Class.forName("${type.name}\$Companion")

                val instanceMethod = companionType.getDeclaredMethod("getInstance")

                val instance = instanceMethod.invoke(type.kotlin.companionObjectInstance)

                val (name, id) = when (instance) {
                    is LonghandId -> {
                        Pair(instance.name(), PropertyId.Longhand(instance))
                    }
                    is ShorthandId -> {
                        Pair(instance.name(), PropertyId.Shorthand(instance))
                    }
                    else -> {
                        throw IllegalStateException("unsupported property entry point")
                    }
                }

                if (ids.containsKey(name)) {
                    throw IllegalStateException("duplicated property: $name")
                }

                ids[name] = id
            }

            return ids
        }

        fun parse(name: String): Result<PropertyId, Empty> {
            val result = ids[name.toLowerCase()]

            return if (result != null) {
                Ok(result)
            } else {
                Err()
            }
        }
    }
}

enum class CssWideKeyword {

    UNSET,

    INITIAL,

    INHERIT;

    companion object {

        fun parse(input: Parser): Result<CssWideKeyword, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "unset" -> Ok(CssWideKeyword.UNSET)
                "initial" -> Ok(CssWideKeyword.INITIAL)
                "inherit" -> Ok(CssWideKeyword.INHERIT)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}

fun cascade(device: Device,
            element: Option<Element>,
            declarations: List<PropertyDeclaration>,
            parentStyle: Option<ComputedValues>,
            parentStyleIgnoringFirstLine: Option<ComputedValues>,
            fontMetricsProvider: FontMetricsProvider): ComputedValues {

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

    var fontFamily: Option<PropertyDeclaration> = None()
    var fontSize: Option<PropertyDeclaration> = None()

    for (declaration in declarations) {
        val longhandId = declaration.id()

        if (!longhandId.isEarlyProperty()) {
            continue
        }

        if (!seen.add(longhandId)) {
            continue
        }

        if (longhandId is FontSizeId) {
            fontFamily = Some(declaration)
            continue
        }

        if (longhandId is FontSizeId) {
            fontSize = Some(declaration)
            continue
        }

        longhandId.cascadeProperty(declaration, context)
    }

    if (fontFamily is Some) {
        val longhandId = FontFamilyId.instance

        longhandId.cascadeProperty(fontFamily.value, context)
    }

    if (fontSize is Some) {
        val longhandId = FontSizeId.instance

        longhandId.cascadeProperty(fontSize.value, context)
    }

    for (declaration in declarations) {
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