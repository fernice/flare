# Flare

Flare is a CSS Engine written in Kotlin.

## Parser

Parsing happens across the projects in the very objects that should be parsed into. Every parse method relies on the Parser
defined under *de.krall.flare.cssparser*. It provide common methods for advancing the token stream, keeping track of nested
blocks and expecting certain kind of tokens. It might be accommodated by *de.krall.flare.style.parser.ParserContext* for
higher lever parse methods such as those related to lengths.

The parsing code is spread across the project purposely as Flare does not use any Parse Generators nor does it use an
intermediate **A**bstract **S**yntax **T**ree. This has been done because of flatness of such an AST. Its more efficient and
easier to represent to use concrete classes for each value instead of any kind of tree representation. Especially as values
have both a computed and used value form which should at best correlate.

## Selector

## Property

## Values

## DOM

The DOM (**D**ocument **O**bject **M**odel) is completely abstract and therefor open to the implementor. Flare does only define
basic interface that ensure the availability of required functions. The two main interfaces are *de.krall.flare.dom.Element* and
*de.krall.flare.dom.Device*, where Element represent a node in the DOM and Device the owner of the tree as well as the display.
