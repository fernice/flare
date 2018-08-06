package de.krall.flare.style.properties

import de.krall.flare.cssparser.ParseError
import de.krall.flare.cssparser.Parser
import de.krall.flare.cssparser.Token
import de.krall.flare.cssparser.newUnexpectedTokenError
import de.krall.flare.dom.Device
import de.krall.flare.dom.Element
import de.krall.flare.font.FontMetricsProvider
import de.krall.flare.font.WritingMode
import de.krall.flare.selector.PseudoElement
import de.krall.flare.std.Empty
import de.krall.flare.std.Err
import de.krall.flare.std.None
import de.krall.flare.std.Ok
import de.krall.flare.std.Option
import de.krall.flare.std.Result
import de.krall.flare.std.Some
import de.krall.flare.std.iter.Iter
import de.krall.flare.std.iter.iter
import de.krall.flare.style.ComputedValues
import de.krall.flare.style.StyleBuilder
import de.krall.flare.style.parser.ParserContext
import de.krall.flare.style.properties.longhand.FontFamilyDeclaration
import de.krall.flare.style.properties.longhand.FontFamilyId
import de.krall.flare.style.properties.longhand.FontSizeDeclaration
import de.krall.flare.style.properties.longhand.FontSizeId
import de.krall.flare.style.ruletree.CascadeLevel
import de.krall.flare.style.ruletree.RuleNode
import de.krall.flare.style.value.Context
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

    class CssWideKeyword(val id: LonghandId, val keyword: de.krall.flare.style.properties.CssWideKeyword) : PropertyDeclaration() {

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
            println("initializing properties...")

            val ids = mutableMapOf<String, PropertyId>()

            val reflections = Reflections("de.krall.flare.style.properties")

            val types = reflections.getTypesAnnotatedWith(PropertyEntryPoint::class.java)

            for (type in types) {
                val companionType = Class.forName("${type.name}\$Companion")

                val instanceMethod = companionType.getDeclaredMethod("getInstance")

                val instance = instanceMethod.invoke(type.kotlin.companionObjectInstance)

                if (!type.isInstance(instance)) {
                    throw IllegalStateException("property id instance is expected to be of type ${type.name} but returned ${instance.javaClass.name}")
                }

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

            val properties = ids.entries.sortedBy { entry -> entry.key }

            println("Loaded ${properties.size} properties:")

            for (entry in properties) {
                val letter = if (entry.value is PropertyId.Longhand) {
                    "L"
                } else {
                    "S"
                }
                println("$letter ${entry.key}")
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

data class DeclarationAndCascadeLevel(val declaration: PropertyDeclaration, val cascadeLevel: CascadeLevel)

fun cascade(device: Device,
            element: Option<Element>,
            pseudoElement: Option<PseudoElement>,
            ruleNode: RuleNode,
            parentStyle: Option<ComputedValues>,
            parentStyleIgnoringFirstLine: Option<ComputedValues>,
            layoutStyle: Option<ComputedValues>,
            fontMetricsProvider: FontMetricsProvider): ComputedValues {

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

            declarations.filterMap<DeclarationAndCascadeLevel> { (declaration, importance) ->
                if (importance == nodeImportance) {
                    Some(DeclarationAndCascadeLevel(declaration, level))
                } else {
                    None()
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

fun applyDeclarations(device: Device,
                      element: Option<Element>,
                      pseudoElement: Option<PseudoElement>,
                      declarations: () -> Iter<DeclarationAndCascadeLevel>,
                      parentStyle: Option<ComputedValues>,
                      parentStyleIgnoringFirstLine: Option<ComputedValues>,
                      layoutStyle: Option<ComputedValues>,
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

    var fontFamily: Option<FontFamilyDeclaration> = None()
    var fontSize: Option<FontSizeDeclaration> = None()

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
        val longhandId = FontFamilyId.instance

        longhandId.cascadeProperty(fontFamily.value, context)
    }

    if (fontSize is Some) {
        val longhandId = FontSizeId.instance

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