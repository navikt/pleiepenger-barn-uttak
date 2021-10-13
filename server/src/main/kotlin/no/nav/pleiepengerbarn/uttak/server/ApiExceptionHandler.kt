package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.regler.ValideringException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ValideringException::class)
    fun handleValideringException(e: ValideringException): ResponseEntity<Any> {
        logger.warn(e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()//.contentType(MediaType.TEXT_PLAIN).body(e.message)
    }

}