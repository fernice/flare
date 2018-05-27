package de.krall.flare.css.selector0

import de.krall.flare.std.Option

sealed class QualifiedName {

    class None : QualifiedName()

    class Some(val localName: Option<String>, val prefix: QualifiedNamePrefix) : QualifiedName()
}