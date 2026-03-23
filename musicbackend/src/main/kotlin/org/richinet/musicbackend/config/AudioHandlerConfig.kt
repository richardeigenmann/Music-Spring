package org.richinet.musicbackend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
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
            setLocations(Collections.singletonList(FileSystemResource(File(musicDirectory))) as List<Resource>)
        }
    }
}
