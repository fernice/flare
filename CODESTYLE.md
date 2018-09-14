# Kotlin

# Null Values

flare does NOT make use of nullable types throughout the whole project, instead it uses a Rust like Optional namely
*fernice.std.Option* with its concrete forms *fernice.std.Some* and *fernice.std.None*. The use of such nullable types is
discouraged and may only be used in cases of calls to the standard library, such as *System.getProperty*, as well as other
third-party libraries.

# Exceptions

flare does also make only very limited use of exception throughout the whole project, as they are meant to for truly
exceptional cases only, in which error recovery is not feasible, as the cause of the error is a substantial bug in the overall
system. In all other case, in which error recovery is needed, the Rust like monad *fernice.std.Result* is used instead. The
Result is used as the return type of such a method, conveying either a success via *fernice.std.Ok* or a failure via 
*fernice.std.Err*.

# Formatting

flare tries to make use of primary constructors only. Every secondary constructor should instead be a companion method,
providing such functionality.

Every method or constructor with more the two arguments may be laid out in such way that each argument has is own separate
line with opening and closing parenthesis preceding and succeeding in also a separate line.