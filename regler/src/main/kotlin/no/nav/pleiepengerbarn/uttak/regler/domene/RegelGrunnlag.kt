package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt
import java.time.Duration
import java.time.LocalDate
import java.util.*

data class RegelGrunnlag(
    val ytelseType: YtelseType = YtelseType.PSB,
    val saksnummer: Saksnummer,
    val behandlingUUID: UUID,
    val barn: Barn,
    val søker: Søker,
    val pleiebehov: Map<LukketPeriode, Pleiebehov>,
    val nyeReglerUtbetalingsgrad: LocalDate? = null,
    val søktUttak: List<SøktUttak>,
    val trukketUttak: List<LukketPeriode> = listOf(),
    val arbeid: List<Arbeid>,
    val tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    val lovbestemtFerie: List<LukketPeriode> = listOf(),
    val inngangsvilkår: Map<String, List<Vilkårsperiode>> = mapOf(),
    val andrePartersUttaksplanPerBehandling: Map<UUID, Uttaksplan> = mapOf(),
    val egneUttaksplanerAllePleietrengendePerBehandling: Map<UUID, Uttaksplan> = mapOf(),
    val vedtatteUttaksplanPerBehandling: Map<UUID, Uttaksplan> = mapOf(),
    val sisteVedtatteUttaksplanForBehandling: Map<UUID, UUID> = mapOf(),
    val forrigeUttaksplan: Uttaksplan? = null,
    val beredskapsperioder: Map<LukketPeriode, Utfall> = mapOf(),
    val nattevåksperioder: Map<LukketPeriode, Utfall> = mapOf(),
    val kravprioritetForBehandlinger: Map<LukketPeriode, List<UUID>> = mapOf(),
    val kravprioritetForEgneBehandlinger: Map<LukketPeriode, List<UUID>> = mapOf(),
    val utenlandsoppholdperioder: Map<LukketPeriode, UtenlandsoppholdInfo> = mapOf(),
    val overstyrtInput: Map<LukketPeriode, OverstyrtInput> = mapOf(),
    val inntektsgradering: Map<LukketPeriode, Inntektsgradering> = mapOf(),
    val commitId: String = ""
) {

    internal fun finnArbeidPerArbeidsforhold(periode: LukketPeriode): Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo> {
        val arbeidPerArbeidsforhold = mutableMapOf<Arbeidsforhold, ArbeidsforholdPeriodeInfo>()
        this.arbeid.forEach { arbeid ->
            val periodeFunnet = arbeid.perioder.keys.firstOrNull {  it.overlapperHelt(periode)}
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
        val pleiebehovPeriode = this.pleiebehov.keys.firstOrNull {it.overlapperHelt(periode)}
        return if (pleiebehovPeriode != null) {
            this.pleiebehov[pleiebehovPeriode] ?: Pleiebehov.PROSENT_0
        } else {
            Pleiebehov.PROSENT_0
        }
    }

    fun finnOverstyrtInput(periode: LukketPeriode): OverstyrtInput? {
        this.overstyrtInput.forEach {
            if (it.key.overlapperHelt(periode)) {
                return it.value
            }
        }
        return null
    }

    fun finnInntektsgradering(periode: LukketPeriode): Inntektsgradering? {
        this.inntektsgradering.forEach {
            if (it.key.overlapperHelt(periode)) {
                return it.value
            }
        }
        return null
    }


    fun finnNattevåk(periode: LukketPeriode): Utfall? {
        val overlappendePeriode = this.nattevåksperioder.keys.firstOrNull {it.overlapperHelt(periode)}
        if (overlappendePeriode != null) {
            return nattevåksperioder[overlappendePeriode]
        }
        return null
    }

    fun finnEtablertTilsyn(periode: LukketPeriode): Duration {
        val etablertTilsynPeriode = this.tilsynsperioder.keys.firstOrNull { it.overlapperHelt(periode) }
        return if (etablertTilsynPeriode != null) {
            this.tilsynsperioder[etablertTilsynPeriode] ?: Duration.ZERO
        } else {
            Duration.ZERO
        }
    }

    fun finnBeredskap(periode: LukketPeriode): Utfall? {
        val overlappendePeriode = this.beredskapsperioder.keys.firstOrNull {it.overlapperHelt(periode)}
        if (overlappendePeriode != null) {
            return beredskapsperioder[overlappendePeriode]
        }
        return null
    }


    fun finnOverseEtablertTilsynÅrsak(nattevåk: Utfall?, beredskap: Utfall?): OverseEtablertTilsynÅrsak? {
        if (nattevåk == Utfall.OPPFYLT && beredskap == Utfall.OPPFYLT) {
            return OverseEtablertTilsynÅrsak.NATTEVÅK_OG_BEREDSKAP
        } else if (beredskap == Utfall.OPPFYLT) {
            return OverseEtablertTilsynÅrsak.BEREDSKAP
        } else if (nattevåk == Utfall.OPPFYLT) {
            return OverseEtablertTilsynÅrsak.NATTEVÅK
        }
        return null
    }

    fun sjekkInngangsvilkår(periode: LukketPeriode): Pair<Utfall, Map<String, Utfall>> {
        val inngangsvilkårForPeriode = inngangsvilkårForPeriode(periode)
        val utfallInngangsvikår = inngangsvilkårForPeriode.utfallInngangsvilkår()
        return Pair(utfallInngangsvikår, inngangsvilkårForPeriode)
    }

    fun finnOppgittTilsyn(periode: LukketPeriode): Duration? {
        val søktUttak = this.søktUttak.firstOrNull { it.periode.overlapperHelt(periode) }
        return søktUttak?.oppgittTilsyn
    }

    private fun inngangsvilkårForPeriode(periode: LukketPeriode): Map<String, Utfall> {
        val inngangsvilkårForPeriode = mutableMapOf<String, Utfall>()
        inngangsvilkår.forEach { (vilkårskode, perioder) ->
            perioder.forEach { vilkårsperiode ->
                if (vilkårsperiode.periode.overlapperHelt(periode)) {
                    inngangsvilkårForPeriode[vilkårskode] = vilkårsperiode.utfall
                }
            }
        }
        return inngangsvilkårForPeriode
    }

    private fun Map<String, Utfall>.utfallInngangsvilkår(): Utfall {
        if (this.values.any { it == Utfall.IKKE_OPPFYLT }) {
            return Utfall.IKKE_OPPFYLT
        }
        return Utfall.OPPFYLT
    }
}
