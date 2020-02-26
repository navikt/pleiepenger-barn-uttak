package no.nav.pleiepengerbarn.uttak.server

import org.springframework.boot.Banner
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<App>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}
