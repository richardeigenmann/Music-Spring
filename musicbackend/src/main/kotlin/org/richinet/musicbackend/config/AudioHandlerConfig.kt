package org.richinet.musicbackend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import java.io.File
import java.util.Collections

@Configuration
class AudioHandlerConfig {

    @Value("\${app.music-directory}")
    private lateinit var musicDirectory: String

    @Bean
    fun audioHandler(): ResourceHttpRequestHandler {
        return object : ResourceHttpRequestHandler() {
            override fun getResource(request: jakarta.servlet.http.HttpServletRequest): Resource? {
                return request.getAttribute("trackResource") as? Resource
            }
        }.apply {
            val path = if (musicDirectory.endsWith("/")) musicDirectory else "$musicDirectory/"
            // Ensure we use a URL that ends with a slash
            val resource = UrlResource("file:$path")
            setLocations(Collections.singletonList(resource) as List<Resource>)
        }
    }
}
