package de.krall.flare.selector

class SelectorBuilder {

    private val components = mutableListOf<Component>()
    private var danglingCombinator = false

    fun pushSimpleSelector(component: Component) {
        components.add(component)
        danglingCombinator = false
    }

    fun pushCombinator(combinator: Combinator) {
        components.add(Component.Combinator(combinator))
        danglingCombinator = false
    }

    fun isEmpty(): Boolean {
        return components.isEmpty()
    }

    fun hasDanglingCombinator(): Boolean {
        return danglingCombinator
    }

    fun build(hasPseudoElement: Boolean): Selector {
        return Selector(components)
    }
}