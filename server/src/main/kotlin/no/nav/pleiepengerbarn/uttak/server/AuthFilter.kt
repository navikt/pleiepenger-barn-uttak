package no.nav.pleiepengerbarn.uttak.server

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthFilter : Filter {

    companion object {
        private const val livenessPath = "/pleiepenger-barn-uttak/internal/actuator/health"
        private const val readinessPath = "/pleiepenger-barn-uttak/internal/actuator/info"
        private const val prometheusPath = "/pleiepenger-barn-uttak/internal/actuator/prometheus"
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @Value("\${NAV_PSB_UTTAK_TOKEN:no_secret}")
    private lateinit var sharedSecret
            : String

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        val request = req as HttpServletRequest
        val response = res as HttpServletResponse

        val hoppOverAuthFilter = request.requestURI in listOf(livenessPath, readinessPath, prometheusPath)

        if (!hoppOverAuthFilter && sharedSecret != request.getHeader("NAV_PSB_UTTAK_TOKEN")) {
            logger.warn("Ingen adgang til: ${request.requestURI}")
            response.sendError(403)
            return
        }
        chain.doFilter(request, response)
    }
}