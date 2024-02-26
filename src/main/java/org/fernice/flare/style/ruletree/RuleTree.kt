/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.fernice.flare.style.ruletree

import org.fernice.flare.style.ApplicableDeclarationBlock
import org.fernice.flare.style.Importance
import org.fernice.flare.style.Origin
import org.fernice.flare.style.source.StyleSource
import org.fernice.std.Recycler
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

enum class CascadeLevel {

    UserAgentNormal,

    UserNormal,

    AuthorNormal,

    AuthorImportant,

    UserImportant,

    UserAgentImportant;

    val origin: Origin
        get() = when (this) {
            UserAgentNormal -> Origin.UserAgent
            UserNormal -> Origin.User
            AuthorNormal -> Origin.Author
            AuthorImportant -> Origin.Author
            UserImportant -> Origin.User
            UserAgentImportant -> Origin.UserAgent
        }

    val importance: Importance
        get() = if (isImportant) Importance.Important else Importance.Normal

    val isImportant: Boolean
        get() {
            return when (this) {
                AuthorImportant,
                UserImportant,
                UserAgentImportant,
                -> true

                else -> false
            }
        }

    companion object {
        val values: List<CascadeLevel> = Collections.unmodifiableList(arrayListOf(*CascadeLevel.values()))

        fun of(origin: Origin, importance: Importance): CascadeLevel {
            return when (origin) {
                Origin.UserAgent -> if (importance == Importance.Important) UserAgentImportant else UserAgentNormal
                Origin.User -> if (importance == Importance.Important) UserImportant else UserNormal
                Origin.Author -> if (importance == Importance.Important) AuthorImportant else AuthorNormal
            }
        }
    }
}

private class ImportantDeclarations {
    val userAgent: MutableList<ApplicableDeclarationBlock> = arrayListOf()
    val user: MutableList<ApplicableDeclarationBlock> = arrayListOf()
    val author: MutableList<ApplicableDeclarationBlock> = arrayListOf()
}

private val ImportantDeclarationsRecycler = Recycler(
    factory = { ImportantDeclarations() },
    reset = {
        it.userAgent.clear()
        it.user.clear()
        it.author.clear()
    },
)

private const val DORMANT_CYCLES = 1000

class RuleTree {
    val root: RuleNode = RuleNode.root()

    // TODO add gc mechanism to prevent build up of StyleSource that aren't StyleRules

    fun computedRuleNode(sources: Iterator<StyleSource>): RuleNode {
        var current = root
        var lastOrigin = current.level.origin
        var seenImportant = false

        val importantDeclarations = ImportantDeclarationsRecycler.acquire()

        for (source in sources) {
            assert(source.origin >= lastOrigin) { "illegal order: ${source.origin} < $lastOrigin" }

            val hasImportant = source.declarations.hasImportant()
            if (hasImportant) {
                seenImportant = true

                when (source.origin) {
                    Origin.Author -> importantDeclarations.author.add(source.getApplicableDeclarations(Importance.Important))
                    Origin.User -> importantDeclarations.user.add(source.getApplicableDeclarations(Importance.Important))
                    Origin.UserAgent -> importantDeclarations.userAgent.add(source.getApplicableDeclarations(Importance.Important))
                }
            }

            current = current.ensureChild(root, source.getApplicableDeclarations(Importance.Normal), CascadeLevel.of(source.origin, Importance.Normal))
            lastOrigin = source.origin
        }

        if (!seenImportant) {
            return current
        }

        for (source in importantDeclarations.author) {
            current = current.ensureChild(root, source, CascadeLevel.AuthorImportant)
        }

        for (source in importantDeclarations.user) {
            current = current.ensureChild(root, source, CascadeLevel.UserImportant)
        }

        for (source in importantDeclarations.userAgent) {
            current = current.ensureChild(root, source, CascadeLevel.UserAgentImportant)
        }

        ImportantDeclarationsRecycler.release(importantDeclarations)

        return current
    }

    private val cycles = AtomicInteger(DORMANT_CYCLES)

    fun gc() {
        val cycle = cycles.updateAndGet { if (it == 0) DORMANT_CYCLES else (it - 1) }
        if (cycle == DORMANT_CYCLES || root.isGcRequested) {
            performGc()
        }
    }

    private fun performGc() {
        root.performGc()
    }

    fun clear() {
        root.clear()
    }
}

private val RULE_NODE_NUMBER_RANGE = AtomicInteger()

class RuleNode private constructor(
    val root: RuleNode?,
    val parent: RuleNode?,
    val declarations: WeakReference<ApplicableDeclarationBlock>?,
    val level: CascadeLevel,
) {

    private val firstChildReference = AtomicReference<RuleNode?>()
    private val nextSiblingReference = AtomicReference<RuleNode?>()
    private val previousSiblingReference = AtomicReference<RuleNode?>()

    internal val number = RULE_NODE_NUMBER_RANGE.getAndIncrement()

    fun ensureChild(
        root: RuleNode?,
        declarations: ApplicableDeclarationBlock,
        level: CascadeLevel,
    ): RuleNode {
        // Find the last child to append the new node after it
        var lastChild: RuleNode? = firstChildReference.get()
        while (lastChild != null) {
            val declarationsReference = lastChild.declarations
            if (declarationsReference != null && declarationsReference.get() == null) {
                gc()
            }

            // Check whether we've already inserted the rule to allow
            // for reusing of existing nodes and minimizing the tree
            if (lastChild.level == level && declarationsReference?.get() == declarations) {
                return lastChild
            }

            lastChild = lastChild.nextSiblingReference.get() ?: break
        }

        // We haven't found a preexisting node, create new one
        val node = RuleNode(root, this, WeakReference(declarations), level)

        while (true) {
            // Retrieve the references of the sibling or first child,
            // if there are no children yet
            val siblingReference = when (lastChild) {
                null -> firstChildReference
                else -> lastChild.nextSiblingReference
            }

            // Try to append the node to the last sibling. Synchronize
            // only for the writing, reading might still have been dirty.
            synchronized(this) {
                if (siblingReference.compareAndSet(null, node)) {
                    lastChild?.previousSiblingReference?.set(node)

                    return node
                }
            }

            // Some other thread appended some node to the last sibling
            val next = siblingReference.get()

            // Check the appended node on whether we can reuse it
            if (next?.level == level && next.declarations?.get() == declarations) {
                return next
            }

            // try again with the new last child
            lastChild = next
        }
    }

    private val gcRequested = AtomicBoolean(false)
    val isGcRequested: Boolean
        get() = gcRequested.get()

    fun gc() {
        if (!gcRequested.getAndSet(true)) {
            parent?.gc()
        }
    }

    internal fun performGc() {
        var child = firstChildReference.get()

        while (child != null) {
            // contents of this node have unrecoverably vacated, remove the node
            val declarationsReference = child.declarations
            if (declarationsReference != null && declarationsReference.get() == null) {
                do {
                    // consistency is guarded by the forward reference, find who's referring
                    // to the node
                    val previousSibling = child.previousSiblingReference.get()
                    val nextSiblingReference = when (previousSibling) {
                        null -> firstChildReference
                        else -> previousSibling.nextSiblingReference
                    }

                    val target = nextSiblingReference.get()

                    // someone else might already have removed this child
                    if (target !== child) break

                    val successful = synchronized(this) {
                        val nextSibling = target.nextSiblingReference.get()
                        if (nextSiblingReference.compareAndSet(target, nextSibling)) {
                            nextSibling?.previousSiblingReference?.set(previousSibling)
                            true
                        } else {
                            false
                        }
                    }
                } while (!successful)
            }

            child.performGc()

            child = child.nextSiblingReference.get()
        }
    }

    fun clear() {
        firstChildReference.set(null)
        previousSiblingReference.set(null)
        nextSiblingReference.set(null)
    }

    val firstChild: RuleNode? get() = firstChildReference.get()
    val previousSibling: RuleNode? get() = previousSiblingReference.get()
    val nextSibling: RuleNode? get() = nextSiblingReference.get()

    fun selfAndAncestors(): SelfAndAncestors {
        return SelfAndAncestors(this)
    }

    fun childrenIterator(): Iterator<RuleNode> {
        return RuleNodeChildrenIterator(firstChild)
    }

    override fun toString(): String {
        return "RuleNode[$number](source: ${declarations?.get()?.source} level: $level)"
    }

    companion object {
        fun root(): RuleNode {
            return RuleNode(
                root = null,
                parent = null,
                declarations = null,
                level = CascadeLevel.UserAgentNormal,
            )
        }
    }
}

class SelfAndAncestors(private val current: RuleNode) : Sequence<RuleNode> {

    override fun iterator(): Iterator<RuleNode> {
        return SelfAndAncestorsIterator(current)
    }

    class SelfAndAncestorsIterator(private var current: RuleNode?) : Iterator<RuleNode> {

        override fun hasNext(): Boolean = current != null

        override fun next(): RuleNode {
            val next = current ?: throw NoSuchElementException()
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
