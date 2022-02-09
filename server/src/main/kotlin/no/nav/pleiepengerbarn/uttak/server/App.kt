package no.nav.pleiepengerbarn.uttak.server

import no.nav.familie.log.filter.LogFilter
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EntityScan("no.nav.pleiepengerbarn.uttak")
@ConfigurationPropertiesScan("no.nav.pleiepengerbarn.uttak")
@ComponentScan("no.nav.pleiepengerbarn.uttak")
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
