# Flare

modern-flare is a CSS Engine written in Kotlin.

# Kotlin

# Null Values

modern-flare does NOT make use of nullable types throughout the whole project, instead it uses a Rust like Optional namely
*modern.std.Option* with its concrete forms *modern.std.Some* and *modern.std.None*. The use of such nullable types is
discouraged and may only be used in cases of calls to the standard library, such as *System.getProperty*, as well as other
third-party libraries.

# Exceptions

modern-flare does also make only very limited use of exception throughout the whole project, as they are meant to for truly
exceptional cases only, in which error recovery is not feasible, as the cause of the error is a substantial bug in the overall
system. In all other case, in which error recovery is needed, the Rust like monad *modern.std.Result* is used instead. The
Result is used as the return type of such a method, conveying either a success via *modern.std.Ok* or a failure via 
*modern.std.Err*.

# Formatting

modern-flare tries to make use of primary constructors only. Every secondary constructor should instead be a companion method,
providing such functionality.

Every method or constructor with more the two arguments may be laid out in such way that each argument has is own separate
line with opening and closing parenthesis preceding and succeeding in also a separate line.

# Structure

## Parser

Parsing happens across the projects in the very objects that should be parsed into. Every parse method relies on the Parser
defined under *org.fernice.flare.cssparser*. It provide common methods for advancing the token stream, keeping track of nested
blocks and expecting certain kind of tokens. It might be accommodated by *org.fernice.flare.style.parser.ParserContext* for
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
basic interface that ensure the availability of required functions. The two main interfaces are *org.fernice.flare.dom.Element* and
*org.fernice.flare.dom.Device*, where Element represent a node in the DOM and Device the owner of the tree as well as the display.
