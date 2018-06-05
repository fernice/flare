package de.krall.flare.style.ruletree

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

class StyleSource(val styleRule: StyleRule)