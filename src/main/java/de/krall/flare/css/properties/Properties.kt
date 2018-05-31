package de.krall.flare.css.properties

import de.krall.flare.css.ParserContext
import de.krall.flare.css.value.Context
import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.Ok
import de.krall.flare.std.Result
import org.reflections.Reflections
import kotlin.reflect.full.companionObjectInstance
import de.krall.flare.css.properties.CssWideKeyword as DeclaredCssWideKeyword
import de.krall.flare.css.properties.LonghandId as PropertyLonghandId
import de.krall.flare.css.properties.ShorthandId as PropertyShorthandId

/**
 * Marks an entry point to a property declaration. Currently only [PropertyLonghandId] and [PropertyShorthandId]
 * are valid entry points.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PropertyEntryPoint

abstract class PropertyDeclaration {

    /**
     * Returns the name of the property
     */
    abstract fun id(): PropertyLonghandId

    class CssWideKeyword(val id: PropertyLonghandId, val keyword: DeclaredCssWideKeyword) : PropertyDeclaration() {

        override fun id(): PropertyLonghandId {
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
}

abstract class ShorthandId {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError>

    abstract fun getLonghands(): List<PropertyLonghandId>

    abstract fun name(): String
}

sealed class PropertyId {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError>

    class Longhand(private val id: PropertyLonghandId) : PropertyId() {

        override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
            val keyword = input.tryParse { DeclaredCssWideKeyword.parse(it) }

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

    class Shorthand(private val id: PropertyShorthandId) : PropertyId() {

        override fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Empty, ParseError> {
            val keyword = input.tryParse { DeclaredCssWideKeyword.parse(it) }

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
                    is PropertyLonghandId -> {
                        Pair(instance.name(), PropertyId.Longhand(instance))
                    }
                    is PropertyShorthandId -> {
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

fun main(args: Array<String>) {
    val a = PropertyId.ids["background-attachment"]

    println(a)
}

enum class CssWideKeyword {

    UNSET,

    INITIAL,

    INHERIT;

    companion object {

        fun parse(input: Parser): Result<DeclaredCssWideKeyword, ParseError> {
            val location = input.sourceLocation()
            val identifierResult = input.expectIdentifier()

            val identifier = when (identifierResult) {
                is Ok -> identifierResult.value
                is Err -> return identifierResult
            }

            return when (identifier.toLowerCase()) {
                "unset" -> Ok(DeclaredCssWideKeyword.UNSET)
                "initial" -> Ok(DeclaredCssWideKeyword.INITIAL)
                "inherit" -> Ok(DeclaredCssWideKeyword.INHERIT)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}