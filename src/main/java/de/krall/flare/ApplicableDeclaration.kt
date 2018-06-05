package de.krall.flare

import de.krall.flare.std.min
import de.krall.flare.style.ruletree.CascadeLevel
import de.krall.flare.style.ruletree.StyleSource

class ApplicableDeclaration(private val styleSource: StyleSource,
                            private val bits: ApplicableDeclarationBits,
                            private val specificity: Int) {

    companion object {
        fun new(source: StyleSource,
                sourceOrder: Int,
                cascadeLevel: CascadeLevel,
                specificity: Int): ApplicableDeclaration {
            return ApplicableDeclaration(
                    source,
                    ApplicableDeclarationBits.new(sourceOrder, cascadeLevel),
                    specificity
            )
        }
    }

    fun sourceOrder(): Int {
        return bits.sourceOrder()
    }

    fun cascadeLevel(): CascadeLevel {
        return bits.cascadeLevel()
    }
}

private const val SOURCE_ORDER_SHIFT = 0
private const val SOURCE_ORDER_BITS = 24
private const val SOURCE_ORDER_MAX = (1 shl SOURCE_ORDER_BITS) - 1
private const val SOURCE_ORDER_MASK = SOURCE_ORDER_MAX shl SOURCE_ORDER_SHIFT

private const val CASCADE_LEVEL_SHIFT = SOURCE_ORDER_BITS
private const val CASCADE_LEVEL_BITS = 4
private const val CASCADE_LEVEL_MAX = (1 shl CASCADE_LEVEL_BITS) - 1
private const val CASCADE_LEVEL_MASK = CASCADE_LEVEL_MAX shl CASCADE_LEVEL_SHIFT

class ApplicableDeclarationBits private constructor(private val bits: Int) {

    companion object {
        fun new(sourceOrder: Int, cascadeLevel: CascadeLevel): ApplicableDeclarationBits {
            var bits = sourceOrder.min(SOURCE_ORDER_MAX)
            bits = bits or (cascadeLevel.ordinal.min(CASCADE_LEVEL_MAX) shl CASCADE_LEVEL_SHIFT)
            return ApplicableDeclarationBits(bits)
        }
    }

    fun sourceOrder(): Int {
        return (bits and SOURCE_ORDER_MASK) shr SOURCE_ORDER_SHIFT
    }

    fun cascadeLevel(): CascadeLevel {
        val ordinal = (bits and CASCADE_LEVEL_MASK) shr CASCADE_LEVEL_SHIFT

        return CascadeLevel.values()[ordinal]
    }
}