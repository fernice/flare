package de.krall.flare.css.properties.stylestruct

import de.krall.flare.css.StyleStruct
import de.krall.flare.css.properties.longhand.Attachment
import de.krall.flare.css.properties.longhand.BackgroundAttachmentDeclaration
import de.krall.flare.css.properties.longhand.BackgroundColorDeclaration
import de.krall.flare.css.value.computed.Color

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