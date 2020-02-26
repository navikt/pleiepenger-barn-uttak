package no.nav.pleiepengerbarn.uttak.server

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DevApp

fun main(args: Array<String>) {
    runApplication<DevApp>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}
