package no.nav.pleiepengerbarn.uttak.server

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.util.*

@Component
@Order(LOWEST_PRECEDENCE)
class LogFilter: GenericFilterBean() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val k9CallIdHeader = "callId"

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        putValues(HttpServletRequest::class.java.cast(request))
    }

    private fun putValues(request: HttpServletRequest) {
        try {
            val hentetCallIdHeader = request.getHeader(k9CallIdHeader) ?: UUID.randomUUID().toString()
            MDC.put(k9CallIdHeader, hentetCallIdHeader)
        } catch (e: Exception) {
            log.warn("Noe gikk galt ved setting av MDC-verdier for request {}, MDC-verdier er inkomplette", request.requestURI, e)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }
}
