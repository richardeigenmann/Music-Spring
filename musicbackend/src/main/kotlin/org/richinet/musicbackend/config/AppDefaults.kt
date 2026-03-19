package org.richinet.musicbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.defaults")
open class AppDefaults {
    var groupTypes: List<GroupTypeDefault> = mutableListOf()
    var groups: List<GroupDefault> = mutableListOf()

    open class GroupTypeDefault {
        var id: String = ""
        var name: String = ""
        var edit: String = ""
    }

    open class GroupDefault {
        var typeId: String = ""
        var name: String = ""
    }
}
