/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.ruletree

import fernice.std.None
import fernice.std.Option
import fernice.std.Some
import fernice.std.ifLet
import fernice.std.into
import fernice.std.map
import org.fernice.flare.ApplicableDeclarationBlock
import org.fernice.flare.RuleTreeValues
import org.fernice.flare.debugAssert
import org.fernice.flare.std.Either
import org.fernice.flare.std.First
import org.fernice.flare.std.Second
import org.fernice.flare.std.iter.Iter
import org.fernice.flare.std.iter.iter
import org.fernice.flare.style.properties.Importance
import org.fernice.flare.style.properties.PropertyDeclarationBlock
import org.fernice.flare.style.stylesheet.StyleRule
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
            CascadeLevel.AUTHOR_IMPORTANT,
            CascadeLevel.STYLE_ATTRIBUTE_IMPORTANT,
            CascadeLevel.USER_IMPORTANT,
            CascadeLevel.USER_AGENT_IMPORTANT -> true
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
        fun new(): RuleTree {
            return RuleTree(
                RuleNode.root()
            )
        }
    }

    fun computedRuleNode(applicableDeclarations: List<ApplicableDeclarationBlock>): RuleNode {
        return insertsRuleNodeWithImportant(
            applicableDeclarations.iter().map(ApplicableDeclarationBlock::forRuleTree)
        )
    }

    fun insertsRuleNodeWithImportant(iter: Iter<RuleTreeValues>): RuleNode {
        var current = root
        var lastLevel = current.level
        var seenImportant = false

        val authorImportant = mutableListOf<StyleSource>()
        val userImportant = mutableListOf<StyleSource>()
        val userAgentImportant = mutableListOf<StyleSource>()
        val styleAttributeImportant = mutableListOf<StyleSource>()


        for ((source, level) in iter) {
            debugAssert(level >= lastLevel, "illegal order")
            debugAssert(!level.isImportant(), "important cannot be inserted")

            val hasImportant = source.declarations().hasImportant()

            if (hasImportant) {
                seenImportant = true

                when (level) {
                    CascadeLevel.AUTHOR_NORMAL -> authorImportant.add(source)
                    CascadeLevel.USER_NORMAL -> userImportant.add(source)
                    CascadeLevel.USER_AGENT_NORMAL -> userAgentImportant.add(source)
                    CascadeLevel.STYLE_ATTRIBUTE_IMPORTANT -> styleAttributeImportant.add(source)
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

class RuleNode(
    private val root: RuleNode?,
    val parent: RuleNode?,
    val source: StyleSource?,
    val level: CascadeLevel,
    private val firstChild: AtomicReference<RuleNode?>,
    private val nextSibling: AtomicReference<RuleNode?>,
    private val previous: AtomicReference<RuleNode?>
) {

    companion object {
        fun new(
            root: RuleNode,
            parent: RuleNode,
            source: StyleSource,
            cascadeLevel: CascadeLevel
        ): RuleNode {
            return RuleNode(
                root = root,
                parent = parent,
                source = source,
                level = cascadeLevel,
                firstChild = AtomicReference(),
                nextSibling = AtomicReference(),
                previous = AtomicReference()
            )
        }

        fun root(): RuleNode {
            return RuleNode(
                root = null,
                parent = null,
                source = null,
                level = CascadeLevel.USER_AGENT_NORMAL,
                firstChild = AtomicReference(),
                nextSibling = AtomicReference(),
                previous = AtomicReference()
            )
        }
    }

    fun ensureChild(
        root: RuleNode,
        source: StyleSource,
        level: CascadeLevel
    ): RuleNode {
        var last: Option<RuleNode> = None

        for (child in iterChildren()) {
            if (child.level == level && child.source == source) {
                return child
            }
            last = Some(child)
        }

        val node = RuleNode.new(root, this, source, level)

        while (true) {

            val nextSibling = when (last) {
                is Some -> last.value.nextSibling
                is None -> firstChild
            }

            val set = nextSibling.compareAndSet(null, node)

            if (set) {
                last.ifLet {
                    node.previous.set(node)
                }

                return node
            }

            val next = nextSibling.get()!!

            if (next.source === source) {
                return next
            }

            last = Some(next)
        }
    }

    fun styleSource(): Option<StyleSource> {
        return source.into()
    }

    fun cascadeLevel(): CascadeLevel {
        return level
    }

    fun importance(): Importance {
        return if (level.isImportant()) {
            Importance.IMPORTANT
        } else {
            Importance.NORMAL
        }
    }

    fun parent(): Option<RuleNode> {
        return parent.into()
    }

    fun nextSibling(): Option<RuleNode> {
        return nextSibling.get().into()
    }

    fun firstChild(): Option<RuleNode> {
        return firstChild.get().into()
    }

    fun selfAndAncestors(): SelfAndAncestors {
        return SelfAndAncestors(this)
    }

    fun iterChildren(): Iter<RuleNode> {
        val firstChild = firstChild.get()
        return RuleNodeChildrenIter(
            if (firstChild != null) {
                Some(firstChild)
            } else {
                None
            }
        )
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

class RuleNodeChildrenIter(private var current: Option<RuleNode>) : Iter<RuleNode> {

    override fun next(): Option<RuleNode> {
        return current.map { head ->
            this.current = head.nextSibling()

            head
        }
    }

    override fun clone(): RuleNodeChildrenIter {
        return RuleNodeChildrenIter(current)
    }
}
