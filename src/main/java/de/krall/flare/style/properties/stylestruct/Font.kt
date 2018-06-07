package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.FontFamilyDeclaration
import de.krall.flare.style.properties.longhand.FontSizeDeclaration
import de.krall.flare.style.value.computed.FontFamily
import de.krall.flare.style.value.computed.FontSize

interface Font : StyleStruct<MutFont> {

    val fontFamily: FontFamily
    val fontSize: FontSize

    companion object {

        val initial: Font by lazy {
            StaticFont(
                    FontFamilyDeclaration.initialValue,
                    FontSizeDeclaration.initialValue
            )
        }
    }
}

class StaticFont(override val fontFamily: FontFamily,
                 override val fontSize: FontSize) : Font {

    override fun clone(): MutFont {
        return MutFont(
                fontFamily,
                fontSize
        )
    }
}

class MutFont(override var fontFamily: FontFamily,
              override var fontSize: FontSize) : Font, MutStyleStruct {

    override fun clone(): MutFont {
        return MutFont(
                fontFamily,
                fontSize
        )
    }
}