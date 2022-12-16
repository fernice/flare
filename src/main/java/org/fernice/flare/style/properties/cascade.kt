/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.properties

import org.fernice.flare.dom.Device
import org.fernice.flare.dom.Element
import org.fernice.flare.font.FontMetricsProvider
import org.fernice.flare.selector.PseudoElement
import org.fernice.flare.style.ComputedValues
import org.fernice.flare.style.StyleBuilder
import org.fernice.flare.style.properties.custom.Name
import org.fernice.flare.style.properties.custom.SubstitutionCache
import org.fernice.flare.style.properties.custom.VariableValue
import org.fernice.flare.style.ruletree.CascadeLevel
import org.fernice.flare.style.ruletree.RuleNode
import org.fernice.flare.style.PerOrigin
import org.fernice.flare.style.value.Context

data class DeclarationAndCascadeLevel(val declaration: PropertyDeclaration, val cascadeLevel: CascadeLevel)

fun cascade(
    device: Device,
    element: Element,
    pseudoElement: PseudoElement?,
    ruleNode: RuleNode,
    previousStyle: ComputedValues?,
    parentStyle: ComputedValues?,
    parentStyleIgnoringFirstLine: ComputedValues?,
    fontMetricsProvider: FontMetricsProvider,
): ComputedValues {
    val declarations = ruleNode.selfAndAncestors().flatMap { node ->
        val level = node.level

        val sequence = when (val declarations = node.declarations?.get()) {
            null -> emptySequence()
            else -> declarations.asSequence(reversed = true)
        }

        sequence.map { DeclarationAndCascadeLevel(it, level) }
    }

    return applyDeclarations(
        device,
        element,
        pseudoElement,
        ruleNode,
        declarations,
        previousStyle,
        parentStyle,
        parentStyleIgnoringFirstLine,
        fontMetricsProvider
    )
}

fun applyDeclarations(
    device: Device,
    element: Element,
    pseudoElement: PseudoElement?,

    ruleNode: RuleNode,
    declarations: Sequence<DeclarationAndCascadeLevel>,

    previousStyle: ComputedValues?,
    parentStyle: ComputedValues?,
    parentStyleIgnoringFirstLine: ComputedValues?,

    fontMetricsProvider: FontMetricsProvider,
): ComputedValues {
    val inheritedStyle = parentStyle ?: device.defaultComputedValues()

    val customProperties = buildCustomProperties(previousStyle?.customProperties, inheritedStyle.customProperties) {
        process(declarations)
    }

    val properties = buildProperties(ruleNode, previousStyle, customProperties) {
        process(declarations)
    }

    val context = Context(
        element.isRoot(),
        StyleBuilder.from(
            device,

            customProperties,
            properties,

            ruleNode,

            parentStyle,
            parentStyleIgnoringFirstLine
        ),
        fontMetricsProvider,
    )

    for ((longhandId, declaration) in properties) {
        if (!longhandId.isEarlyProperty) {
            continue
        }

        longhandId.cascadeProperty(declaration, context)
    }

    for ((longhandId, declaration) in properties) {
        if (longhandId.isEarlyProperty) {
            continue
        }

        longhandId.cascadeProperty(declaration, context)
    }

    return context.builder.build()
}

private inline fun buildCustomProperties(
    previous: CustomPropertiesList?,
    inherited: CustomPropertiesList?,
    block: CustomPropertiesListBuilder.() -> Unit,
): CustomPropertiesList? {
    val builder = CustomPropertiesListBuilder(previous, inherited)
    builder.block()
    return builder.build()
}

class CustomPropertiesList(
    properties: Map<Name, VariableValue>,
) : Iterable<Pair<Name, VariableValue>> {

    private val properties: List<Pair<Name, VariableValue>> = properties.toList().sortedBy { (name, _) -> name }

    fun get(name: Name): VariableValue? {
        val index = properties.binarySearch { (candidate, _) -> candidate.compareTo(name) }
        return properties.getOrNull(index)?.second
    }

    override fun iterator(): Iterator<Pair<Name, VariableValue>> = properties.iterator()

    fun toMap(): Map<Name, VariableValue> = properties.toMap()
    fun toMutableMap(): MutableMap<Name, VariableValue> = properties.toMap(mutableMapOf())

    fun isCompatible(properties: Map<Name, VariableValue>): Boolean {
        if (properties.size != this.properties.size) return false
        for ((name, value) in this.properties) {
            if (properties[name] !== value) return false
        }
        return true
    }
}

class CustomPropertiesListBuilder(
    private val previous: CustomPropertiesList?,
    private val inherited: CustomPropertiesList?,
) {
    private var customProperties: MutableMap<Name, VariableValue>? = null

    fun process(
        declarations: Sequence<DeclarationAndCascadeLevel>,
    ) {
        val seen = mutableSetOf<Name>()
        val reverted = PerOrigin { mutableSetOf<Name>() }

        for ((declaration, level) in declarations) {
            if (declaration !is PropertyDeclaration.Custom) continue

            val (name, value) = declaration.declaration

            val origin = level.origin
            if (reverted.find(origin)?.contains(name) == true) {
                continue
            }

            if (!seen.add(name)) {
                continue
            }

            if (!valueMayAffectStyle(name, value)) {
                continue
            }

            var customProperties = this.customProperties
            if (customProperties == null) {
                customProperties = inherited?.toMutableMap() ?: mutableMapOf()
                this.customProperties = customProperties
            }

            when (value) {
                is CustomDeclarationValue.Value -> {
                    customProperties[name] = value.value
                }

                is CustomDeclarationValue.CssWideKeyword -> {
                    when (value.keyword) {
                        CssWideKeyword.Revert -> {
                            seen.remove(name)
                            for (relevantOrigin in origin.selfAndPrevious()) {
                                reverted.get(relevantOrigin).add(name)
                            }
                        }

                        CssWideKeyword.Initial -> {
                            customProperties.remove(name)
                        }

                        CssWideKeyword.Unset,
                        CssWideKeyword.Inherit,
                        -> error("should have already been handled in valueMayAffectStyle()")
                    }
                }
            }
        }
    }

    private fun valueMayAffectStyle(name: Name, value: CustomDeclarationValue): Boolean {
        if (value is CustomDeclarationValue.CssWideKeyword) {
            when (value.keyword) {
                CssWideKeyword.Unset,
                CssWideKeyword.Inherit,
                -> return false

                else -> {}
            }
        }

        val existingValue = customProperties?.get(name) ?: inherited?.get(name)

        if (existingValue == null && value is CustomDeclarationValue.CssWideKeyword && value.keyword == CssWideKeyword.Initial) {
            return false
        }

        if (existingValue != null && value is CustomDeclarationValue.Value && existingValue === value.value) {
            return false
        }

        return true
    }

    fun build(): CustomPropertiesList? {
        val customProperties = customProperties ?: return inherited

        val previous = previous
        if (previous?.isCompatible(customProperties) == true) return previous

        return CustomPropertiesList(customProperties)
    }
}

private inline fun buildProperties(
    ruleNode: RuleNode,
    previousStyle: ComputedValues?,
    customProperties: CustomPropertiesList?,
    block: PropertiesListBuilder.() -> Unit,
): PropertiesList {
    val previousProperties = previousStyle?.properties
    return if (previousProperties == null || customProperties !== previousStyle.customProperties || ruleNode !== previousStyle.ruleNode) {
        val builder = PropertiesListBuilder(customProperties)
        builder.block()
        builder.build()
    } else {
        previousProperties
    }
}

class PropertiesList(
    private val properties: List<Pair<LonghandId, PropertyDeclaration>>,
) : Iterable<Pair<LonghandId, PropertyDeclaration>> {

    override fun iterator(): Iterator<Pair<LonghandId, PropertyDeclaration>> = properties.iterator()

    fun toMap(): Map<LonghandId, PropertyDeclaration> = properties.toMap()
}

class PropertiesListBuilder(
    private val customProperties: CustomPropertiesList?,
) {
    private val substitutionCache = SubstitutionCache()

    private val seen = LonghandIdSet()
    private val reverted = PerOrigin<LonghandIdSet>()

    private val properties = mutableListOf<Pair<LonghandId, PropertyDeclaration>>()

    fun process(
        declarations: Sequence<DeclarationAndCascadeLevel>,
    ) {
        for ((declaration, level) in declarations) {
            val longhandId = when (val id = declaration.id) {
                is PropertyDeclarationId.Longhand -> id.id
                is PropertyDeclarationId.Custom -> continue
            }

            if (seen.contains(longhandId)) {
                continue
            }

            val origin = level.origin

            if (reverted.find(origin)?.contains(longhandId) == true) {
                continue
            }

            val substitutedDeclaration = substituteVariables(declaration)

            val cssWideKeyword = substitutedDeclaration.getCssWideKeyword()
            if (cssWideKeyword == CssWideKeyword.Revert) {
                for (relevantOrigin in origin.selfAndPrevious()) {
                    reverted.get(relevantOrigin).add(longhandId)
                }
                continue
            }

            seen.add(longhandId)

            val inherited = longhandId.isInherited
            val unset = when (cssWideKeyword) {
                CssWideKeyword.Unset -> true
                CssWideKeyword.Initial -> !inherited
                CssWideKeyword.Inherit -> inherited
                else -> false
            }

            if (unset) {
                continue
            }

            properties.add(longhandId to substitutedDeclaration)
        }
    }

    private fun substituteVariables(
        declaration: PropertyDeclaration,
    ): PropertyDeclaration {
        if (declaration !is PropertyDeclaration.WithVariables) return declaration

        return declaration.declaration.substituteVariables(customProperties, substitutionCache)
    }

    private fun PropertyDeclaration.getCssWideKeyword(): CssWideKeyword? {
        if (this !is PropertyDeclaration.CssWideKeyword) return null
        return declaration.keyword
    }

    fun build(): PropertiesList {
        return PropertiesList(properties)
    }
}
