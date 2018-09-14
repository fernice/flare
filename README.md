# Flare

flare is a CSS Engine written in Kotlin. It is the foundation to other projects like [reflare](https://github.com/fernice/reflare). flare is a engine only and
therefor is not biased towards any windowing toolkit. In fact it is meant to be used by any toolkit that wants to make use of a specification conform
CSS engine.

## Extend and Conformity

flares goal is to be fully specification conform both CSS 3 and once released CSS 4. Currently it is not the goal to cover the specification, but only the
portions that are contribute to the design of a component. There is currently no need for layouting as the implementation that use flare have their own
layouting engines. Even in the future it may better to create a constraint-based layouting engine instead of implementing a CSS one.
