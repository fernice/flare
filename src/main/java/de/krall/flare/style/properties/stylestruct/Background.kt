package de.krall.flare.style.properties.stylestruct

import de.krall.flare.style.StyleStruct
import de.krall.flare.style.properties.longhand.Attachment
import de.krall.flare.style.properties.longhand.BackgroundAttachmentDeclaration
import de.krall.flare.style.properties.longhand.BackgroundColorDeclaration
import de.krall.flare.style.value.computed.Color

data class Background(private var color: Color,
                      private var attachment: List<Attachment>) : StyleStruct<Background> {

    fun getColor(): Color {
        return color
    }

    fun setColor(color: Color) {
        this.color = color
    }

    fun getAttachment(): List<Attachment> {
        return attachment
    }

    fun setAttachment(attachment: List<Attachment>) {
        this.attachment = attachment
    }

    override fun clone(): Background {
        return Background(color, attachment)
    }

    companion object {

        val initial: Background by lazy {
            Background(
                    BackgroundColorDeclaration.initialValue,
                    BackgroundAttachmentDeclaration.initialValue
            )
        }
    }
}