package org.richinet.musicbackend.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class WebController {

    @GetMapping("/index.html")
    fun index(): String {
        return "index"
    }
}
