package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.MutStyleStruct
import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.*
import de.krall.flare.style.value.computed.Color

interface Background : StyleStruct<MutBackground> {

    val color: Color
    val attachment: List<Attachment>
    val clip: Clip

    companion object {

        val initial: Background by lazy {
            StaticBackground(
                    BackgroundColorDeclaration.initialValue,
                    BackgroundAttachmentDeclaration.initialValue,
                    BackgroundClipDeclaration.initialValue
            )
        }
    }
}

private class StaticBackground(override val color: Color,
                               override val attachment: List<Attachment>,
                               override val clip: Clip) : Background {

    override fun clone(): MutBackground {
        return MutBackground(
                color,
                attachment,
                clip
        )
    }
}

class MutBackground(override var color: Color,
                    override var attachment: List<Attachment>,
                    override var clip: Clip) : Background, MutStyleStruct {

    override fun clone(): MutBackground {
        return MutBackground(
                color,
                attachment,
                clip
        )
    }
}