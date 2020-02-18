package no.nav.pleiepengerbarn.uttak.server

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttakTjeneste
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.Month

@RestController
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


    @GetMapping("/uttaksplan")
    fun hentUttaksplan(behandingId:BehandlingId):Uttaksplan {
        //TODO fjern mock kode
        return Uttaksplan(listOf(
                Uttaksperiode(
                        periode = LukketPeriode(LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.JANUARY, 31)),
                        uttaksperiodeResultat = UttaksperiodeResultat(
                                grad = Prosent(100)
                        )
                )
            )
        )
    }
}