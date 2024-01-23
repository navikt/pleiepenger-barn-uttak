package no.nav.pleiepengerbarn.uttak.server

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@EnableJwtTokenValidation
@Configuration
class SecurityConfig