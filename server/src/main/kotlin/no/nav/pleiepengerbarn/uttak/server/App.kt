package no.nav.pleiepengerbarn.uttak.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.ClassPathResource
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter


@SpringBootApplication
@EntityScan("no.nav.pleiepengerbarn.uttak")
@ConfigurationPropertiesScan("no.nav.pleiepengerbarn.uttak")
@ComponentScan("no.nav.pleiepengerbarn.uttak")
class App {

    /**
     * Sikrer at Jackson 2 brukes for HTTP meldingskonvertering i RestTemplate/RestClient.
     * Nødvendig fordi Jackson 3  også er på classpath
     * via logstash-logback-encoder og token-support, og Jackson 3 sin
     * konverter kan ikke håndtere Jackson 2 typer som ObjectNode.
     */
    @Bean
    fun clientHttpMessageConvertersCustomizer(objectMapper: ObjectMapper): ClientHttpMessageConvertersCustomizer {
        return ClientHttpMessageConvertersCustomizer { converters: HttpMessageConverters.ClientBuilder ->
            converters.addCustomConverter(MappingJackson2HttpMessageConverter(objectMapper))
        }
    }

    @Bean
    fun serverHttpMessageConvertersCustomizer(objectMapper: ObjectMapper): ServerHttpMessageConvertersCustomizer {
        return ServerHttpMessageConvertersCustomizer { converters: HttpMessageConverters.ServerBuilder ->
            converters.addCustomConverter(MappingJackson2HttpMessageConverter(objectMapper))
        }
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        // Registrer CallId MDC propageringsfilter
        val filterRegistration = FilterRegistrationBean(LogFilter())
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
