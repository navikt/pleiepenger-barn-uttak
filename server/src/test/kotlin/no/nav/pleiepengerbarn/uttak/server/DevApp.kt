package no.nav.pleiepengerbarn.uttak.server

import org.springframework.boot.builder.SpringApplicationBuilder

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(App::class.java)
            .profiles("postgres").build()
    app.run(*args)
}
