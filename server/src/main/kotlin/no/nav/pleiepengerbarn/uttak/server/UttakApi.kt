package no.nav.pleiepengerbarn.uttak.server

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.Month

@RestController
@Tag(name = "Uttak API", description = "Operasjoner for uttak pleiepenger barn")
class UttakApi {

    @PostMapping("/uttaksplan")
    fun opprettUttaksplan(uttakInput: UttakInput):Uttaksplan {

        //TODO hent uttaksplan for andre parter

        val uttaksplan = UttakTjeneste.uttaksplan(RegelGrunnlag(
                tilsynsbehov = uttakInput.tilsynsbehov,
                søknadsperioder = uttakInput.søknadsperioder,
                arbeidsforhold = uttakInput.arbeidsforhold,
                tilsynsperioder = uttakInput.tilsynsperioder,
                ferier = uttakInput.ferier,
                andrePartersUttaksplan = listOf() //TODO andre uttaksplaner skal her
            )
        )
        //TODO lagre uttaksplan
        return uttaksplan
    }


    @GetMapping("/uttaksplan", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(description = "Uttaksplaner for alle etterspurte behandlinger.")
    fun hentUttaksplan(@RequestParam behandlingId: Set<BehandlingId>): ResponseEntity<Uttaksplaner> {
        //TODO fjern mock kode
        val uttaksplaner = Uttaksplaner(
                uttaksplaner = mapOf(behandlingId.first() to Uttaksplan(listOf(
                        Uttaksperiode(
                                periode = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)),
                                uttaksperiodeResultat = UttaksperiodeResultat(
                                        grad = Prosent(100)
                                )
                        )
                )
                )))

        return ResponseEntity.ok(uttaksplaner)
    }
}