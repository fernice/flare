package de.krall.flare.css.selector0

import de.flare.css.selector.NamespaceConstraint
import de.flare.css.value.specified.LengthOrPercentage
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.flare.css.value.computed.LengthOrPercentage as ComputedLengthOrPercentage

class Selector {

    fun test(): String? {
        val result = parseQualifiedName()

        val localName: String
        val namespace: Option<NamespaceConstraint>

        when (result) {
            is QualifiedName.Some -> {
                localName = when (result.localName) {
                    is Option.None -> {
                        throw IllegalStateException("unreachable")
                    }
                    is Option.Some -> {
                        result.localName.value
                    }
                }

                val prefix = result.prefix
                namespace = when (prefix) {
                    is QualifiedNamePrefix.ImplicitNoNamespace, is QualifiedNamePrefix.ExplicitNoNamespace -> {
                        Option.None()
                    }
                    is QualifiedNamePrefix.ExplicitNamespace -> {
                        Some(NamespaceConstraint.Specific(prefix.prefix, prefix.url))
                    }
                    is QualifiedNamePrefix.ExplicitAnyNamespace -> Option.Some(NamespaceConstraint.Any())
                    is QualifiedNamePrefix.ImplicitAnyNamespace, is QualifiedNamePrefix.ImplicitDefaultNamespace -> {
                        throw IllegalStateException("unreachable")
                    }
                }
            }
        }

        val a: LengthOrPercentage
        val b: ComputedLengthOrPercentage

        return null
    }

    private fun parseQualifiedName(): QualifiedName {
        return QualifiedName.None()
    }
}