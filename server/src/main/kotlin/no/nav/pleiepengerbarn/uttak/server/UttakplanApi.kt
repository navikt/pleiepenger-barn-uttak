package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.Month

@RestController
@Tag(name = "Uttak API", description = "Operasjoner for uttak pleiepenger barn")
class UttakplanApi {

    private companion object {
        private const val Path = "/uttaksplan"
        private const val BehandlingId = "behandlingId"
    }

    @PostMapping(Path, consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Opprette en ny uttaksplan. Tar inn grunnlaget som skal tas med i betraktning for å utlede uttaksplanen.")
    fun opprettUttaksplan(
            @RequestBody uttaksgrunnlag: Uttaksgrunnlag,
            uriComponentsBuilder: UriComponentsBuilder): ResponseEntity<Uttaksplan> {

        //TODO hent uttaksplan for andre parter
        //TODO lagre uttaksplan

        val uri = uriComponentsBuilder
                .path(Path)
                .queryParam(BehandlingId, uttaksgrunnlag.behandlingId)
                .build()
                .toUri()

        return ResponseEntity
                .created(uri)
                .body(dummyUttaksplan())
    }


    @GetMapping(Path, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Uttaksplaner for alle etterspurte behandlinger.")
    fun hentUttaksplan(@RequestParam behandlingId: Set<BehandlingId>): ResponseEntity<Uttaksplaner> {

        //TODO fjern mock kode
        val uttaksplaner = Uttaksplaner(
                uttaksplaner = mapOf(
                        behandlingId.first() to dummyUttaksplan()
                )
        )

        return ResponseEntity.ok(uttaksplaner)
    }


    private fun dummyUttaksplan() = Uttaksplan(listOf(
            Uttaksperiode(
                    periode = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)),
                    uttaksperiodeResultat = UttaksperiodeResultat(
                            grad = Prosent(100)
                    )
            )
    ))
}

