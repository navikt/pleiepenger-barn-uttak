package no.nav.pleiepengerbarn.uttak.server

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.IOException
import java.util.*
import java.util.function.Supplier

// Kopieret klasse fra familie-felles å oppgradert til jakarta
// https://github.com/navikt/familie-felles/blob/3cc4a074374717a7a916bdc683f91bd7abec984a/log/src/main/java/no/nav/familie/log/filter/LogFilter.kt
class LogFilter(
    /**
     * Filter init param used to specify a [&lt;Boolean&gt;][Supplier]
     * that will return whether stacktraces should be exposed or not
     * Defaults to always false
     */
    private val exposeErrorDetails: Supplier<Boolean> = Supplier { false },
    private val serverName: String? = null) : HttpFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilter(httpServletRequest: HttpServletRequest,
                          httpServletResponse: HttpServletResponse,
                          filterChain: FilterChain
    ) {
        val userId = resolveUserId(httpServletRequest)
        if (userId == null || userId.isEmpty()) {
            // user-id tracking only works if the client is stateful and supports cookies.
            // if no user-id is found, generate one for any following requests but do not use it on the
            // current request to avoid generating large numbers of useless user-ids.
            generateUserIdCookie(httpServletResponse)
        }
        val consumerId = httpServletRequest.getHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString())
        val callId = resolveCallId(httpServletRequest)
        MDC.put(MDC_CALL_ID, callId)
        MDC.put(MDC_USER_ID, userId)
        MDC.put(MDC_CONSUMER_ID, consumerId)
        MDC.put(MDC_REQUEST_ID, generateId())
        httpServletResponse.setHeader(NavHttpHeaders.NAV_CALL_ID.asString(), callId)
        if (serverName != null) {
            httpServletResponse.setHeader("Server", serverName)
        }
        try {
            filterWithErrorHandling(httpServletRequest, httpServletResponse, filterChain)
        } finally {
            MDC.remove(MDC_CALL_ID)
            MDC.remove(MDC_USER_ID)
            MDC.remove(MDC_CONSUMER_ID)
            MDC.remove(MDC_REQUEST_ID)
        }
    }

    @Throws(IOException::class, ServletException::class)
    private fun filterWithErrorHandling(httpServletRequest: HttpServletRequest,
                                        httpServletResponse: HttpServletResponse,
                                        filterChain: FilterChain) {
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse)
        } catch (e: Exception) {
            log.error(e.message, e)
            if (httpServletResponse.isCommitted) {
                log.error("failed with status={}", httpServletResponse.status)
                throw e
            }
            httpServletResponse.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            if (exposeErrorDetails.get()) {
                e.printStackTrace(httpServletResponse.writer)
            }
        }
    }

    override fun destroy() {}

    private companion object {
        // there is no consensus in NAV about header-names for correlation ids, so we support 'em all!
        // https://nav-it.slack.com/archives/C9UQ16AH4/p1538488785000100
        val NAV_CALL_ID_HEADER_NAMES =
            arrayOf(NavHttpHeaders.NAV_CALL_ID.asString(),
                "Nav-CallId",
                "Nav-Callid",
                "X-Correlation-Id")
        const val MDC_USER_ID = "userId"
        const val MDC_CONSUMER_ID = "consumerId"
        const val MDC_CALL_ID = "callId"
        const val MDC_REQUEST_ID = "requestId"

        val log = LoggerFactory.getLogger(LogFilter::class.java)
        const val RANDOM_USER_ID_COOKIE_NAME = "RUIDC"
        const val ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30

        fun resolveCallId(httpServletRequest: HttpServletRequest): String {
            return NAV_CALL_ID_HEADER_NAMES
                .mapNotNull { httpServletRequest.getHeader(it) }
                .firstOrNull { it.isNotEmpty() }
                ?: generateId()
        }

        fun generateUserIdCookie(httpServletResponse: HttpServletResponse) {
            val userId = generateId()
            val cookie = Cookie(RANDOM_USER_ID_COOKIE_NAME, userId).apply {
                path = "/"
                maxAge = ONE_MONTH_IN_SECONDS
                isHttpOnly = true
                secure = true
            }
            httpServletResponse.addCookie(cookie)
        }

        @JvmStatic fun generateId(): String {
            val uuid = UUID.randomUUID()
            return java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
        }

        private fun resolveUserId(httpServletRequest: HttpServletRequest): String? {
            return httpServletRequest.cookies?.firstOrNull { it -> RANDOM_USER_ID_COOKIE_NAME == it.name }?.value
        }
    }

    private enum class NavHttpHeaders(private val header: String) {
        NAV_PERSONIDENT("Nav-Personident"),
        NAV_CALL_ID("Nav-Call-Id"),
        NAV_CONSUMER_ID("Nav-Consumer-Id");

        fun asString(): String {
            return header
        }
    }
}