package de.krall.flare.style.ruletree

import de.krall.flare.std.Either
import de.krall.flare.std.First
import de.krall.flare.std.Second
import de.krall.flare.style.properties.PropertyDeclarationBlock
import de.krall.flare.style.stylesheet.StyleRule

enum class CascadeLevel {

    USER_AGENT_NORMAL,

    USER_NORMAL,

    AUTHOR_NORMAL,

    STYLE_ATTRIBUTE_NORMAL,

    AUTHOR_IMPORTANT,

    STYLE_ATTRIBUTE_IMPORTANT,

    USER_IMPORTANT,

    USER_AGENT_IMPORTANT
}

class StyleSource(private val either: Either<StyleRule, PropertyDeclarationBlock>) {

    companion object {
        fun fromDeclarations(declarations: PropertyDeclarationBlock): StyleSource {
            return StyleSource(
                    Second(declarations)
            )
        }

        fun fromRule(rule: StyleRule): StyleSource {
            return StyleSource(
                    First(rule)
            )
        }
    }

    fun declarations(): PropertyDeclarationBlock {
        return when (either) {
            is First -> either.value.declarations
            is Second -> either.value
        }
    }
}