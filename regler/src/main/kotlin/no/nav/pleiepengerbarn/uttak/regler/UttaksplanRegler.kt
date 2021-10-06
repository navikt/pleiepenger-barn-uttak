package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.IkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.annenPart
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal object UttaksplanRegler {

    private val PeriodeRegler = linkedSetOf(
            FerieRegel(),
            BarnsDødPeriodeRegel()
    )

    private val UttaksplanRegler = linkedSetOf(
            InngangsvilkårIkkeOppfyltRegel(),
// NB: erstartet inntil videre med  BarnsDødPeriodeRegel
// BarnsDødRegel()
    )

    internal fun fastsettUttaksplan(grunnlag: RegelGrunnlag, knektePerioder: Map<SøktUttak,Set<KnekkpunktType>>) : Uttaksplan {
        // Fastsett periode
        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        knektePerioder.forEach { (søktUttaksperiode, knekkpunktTyper) ->
            val ikkeOppfyltÅrsaker = fastsettPeriodeRegler(søktUttaksperiode.periode, grunnlag)
            fastsettGrader(perioder, søktUttaksperiode.periode, grunnlag, knekkpunktTyper, ikkeOppfyltÅrsaker)
        }
        //Fastsett uttaksplan
        return fastsettUttaksplanRegler(perioder, grunnlag)
    }

    private fun fastsettPeriodeRegler(søktUttaksperiode: LukketPeriode, grunnlag: RegelGrunnlag): Set<Årsak> {
        val årsaker = mutableSetOf<Årsak>()
        var overstyrtÅrsak: Årsak? = null
        PeriodeRegler.forEach { regel ->
            val utfall = regel.kjør(periode = søktUttaksperiode, grunnlag = grunnlag)
            if (utfall is IkkeOppfylt) {
                årsaker.addAll(utfall.årsaker)
            } else if (utfall is TilBeregningAvGrad) {
                if (utfall.overstyrtÅrsak != null) {
                    overstyrtÅrsak = utfall.overstyrtÅrsak
                }
            }
        }
        if (overstyrtÅrsak != null) {
            return setOf(overstyrtÅrsak!!)
        }
        return årsaker
    }

    private fun fastsettGrader(
        perioder: MutableMap<LukketPeriode, UttaksperiodeInfo>,
        søktUttaksperiode: LukketPeriode,
        grunnlag: RegelGrunnlag,
        knekkpunktTyper: Set<KnekkpunktType>,
        årsaker: Set<Årsak>)
    {
        val grader = finnGrader(søktUttaksperiode, grunnlag)
        val nattevåk = grunnlag.finnNattevåk(søktUttaksperiode)
        val beredskap = grunnlag.finnBeredskap(søktUttaksperiode)
        val ikkeOppfyltÅrsaker = årsaker.filter { !it.oppfylt } .toSet()
        if (ikkeOppfyltÅrsaker.isNotEmpty()) {
            perioder[søktUttaksperiode] = UttaksperiodeInfo.ikkeOppfylt(
                utbetalingsgrader = grader.tilUtbetalingsgrader(false),
                søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                oppgittTilsyn = grader.oppgittTilsyn,
                årsaker = ikkeOppfyltÅrsaker,
                pleiebehov = grader.pleiebehov.prosent,
                graderingMotTilsyn = grader.graderingMotTilsyn,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = grunnlag.behandlingUUID.toString(),
                annenPart = grunnlag.annenPart(søktUttaksperiode),
                nattevåk = nattevåk,
                beredskap = beredskap
            )
        } else {
            if (grader.årsak.oppfylt) {
                val årsak = if (årsaker.size == 1) {
                    årsaker.first()
                } else {
                    grader.årsak
                }
                perioder[søktUttaksperiode] = UttaksperiodeInfo.oppfylt(
                    uttaksgrad = grader.uttaksgrad,
                    utbetalingsgrader = grader.tilUtbetalingsgrader(true),
                    søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                    oppgittTilsyn = grader.oppgittTilsyn,
                    årsak = årsak,
                    pleiebehov = grader.pleiebehov.prosent,
                    graderingMotTilsyn = grader.graderingMotTilsyn,
                    knekkpunktTyper = knekkpunktTyper,
                    kildeBehandlingUUID = grunnlag.behandlingUUID.toString(),
                    annenPart = grunnlag.annenPart(søktUttaksperiode),
                    nattevåk = nattevåk,
                    beredskap = beredskap
                )
            } else {
                perioder[søktUttaksperiode] = UttaksperiodeInfo.ikkeOppfylt(
                    utbetalingsgrader = grader.tilUtbetalingsgrader(false),
                    søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                    oppgittTilsyn = grader.oppgittTilsyn,
                    årsaker = setOf(grader.årsak),
                    pleiebehov = grader.pleiebehov.prosent,
                    graderingMotTilsyn = grader.graderingMotTilsyn,
                    knekkpunktTyper = knekkpunktTyper,
                    kildeBehandlingUUID = grunnlag.behandlingUUID.toString(),
                    annenPart = grunnlag.annenPart(søktUttaksperiode),
                    nattevåk = nattevåk,
                    beredskap = beredskap
                )
            }
        }
    }

    private fun fastsettUttaksplanRegler(perioder: Map<LukketPeriode, UttaksperiodeInfo>, grunnlag: RegelGrunnlag): Uttaksplan {
        var uttaksplan = Uttaksplan(perioder, grunnlag.trukketUttak)
        UttaksplanRegler.forEach {uttaksplanRegler ->
            uttaksplan = uttaksplanRegler.kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
            )
        }
        return uttaksplan
    }


    private fun GraderBeregnet.tilUtbetalingsgrader(oppfylt: Boolean): List<Utbetalingsgrader> {
        return this.utbetalingsgrader.map {
            val utbetalingsgrad  = if (oppfylt) it.value.utbetalingsgrad else BigDecimal.ZERO
            Utbetalingsgrader(
                arbeidsforhold = it.key,
                utbetalingsgrad = utbetalingsgrad,
                normalArbeidstid = it.value.normalArbeidstid,
                faktiskArbeidstid = it.value.faktiskArbeidstid
            )
        }
    }

    private fun finnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): GraderBeregnet {
        val pleiebehov = grunnlag.finnPleiebehov(periode)
        val etablertTilsyn = grunnlag.finnEtablertTilsyn(periode)
        val oppgittTilsyn = grunnlag.finnOppgittTilsyn(periode)
        val (andreSøkeresTilsynReberegnet, andrePartersTilsyn) = grunnlag.finnAndreSøkeresTilsyn(periode)
        val arbeidPerArbeidsforhold = grunnlag.finnArbeidPerArbeidsforhold(periode)
        val overseEtablertTilsynÅrsak = grunnlag.avklarOverseEtablertTilsynÅrsak(periode, etablertTilsyn)

        return BeregnGrader.beregn(
            pleiebehov = pleiebehov,
            etablertTilsyn = etablertTilsyn,
            oppgittTilsyn = oppgittTilsyn,
            andreSøkeresTilsyn = andrePartersTilsyn,
            andreSøkeresTilsynReberegnet = andreSøkeresTilsynReberegnet,
            arbeid = arbeidPerArbeidsforhold,
            overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
        )
    }

    private fun RegelGrunnlag.avklarOverseEtablertTilsynÅrsak(periode: LukketPeriode, etablertTilsyn: Duration): OverseEtablertTilsynÅrsak? {
        val etablertTilsynsprosent = BigDecimal(etablertTilsyn.toMillis()).setScale(2, RoundingMode.HALF_UP) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
        if (etablertTilsynsprosent > Prosent.ZERO && etablertTilsynsprosent < TI_PROSENT) {
            return OverseEtablertTilsynÅrsak.FOR_LAVT
        }
        val nattevåk = this.finnNattevåk(periode)
        val beredskap = this.finnBeredskap(periode)
        return finnOverseEtablertTilsynÅrsak(nattevåk, beredskap)
    }

    private fun RegelGrunnlag.finnOppgittTilsyn(periode: LukketPeriode): Duration? {
        val søktUttak = this.søktUttak.firstOrNull {it.periode.overlapperHelt(periode)}
        return søktUttak?.oppgittTilsyn
    }

    private fun RegelGrunnlag.finnEtablertTilsyn(periode: LukketPeriode): Duration {
        val etablertTilsynPeriode = this.tilsynsperioder.keys.firstOrNull {it.overlapperHelt(periode)}
        return if (etablertTilsynPeriode != null) {
            this.tilsynsperioder[etablertTilsynPeriode] ?: Duration.ZERO
        } else {
            Duration.ZERO
        }
    }

}
