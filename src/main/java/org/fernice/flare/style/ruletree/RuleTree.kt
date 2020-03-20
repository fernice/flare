/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.ruletree

import org.fernice.flare.ApplicableDeclarationBlock
import org.fernice.flare.RuleTreeValues
import org.fernice.flare.debugAssert
import org.fernice.flare.std.Either
import org.fernice.flare.std.First
import org.fernice.flare.std.Second
import org.fernice.flare.style.properties.Importance
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.stylesheet.StyleRule
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

enum class CascadeLevel {

    USER_AGENT_NORMAL,

    USER_NORMAL,

    AUTHOR_NORMAL,

    STYLE_ATTRIBUTE_NORMAL,

    AUTHOR_IMPORTANT,

    STYLE_ATTRIBUTE_IMPORTANT,

    USER_IMPORTANT,

    USER_AGENT_IMPORTANT;

    fun isImportant(): Boolean {
        return when (this) {
            AUTHOR_IMPORTANT,
            STYLE_ATTRIBUTE_IMPORTANT,
            USER_IMPORTANT,
            USER_AGENT_IMPORTANT -> true
            else -> false
        }
    }
}

data class StyleSource(val source: Either<StyleRule, PropertyDeclarationBlock>) {

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
        return when (source) {
            is First -> source.value.declarations
            is Second -> source.value
        }
    }
}

class RuleTree(private val root: RuleNode) {

    companion object {
        fun new(): RuleTree = RuleTree(RuleNode.root())
    }

    fun computedRuleNode(applicableDeclarations: List<ApplicableDeclarationBlock>): RuleNode {
        return insertsRuleNodeWithImportant(
            applicableDeclarations.asSequence().map(ApplicableDeclarationBlock::forRuleTree).iterator()
        )
    }

    fun insertsRuleNodeWithImportant(iterator: Iterator<RuleTreeValues>): RuleNode {
        var current = root
        var lastLevel = current.level
        var seenImportant = false

        val authorImportant = mutableListOf<StyleSource>()
        val userImportant = mutableListOf<StyleSource>()
        val userAgentImportant = mutableListOf<StyleSource>()
        val styleAttributeImportant = mutableListOf<StyleSource>()

        for ((source, level) in iterator) {
            debugAssert(level >= lastLevel, "illegal order")
            debugAssert(!level.isImportant(), "important cannot be inserted")

            val hasImportant = source.declarations().hasImportant()
            if (hasImportant) {
                seenImportant = true

                when (level) {
                    CascadeLevel.AUTHOR_NORMAL -> authorImportant.add(source)
                    CascadeLevel.USER_NORMAL -> userImportant.add(source)
                    CascadeLevel.USER_AGENT_NORMAL -> userAgentImportant.add(source)
                    CascadeLevel.STYLE_ATTRIBUTE_NORMAL -> styleAttributeImportant.add(source)
                    else -> {
                    }
                }
            }

            current = current.ensureChild(root, source, level)
            lastLevel = level
        }

        if (!seenImportant) {
            return current
        }

        for (source in authorImportant) {
            current = current.ensureChild(root, source, CascadeLevel.AUTHOR_IMPORTANT)
        }

        for (source in styleAttributeImportant) {
            current = current.ensureChild(root, source, CascadeLevel.STYLE_ATTRIBUTE_IMPORTANT)
        }

        for (source in userImportant) {
            current = current.ensureChild(root, source, CascadeLevel.USER_IMPORTANT)
        }

        for (source in userAgentImportant) {
            current = current.ensureChild(root, source, CascadeLevel.USER_AGENT_IMPORTANT)
        }

        return current
    }

    fun root(): RuleNode {
        return root
    }
}

private val RULE_NODE_NUMBER_RANGE = AtomicInteger()

class RuleNode private constructor(
    val root: RuleNode?,
    val parent: RuleNode?,
    val source: StyleSource?,
    val level: CascadeLevel
) {

    private val firstChildReference = AtomicReference<RuleNode?>()
    private val nextSiblingReference = AtomicReference<RuleNode?>()
    private val previousSiblingReference = AtomicReference<RuleNode?>()

    internal val number = RULE_NODE_NUMBER_RANGE.getAndIncrement()

    companion object {
        fun root(): RuleNode {
            return RuleNode(
                root = null,
                parent = null,
                source = null,
                level = CascadeLevel.USER_AGENT_NORMAL
            )
        }
    }

    fun ensureChild(
        root: RuleNode,
        source: StyleSource,
        level: CascadeLevel
    ): RuleNode {
        // Find the last child to append the new node after it
        var lastChild: RuleNode? = firstChild
        while (lastChild != null) {
            // Check whether we've already inserted the rule to allow
            // for reusing of existing nodes and minimizing the tree
            if (lastChild.level == level && lastChild.source == source) {
                return lastChild
            }
            lastChild = lastChild.nextSibling ?: break
        }

        // We haven't found the a preexisting node, create new one
        val node = RuleNode(root, this, source, level)

        while (true) {
            // Retrieve the references of the sibling or first child,
            // if there are no children yet
            val siblingReference = when (lastChild) {
                null -> firstChildReference
                else -> lastChild.nextSiblingReference
            }

            // Try to append the node to the last sibling
            if (siblingReference.compareAndSet(null, node)) {
                lastChild?.previousSiblingReference?.set(node)

                return node
            }

            // Some other thread appended some node to the last sibling
            val next = siblingReference.get()

            // Check the appended node on whether we can reuse it
            if (next?.level == level && next.source == source) {
                return next
            }

            // try again with the new last child
            lastChild = next
        }
    }

    val importance: Importance get() = if (level.isImportant()) Importance.IMPORTANT else Importance.NORMAL

    val nextSibling: RuleNode? get() = nextSiblingReference.get()
    val firstChild: RuleNode? get() = firstChildReference.get()

    fun selfAndAncestors(): SelfAndAncestors {
        return SelfAndAncestors(this)
    }

    fun childrenIterator(): Iterator<RuleNode> {
        return RuleNodeChildrenIterator(firstChild)
    }

    override fun toString(): String {
        return "RuleNode[$number source: $source]"
    }
}

class SelfAndAncestors(private val current: RuleNode) : Sequence<RuleNode> {

    override fun iterator(): Iterator<RuleNode> {
        return SelfAndAncestorsIterator(current)
    }

    inner class SelfAndAncestorsIterator(private var current: RuleNode?) : Iterator<RuleNode> {

        override fun hasNext(): Boolean = current != null

        override fun next(): RuleNode {
            val next = current ?: error("no more rule nodes left")
            current = next.parent
            return next
        }
    }
}

class RuleNodeChildrenIterator(private var current: RuleNode?) : Iterator<RuleNode> {

    override fun hasNext(): Boolean = current != null

    override fun next(): RuleNode {
        val actual = current ?: throw NoSuchElementException()
        current = actual.nextSibling
        return actual
    }
}
