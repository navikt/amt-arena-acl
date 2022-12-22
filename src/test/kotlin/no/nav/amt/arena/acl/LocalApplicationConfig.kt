package no.nav.amt.arena.acl

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@EnableJwtTokenValidation
@Configuration
open class LocalApplicationConfig {}
