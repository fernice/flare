package de.krall.flare.css.value.computed

import de.krall.flare.css.value.ComputedValue
import de.krall.flare.css.value.generic.KeywordSize

import de.krall.flare.css.value.generic.FontInfo as GenericKeywordInfo

class FontFamily : ComputedValue

class FontSize(val size: NonNegativeLength,
               val info: KeywordInfo)

class KeywordInfo(keyword: KeywordSize,
                  factor: Float,
                  offset: NonNegativeLength) : GenericKeywordInfo<NonNegativeLength>(keyword, factor, offset)