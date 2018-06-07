package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.Attachment
import de.krall.flare.style.properties.longhand.BackgroundAttachmentDeclaration
import de.krall.flare.style.properties.longhand.BackgroundColorDeclaration
import de.krall.flare.style.value.computed.Color

interface Background : StyleStruct<MutBackground> {

    val color: Color
    val attachment: List<Attachment>

    companion object {

        val initial: Background by lazy {
            StaticBackground(
                    BackgroundColorDeclaration.initialValue,
                    BackgroundAttachmentDeclaration.initialValue
            )
        }
    }
}

private class StaticBackground(override val color: Color,
                               override val attachment: List<Attachment>) : Background {

    override fun clone(): MutBackground {
        return MutBackground(
                color,
                attachment
        )
    }
}

class MutBackground(override var color: Color,
                    override var attachment: List<Attachment>) : Background, MutStyleStruct {

    override fun clone(): MutBackground {
        return MutBackground(
                color,
                attachment
        )
    }
}