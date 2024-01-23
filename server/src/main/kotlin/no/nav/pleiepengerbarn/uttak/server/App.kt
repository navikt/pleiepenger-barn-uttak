package no.nav.pleiepengerbarn.uttak.server

import org.slf4j.LoggerFactory
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import java.util.*


@SpringBootApplication
@EntityScan("no.nav.pleiepengerbarn.uttak")
@ConfigurationPropertiesScan("no.nav.pleiepengerbarn.uttak")
@ComponentScan("no.nav.pleiepengerbarn.uttak")
class App {

    private val logger = LoggerFactory.getLogger(App::class.java)

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        // Registrer CallId MDC propageringsfilter
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun gitPropertiesPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
        val propsConfig = PropertySourcesPlaceholderConfigurer()
        propsConfig.setLocation(ClassPathResource("git.properties"))
        propsConfig.setIgnoreResourceNotFound(true)
        propsConfig.setIgnoreUnresolvablePlaceholders(true)
        return propsConfig
    }

}

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(App::class.java)
        .profiles("prodConfig")
        .build()

    app.setBannerMode(Banner.Mode.OFF)

    app.run(*args)
}
