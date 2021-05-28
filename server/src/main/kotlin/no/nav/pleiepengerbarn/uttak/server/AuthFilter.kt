package no.nav.pleiepengerbarn.uttak.server

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthFilter : Filter {

    @Value("\${NAV_PSB_UTTAK_TOKEN:}")
    private lateinit var sharedSecret
            : String

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        val request = req as HttpServletRequest
        val response = res as HttpServletResponse
        if (sharedSecret != request.getHeader("Nav-Psb-Uttak-Token")) {
            response.sendError(403)
            return
        }
        chain.doFilter(request, response)
    }
}