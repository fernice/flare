/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.fernice.flare.style.ruletree

fun RuleTree.printTree() = println(toFormattedString())
fun RuleNode.printTree() = println((root ?: this).toFormattedString())

fun RuleTree.toFormattedString(): String = root().toFormattedString()

fun RuleNode.toFormattedString(): String = buildString { write { append(it) } }

private fun RuleNode.write(sink: (String) -> Unit) {
    sink(this.toString())
    sink("\n")
    for (child in childrenIterator()) {
        child.write { text -> sink("  $text") }
    }
}