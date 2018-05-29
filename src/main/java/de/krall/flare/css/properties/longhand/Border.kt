package de.krall.flare.css.properties.longhand

import de.krall.flare.css.properties.Longhand
import sun.security.util.Length

@Longhand(
        "border-top",
        Length::class,
        initialValue = "computed.Length.zero()",
        initialValueSpecified = "specified.Length.zero()"
        )
class Border