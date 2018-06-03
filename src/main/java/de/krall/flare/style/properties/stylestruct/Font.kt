package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.FontFamilyDeclaration
import de.krall.flare.style.properties.longhand.FontSizeDeclaration
import de.krall.flare.style.value.computed.FontFamily
import de.krall.flare.style.value.computed.FontSize

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