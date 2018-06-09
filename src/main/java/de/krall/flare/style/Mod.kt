package de.krall.flare.style

import de.krall.flare.selector.PSEUDO_COUNT
import de.krall.flare.selector.PseudoElement
import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.krall.flare.std.iter.Iter
import de.krall.flare.std.iter.iter
import de.krall.flare.std.unwrap
import de.krall.flare.style.stylesheet.Origin

class PerPseudoElementMap<E> {

    private val entries: Array<Option<E>> = Array(PSEUDO_COUNT, { None<E>() })

    fun get(pseudoElement: PseudoElement): Option<E> {
        return entries[pseudoElement.ordinal()]
    }

    fun set(pseudoElement: PseudoElement, value: E) {
        entries[pseudoElement.ordinal()] = Some(value)
    }

    fun computeIfAbsent(pseudoElement: PseudoElement, insert: () -> E): E {
        var entry = entries[pseudoElement.ordinal()]

        if (entry.isNone()) {
            entry = Some(insert())
            entries[pseudoElement.ordinal()] = entry
        }

        return entry.unwrap()
    }

    fun clear() {
        for (i in 0..entries.size) {
            entries[i] = None()
        }
    }

    fun iter(): Iter<Option<E>> {
        return entries.iter()
    }
}

class PerOrigin<E>(val userAgent: E,
                   val user: E,
                   val author: E) {

    fun get(origin: Origin): E {
        return when (origin) {
            Origin.USER_AGENT -> userAgent
            Origin.USER -> user
            Origin.AUTHOR -> author
        }
    }

    fun iter(): PerOriginIter<E> {
        return PerOriginIter(
                this,
                0,
                false
        )
    }

    fun iterReversed(): PerOriginIter<E> {
        return PerOriginIter(
                this,
                0,
                false
        )
    }
}

class PerOriginIter<E>(private val perOrigin: PerOrigin<E>,
                       private var index: Int,
                       private val reversed: Boolean) : Iter<E> {

    override fun next(): Option<E> {
        if (reversed) {
            if (index == 0) {
                return None()
            }
        } else {
            if (index == 3) {
                return None()
            }
        }

        val value = perOrigin.get(Origin.values()[index])

        if (reversed) {
            index--
        } else {
            index++
        }

        return Some(value)
    }

    override fun clone(): PerOriginIter<E> {
        return PerOriginIter(
                perOrigin,
                index,
                reversed
        )
    }
}