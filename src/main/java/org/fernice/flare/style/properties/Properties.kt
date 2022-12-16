/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.properties

import org.fernice.flare.cssparser.ParseError
import org.fernice.flare.cssparser.Parser
import org.fernice.flare.cssparser.ToCss
import org.fernice.flare.cssparser.Token
import org.fernice.flare.cssparser.newUnexpectedTokenError
import org.fernice.flare.style.parser.ParserContext
import org.fernice.flare.style.properties.custom.Name
import org.fernice.flare.style.properties.custom.SubstitutionCache
import org.fernice.flare.style.properties.custom.UnparsedValue
import org.fernice.flare.style.properties.custom.TemplateValue
import org.fernice.flare.style.properties.custom.VariableValue
import org.fernice.flare.style.value.Context
import org.fernice.std.Err
import org.fernice.std.Ok
import org.fernice.std.Result
import org.fernice.std.map
import org.fernice.std.unwrap
import org.fernice.std.unwrapErr
import java.io.Writer
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

abstract class LonghandId(
    val name: String,
    val isInherited: Boolean,
    val isEarlyProperty: Boolean = false,
) : Comparable<LonghandId> {

    abstract fun parseValue(context: ParserContext, input: Parser): Result<PropertyDeclaration, ParseError>

    abstract fun cascadeProperty(declaration: PropertyDeclaration, context: Context)

    final override fun compareTo(other: LonghandId): Int {
        return name.compareTo(other.name)
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LonghandId) return false

        val allocation = allocation.get()
        if (allocation > 0 && allocation == other.allocation.get()) return true

        return name == other.name
    }

    final override fun hashCode(): Int {
        return allocation.hashCode()
    }

    final override fun toString(): String = name

    private val allocation = AtomicInteger(-1)
    val ordinal: Int
        get() {
            val ordinal = allocation.get()
            if (ordinal == -1) error("LonghandId $this was never allocated")
            return ordinal
        }

    internal fun allocate(ordinal: Int) {
        if (!allocation.compareAndSet(-1, ordinal)) {
            error("LonghandId $this has already been allocated")
        }
    }
}

abstract class AbstractLonghandId<T : PropertyDeclaration>(
    name: String,
    declarationType: KClass<T>,
    isInherited: Boolean,
    isEarlyProperty: Boolean = false,
) : LonghandId(name, isInherited, isEarlyProperty) {

    private val declarationType: Class<T> = declarationType.java

    abstract override fun parseValue(context: ParserContext, input: Parser): Result<T, ParseError>

    final override fun cascadeProperty(declaration: PropertyDeclaration, context: Context) {
        if (declarationType.isInstance(declaration)) {
            cascadeProperty(context, declarationType.cast(declaration))
        }
        when (declaration) {
            is PropertyDeclaration.CssWideKeyword -> {
                when (declaration.declaration.keyword) {
                    CssWideKeyword.Unset -> {
                        if (!isInherited) {
                            resetProperty(context)
                        } else {
                            inheritProperty(context)
                        }
                    }

                    CssWideKeyword.Initial -> {
                        if (!isInherited) error("keyword 'initial' should have been handled by caller")
                        resetProperty(context)
                    }

                    CssWideKeyword.Inherit -> {
                        if (isInherited) error("keyword 'inherit' should have been handled by caller")
                        inheritProperty(context)
                    }

                    CssWideKeyword.Revert -> error("keyword 'revert' should have been handled by caller")
                }
            }

            is PropertyDeclaration.WithVariables -> error("variables should have already been substituted")
        }
    }

    protected abstract fun cascadeProperty(context: Context, declaration: T)
    protected abstract fun resetProperty(context: Context)
    protected abstract fun inheritProperty(context: Context)
}

class LonghandIdSet : AbstractMutableSet<LonghandId>() {
    private val allocation = Properties.longhandIds.size
    private val storage = LongArray(allocation % 64)

    private fun get(ordinal: Int): Boolean {
        val bin = ordinal % 64
        val bit = ordinal / 64
        val bits = storage[bin]
        return (bits and (1L shl bit)) != 0L
    }

    private fun set(ordinal: Int): Boolean {
        val bin = ordinal % 64
        val bit = ordinal / 64
        val bits = storage[bin]
        if ((bits and (1L shl bit)) == 0L) {
            storage[bin] = bits or (1L shl bit)
            return true
        }
        return false
    }

    private fun clear(ordinal: Int): Boolean {
        val bin = ordinal % 64
        val bit = ordinal / 64
        val bits = storage[bin]
        if ((bits and (1L shl bit)) != 0L) {
            storage[bin] = bits and (1L shl bit).inv()
            return true
        }
        return false
    }

    override fun clear() {
        for (bin in storage.indices) {
            storage[bin] = 0L
        }
    }

    override fun isEmpty(): Boolean {
        for (bin in storage.indices) {
            if (storage[bin] != 0L) return false
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LonghandIdSet) return false
        if (!super.equals(other)) return false

        if (!storage.contentEquals(other.storage)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + storage.contentHashCode()
        return result
    }

    override val size: Int
        get() = count()

    override fun contains(element: LonghandId): Boolean = get(element.ordinal)

    override fun add(element: LonghandId): Boolean = set(element.ordinal)
    override fun remove(element: LonghandId): Boolean = clear(element.ordinal)

    override fun iterator(): MutableIterator<LonghandId> = SetIterator()

    private inner class SetIterator : MutableIterator<LonghandId> {
        private var ordinal = 0

        override fun hasNext(): Boolean {
            while (ordinal < allocation) {
                if (get(ordinal)) return true
                ordinal++
            }
            return false
        }

        override fun next(): LonghandId {
            if (!hasNext()) throw NoSuchElementException()
            return Properties.longhandIds[ordinal++]
        }

        override fun remove() {
            if (ordinal <= 0) error("next() has not been called yet")
            clear(ordinal - 1)
        }
    }
}

abstract class ShorthandId(
    val name: String,
    val longhands: List<LonghandId>,
) : Comparable<ShorthandId> {

    abstract fun parseInto(declarations: MutableList<PropertyDeclaration>, context: ParserContext, input: Parser): Result<Unit, ParseError>

    final override fun compareTo(other: ShorthandId): Int = name.compareTo(other.name)

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShorthandId) return false

        if (name != other.name) return false

        return true
    }

    final override fun hashCode(): Int {
        return name.hashCode()
    }

    final override fun toString(): String = name
}

sealed class PropertyId {

    data class Longhand(val id: LonghandId) : PropertyId() {

        override fun toString(): String {
            return "PropertyId::Longhand($id)"
        }
    }

    data class Shorthand(val id: ShorthandId) : PropertyId() {

        override fun toString(): String {
            return "PropertyId::Shorthand($id)"
        }
    }

    data class Custom(val name: Name) : PropertyId() {

        override fun toString(): String {
            return "PropertyId::Custom($name)"
        }
    }

    companion object {

        fun parse(propertyName: String): Result<PropertyId, Unit> {
            val result = Properties[propertyName.lowercase()]

            if (result != null) return Ok(result)

            val name = Name.parse(propertyName).unwrap { return it }
            return Ok(PropertyId.Custom(name))
        }
    }
}

object Properties {

    private val properties: Map<String, PropertyId>

    val longhandIds: Array<out LonghandId>

    init {
        val propertyRegistryContainer = PropertyContainer()

        val classLoader = PropertyId::class.java.classLoader
        val containerContributorLoader = ServiceLoader.load(PropertyContainerContributor::class.java, classLoader)

        for (containerContributor in containerContributorLoader) {
            containerContributor.contribute(propertyRegistryContainer)
        }

        properties = propertyRegistryContainer.getRegisteredProperties()

        longhandIds = properties.values.asSequence()
            .filterIsInstance<PropertyId.Longhand>()
            .map { it.id }
            .onEachIndexed { index, longhandId -> longhandId.allocate(index) }
            .toList()
            .toTypedArray()
    }

    operator fun get(name: String): PropertyId? {
        return properties[name]
    }
}

sealed class PropertyDeclarationId {
    data class Longhand(val id: LonghandId) : PropertyDeclarationId()
    data class Custom(val name: Name) : PropertyDeclarationId()
}

fun PropertyDeclarationId.toCss(writer: Writer) {
    when (this) {
        is PropertyDeclarationId.Longhand -> writer.append(id.name)
        is PropertyDeclarationId.Custom -> writer.append("--").append(name.value)
    }
}

abstract class PropertyDeclaration(
    val id: PropertyDeclarationId,
) : ToCss {

    data class CssWideKeyword(val declaration: CssWideKeywordDeclaration) : PropertyDeclaration(PropertyDeclarationId.Longhand(declaration.id)) {

        override fun toCssInternally(writer: Writer) = declaration.keyword.toCss(writer)
    }

    data class WithVariables(val declaration: VariablesDeclaration) : PropertyDeclaration(PropertyDeclarationId.Longhand(declaration.id)) {

        override fun toCssInternally(writer: Writer) {}
    }

    data class Custom(val declaration: CustomDeclaration) : PropertyDeclaration(PropertyDeclarationId.Custom(declaration.name)) {

        override fun toCssInternally(writer: Writer) {}
    }

    protected abstract fun toCssInternally(writer: Writer)

    final override fun toCss(writer: Writer) {
        id.toCss(writer)
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
            return when (id) {
                is PropertyId.Custom -> {
                    input.tryParse { org.fernice.flare.style.properties.CssWideKeyword.parse(it) }
                        .map { CustomDeclaration(id.name, CustomDeclarationValue.CssWideKeyword(it)) }
                        .map { declarations.add(PropertyDeclaration.Custom(it)) }
                        .unwrapErr { return Ok() }

                    TemplateValue.parse(input)
                        .map { VariableValue(it) }
                        .map { CustomDeclaration(id.name, CustomDeclarationValue.Value(it)) }
                        .map { declarations.add(PropertyDeclaration.Custom(it)) }
                        .unwrap { return it }

                    Ok()
                }

                is PropertyId.Longhand -> {
                    input.skipWhitespace()

                    input.tryParse { org.fernice.flare.style.properties.CssWideKeyword.parse(it) }
                        .map { CssWideKeywordDeclaration(id.id, it) }
                        .map { declarations.add(PropertyDeclaration.CssWideKeyword(it)) }
                        .unwrapErr { return Ok() }

                    // try parse, but we're looking for var() functions
                    val state = input.state()
                    input.lookForVarFunctions()

                    val error = input.parseEntirely { id.id.parseValue(context, input) }
                        .map { declarations.add(it) }
                        .unwrapErr { return Ok() }

                    while (input.next().isOk()) {
                    }

                    if (!input.seenVarFunctions()) return Err(error)
                    input.reset(state)

                    TemplateValue.parse(input)
                        .map { UnparsedValue(it, context.baseUrl, fromShorthand = null) }
                        .map { VariablesDeclaration(id.id, it) }
                        .map { declarations.add(PropertyDeclaration.WithVariables(it)) }
                        .unwrap { return it }

                    Ok()
                }

                is PropertyId.Shorthand -> {
                    input.skipWhitespace()

                    input.tryParse { org.fernice.flare.style.properties.CssWideKeyword.parse(it) }
                        .map { keyword -> id.id.longhands.map { longhand -> CssWideKeywordDeclaration(longhand, keyword) } }
                        .map { values -> values.forEach { declarations.add(PropertyDeclaration.CssWideKeyword(it)) } }
                        .unwrapErr { return Ok() }

                    // try parse, but we're looking for var() functions
                    val state = input.state()
                    input.lookForVarFunctions()

                    val error = input.parseEntirely { id.id.parseInto(declarations, context, input) }
                        .unwrapErr { return Ok() }

                    while (input.next().isOk()) {
                    }

                    if (!input.seenVarFunctions()) return Err(error)
                    input.reset(state)

                    TemplateValue.parse(input)
                        .map { UnparsedValue(it, context.baseUrl, fromShorthand = id.id) }
                        .map { value -> id.id.longhands.map { longhand -> VariablesDeclaration(longhand, value) } }
                        .map { values -> values.forEach { declarations.add(PropertyDeclaration.WithVariables(it)) } }
                        .unwrap { return it }

                    Ok()
                }
            }
        }
    }
}

data class CssWideKeywordDeclaration(
    val id: LonghandId,
    val keyword: CssWideKeyword,
)

/**
 * Represents the three universally definable property value keywords.
 */
enum class CssWideKeyword : ToCss {

    /**
     * The `inherit` CSS keyword causes the element for which it is specified to take the computed value of the property
     * from its parent element. It can be applied to any CSS property, including the CSS shorthand `all`.
     *
     * For inherited properties this keyword behaves the same as `unset`, on non-inherited properties it causes an
     * explicit inheritance.
     */
    Unset,

    /**
     * The `unset` CSS keyword resets a property to its inherited value if it inherits from its parent, and to its initial
     * value if not. In other words, it behaves like the inherit keyword in the first case, and like the initial keyword
     * in the second case. It can be applied to any CSS property, including the CSS shorthand `all`.
     */
    Initial,

    /**
     * The `initial` CSS keyword applies the initial (or default) value of a property to an element. It can be applied to
     * any CSS property. This includes the CSS shorthand `all`, with which `initial` can be used to restore all CSS
     * properties to their initial state.
     */
    Inherit,

    Revert;

    override fun toCss(writer: Writer) {
        writer.write(
            when (this) {
                Unset -> "unset"
                Initial -> "initial"
                Inherit -> "inherit"
                Revert -> "revert"
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
                "revert" -> Ok(Revert)
                else -> Err(location.newUnexpectedTokenError(Token.Identifier(identifier)))
            }
        }
    }
}

data class VariablesDeclaration(
    val id: LonghandId,
    val unparsedValue: UnparsedValue,
) {

    fun substituteVariables(
        customProperties: CustomPropertiesList?,
        substitutionCache: SubstitutionCache,
    ): PropertyDeclaration {
        return unparsedValue.substituteVariables(id, customProperties, substitutionCache)
    }
}

data class CustomDeclaration(
    val name: Name,
    val value: CustomDeclarationValue,
)

sealed class CustomDeclarationValue {
    data class Value(val value: VariableValue) : CustomDeclarationValue()
    data class CssWideKeyword(val keyword: org.fernice.flare.style.properties.CssWideKeyword) : CustomDeclarationValue()
}
