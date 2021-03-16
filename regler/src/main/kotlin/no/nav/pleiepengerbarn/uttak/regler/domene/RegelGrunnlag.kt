package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.time.Duration

data class RegelGrunnlag(
    val behandlingUUID: BehandlingUUID,
    val barn: Barn,
    val søker: Søker,
    val pleiebehov: Map<LukketPeriode, Pleiebehov>,
    val søktUttak: List<SøktUttak>,
    val arbeid: List<Arbeid>,
    val tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    val lovbestemtFerie: List<LukketPeriode> = listOf(),
    val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
    val andrePartersUttaksplan: Map<Saksnummer, Uttaksplan> = mapOf(),
    val beredskapsperioder: Set<LukketPeriode> = setOf(),
    val nattevåksperioder: Set<LukketPeriode> = setOf()
) {

    internal fun finnArbeidPerArbeidsforhold(periode: LukketPeriode): Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo> {
        val arbeidPerArbeidsforhold = mutableMapOf<Arbeidsforhold, ArbeidsforholdPeriodeInfo>()
        this.arbeid.forEach { arbeid ->
            val periodeFunnet = arbeid.perioder.keys.firstOrNull {  it.overlapper(periode)}
            if (periodeFunnet != null) {
                val info = arbeid.perioder[periodeFunnet]
                if (info != null) {
                    arbeidPerArbeidsforhold[arbeid.arbeidsforhold] = info
                }
            }
        }
        return arbeidPerArbeidsforhold
    }


    fun finnPleiebehov(periode: LukketPeriode): Pleiebehov {
        val pleiebehovPeriode = this.pleiebehov.keys.firstOrNull {it.overlapper(periode)}
        return if (pleiebehovPeriode != null) {
            this.pleiebehov[pleiebehovPeriode] ?: Pleiebehov.PROSENT_0
        } else {
            Pleiebehov.PROSENT_0
        }
    }


}
