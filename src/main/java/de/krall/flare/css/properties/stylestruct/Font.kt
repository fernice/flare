package de.krall.flare.css.properties.stylestruct

import de.krall.flare.css.StyleStruct
import de.krall.flare.css.properties.longhand.FontFamilyDeclaration
import de.krall.flare.css.properties.longhand.FontSizeDeclaration
import de.krall.flare.css.value.computed.FontFamily
import de.krall.flare.css.value.computed.FontSize

class Font(private var fontFamily: FontFamily,
           private var fontSize: FontSize) : StyleStruct<Font> {

    fun getFontFamily(): FontFamily {
        return fontFamily
    }

    fun setFontFamily(fontFamily: FontFamily) {
        this.fontFamily = fontFamily
    }

    fun getFontSize(): FontSize {
        return fontSize
    }

    fun setFontSize(fontSize: FontSize) {
        this.fontSize = fontSize
    }

    override fun clone(): Font {
        return Font(fontFamily,
                fontSize)
    }

    companion object {

        val initial: Font by lazy {
            Font(
                    FontFamilyDeclaration.initialValue,
                    FontSizeDeclaration.initialValue
            )
        }
    }
}