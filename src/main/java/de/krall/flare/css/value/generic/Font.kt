package de.krall.flare.css.value.generic

open class FontInfo<T>(val keywordSize: KeywordSize,
                  val factor: Float,
                  val offset: T)

sealed class KeywordSize {

    class XXSmall : KeywordSize()
    class XSmall : KeywordSize()
    class Small : KeywordSize()
    class Medium : KeywordSize()
    class Large : KeywordSize()
    class XLarge : KeywordSize()
    class XXLarge : KeywordSize()
}