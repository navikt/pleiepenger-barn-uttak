package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.time.Duration

data class RegelGrunnlag(
    val behandlingUUID: BehandlingUUID,
    val barn: Barn = Barn(),
    val søker: Søker,
    val pleiebehov: Map<LukketPeriode, Pleiebehov>,
    val søknadsperioder: List<LukketPeriode>,
    val arbeid: List<Arbeid>,
    val tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    val lovbestemtFerie: List<LukketPeriode> = listOf(),
    val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
    val andrePartersUttaksplan: Map<Saksnummer, Uttaksplan> = mapOf()
)
