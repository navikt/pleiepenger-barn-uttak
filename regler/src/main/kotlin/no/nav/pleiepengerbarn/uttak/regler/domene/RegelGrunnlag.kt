package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.time.Duration

data class RegelGrunnlag(
        val behandlingUUID: BehandlingUUID,
        val barn: Barn = Barn(),
        val søker: Søker,
        val tilsynsbehov: Map<LukketPeriode, Tilsynsbehov>,
        val søknadsperioder: List<LukketPeriode>,
        val arbeid: List<Arbeid>,
        val tilsynsperioder:Map<LukketPeriode, Duration> = mapOf(),
        val lovbestemtFerie:List<LukketPeriode> = listOf(),
        val andrePartersUttaksplan: Map<Saksnummer, Uttaksplan> = mapOf(),
        val ikkeMedlem: List<LukketPeriode> = listOf()
)
