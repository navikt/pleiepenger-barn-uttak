package no.nav.pleiepengerbarn.uttak.server

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class App

fun main(args: Array<String>) {
    runApplication<App>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}
