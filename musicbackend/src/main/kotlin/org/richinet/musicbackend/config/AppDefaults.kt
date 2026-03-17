package org.richinet.musicbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.defaults")
class AppDefaults {
    var groupTypes: List<GroupTypeDefault> = mutableListOf()

    class GroupTypeDefault {
        var id: String = ""
        var name: String = ""
        var edit: String = ""
    }
}
