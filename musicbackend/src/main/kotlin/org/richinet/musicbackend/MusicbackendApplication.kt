package org.richinet.musicbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MusicbackendApplication

fun main(args: Array<String>) {
    runApplication<MusicbackendApplication>(*args)
}
