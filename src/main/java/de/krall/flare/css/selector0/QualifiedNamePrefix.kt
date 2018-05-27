package de.krall.flare.css.selector0

import de.flare.css.selector.NamespacePrefix
import de.flare.css.selector.NamespaceUrl

sealed class QualifiedNamePrefix {

    class ImplicitNoNamespace : QualifiedNamePrefix()

    class ImplicitAnyNamespace : QualifiedNamePrefix()

    class ImplicitDefaultNamespace(val url: NamespaceUrl) : QualifiedNamePrefix()

    class ExplicitNoNamespace : QualifiedNamePrefix()

    class ExplicitAnyNamespace : QualifiedNamePrefix()

    class ExplicitNamespace(val prefix: NamespacePrefix, val url: NamespaceUrl) : QualifiedNamePrefix()
}