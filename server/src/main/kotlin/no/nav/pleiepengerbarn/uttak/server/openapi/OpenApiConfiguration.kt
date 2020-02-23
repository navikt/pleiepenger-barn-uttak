package no.nav.pleiepengerbarn.uttak.server.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class OpenApiConfiguration {
    @Bean
    internal fun openApi(
            @Value("\${no.nav.navn}") navn: String,
            @Value("\${no.nav.beskrivelse}") beskrivelse: String,
            @Value("\${no.nav.versjon}") versjon: String,
            @Value("\${no.nav.security.jwt.issuer.azure.accepted_audience}") azureClientId: String
    ): OpenAPI = OpenAPI()
            .info(
                    Info()
                            .title(navn)
                            .description("$beskrivelse\n\nScope for å nå tjenesten: `$azureClientId/.default`")
                            .version(versjon.semver())
                            .contact(
                                    Contact()
                                            .name("Arbeids- og velferdsdirektoratet")
                                            .url("https://www.nav.no")
                            )
                            .license(
                                    License()
                                            .name("MIT")
                                            .url("https://github.com/navikt/pleiepenger-barn-uttak/blob/master/LICENSE")
                            )
            )
}

private fun String.semver() = substringBefore("-")