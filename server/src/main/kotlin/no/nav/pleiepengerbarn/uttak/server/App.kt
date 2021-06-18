package no.nav.pleiepengerbarn.uttak.server

import no.nav.familie.log.filter.LogFilter
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean

@SpringBootApplication
class App {

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        // Registrer CallId MDC propageringsfilter
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

}

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(App::class.java)
        .profiles("prodConfig")
        .build()

    app.setBannerMode(Banner.Mode.OFF)

    app.run(*args)
}
