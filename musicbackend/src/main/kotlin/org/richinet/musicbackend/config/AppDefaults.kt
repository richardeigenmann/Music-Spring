package org.richinet.musicbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.defaults")
open class AppDefaults {
    var tagTypes: List<TagTypeDefault> = mutableListOf()
    var tags: List<TagDefault> = mutableListOf()

    open class TagTypeDefault {
        var id: String = ""
        var name: String = ""
        var edit: String = ""
    }

    open class TagDefault {
        var typeId: String = ""
        var name: String = ""
    }
}
