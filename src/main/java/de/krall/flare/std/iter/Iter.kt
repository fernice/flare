package de.krall.flare.std.iter

import de.krall.flare.std.None
import de.krall.flare.std.Option
import de.krall.flare.std.Some
import de.krall.flare.std.unwrap

interface Iter<E> : Iterable<E> {

    fun next(): Option<E>

    fun <B> map(function: (E) -> B): Map<E, B> {
        return Map(this, function)
    }

    fun filter(predicate: (E) -> Boolean): Filter<E> {
        return Filter(this, predicate)
    }

    fun <B> filterMap(mapper: (E) -> Option<B>): FilterMap<E, B> {
        return FilterMap(this, mapper)
    }

    override fun iterator(): Iterator<E> {
        return IterIterator(this)
    }
}

class Map<E, B>(private val iter: Iter<E>, private val f: (E) -> B) : Iter<B> {
    override fun next(): Option<B> {
        return iter.next().map(f)
    }
}

class Filter<E>(private val iter: Iter<E>, private val p: (E) -> Boolean) : Iter<E> {
    override fun next(): Option<E> {
        for (item in iter) {
            if (p(item)) {
                return Some(item)
            }
        }
        return None()
    }
}

class FilterMap<E, B>(private val iter: Iter<E>, private val fp: (E) -> Option<B>) : Iter<B> {
    override fun next(): Option<B> {
        for (item in iter) {
            val result = fp(item)
            if (result is Some) {
                return Some(result.value)
            }
        }
        return None()
    }
}

class IterIterator<E>(private val iter: Iter<E>) : Iterator<E> {

    private var next: Option<E> = iter.next()
    private var hasNext = next.isSome()

    override fun hasNext(): Boolean {
        return hasNext
    }

    override fun next(): E {
        val now = next

        next = iter.next()
        hasNext = next.isSome()

        return now.unwrap()
    }
}

fun <E> Collection<E>.iter(): Iter<E> {
    return CollectionIter(this.iterator())
}

class CollectionIter<E>(private val iterator: Iterator<E>) : Iter<E> {
    override fun next(): Option<E> {
        return if (iterator.hasNext()) {
            Some(iterator.next())
        } else {
            None()
        }
    }
}

fun <E> MutableList<E>.drain(): List<E> {
    val copy = this.toList()
    this.clear()
    return copy
}