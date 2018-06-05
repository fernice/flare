package de.krall.flare.style

import de.krall.flare.ApplicableDeclaration
import de.krall.flare.selector.AncestorHashes
import de.krall.flare.selector.Selector
import de.krall.flare.style.ruletree.CascadeLevel
import de.krall.flare.style.ruletree.StyleSource
import de.krall.flare.style.stylesheet.StyleRule

class Rule(val selector: Selector,
           val hashes: AncestorHashes,
           val sourceOrder: Int,
           val styleRule: StyleRule) {

    fun specificity(): Int {
        return selector.specificity()
    }

    fun toApplicableDeclaration(cascadeLevel: CascadeLevel): ApplicableDeclaration {
        return ApplicableDeclaration.new(
                StyleSource(styleRule),
                sourceOrder,
                cascadeLevel,
                specificity()
        )
    }
}