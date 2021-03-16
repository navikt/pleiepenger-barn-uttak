package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.regler.delregler.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.IkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.PleiebehovRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.annenPart
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.math.BigDecimal
import java.time.Duration

internal object UttaksplanRegler {

    private val TI_PROSENT = Prosent(10)
    private val HUNDRE_PROSENT = Prosent(100)

    private val FULL_DAG = Duration.ofHours(7).plusMinutes(30)


    private val PeriodeRegler = linkedSetOf(
            FerieRegel(),
            PleiebehovRegel()
    )

    private val UttaksplanRegler = linkedSetOf(
            InngangsvilkårIkkeOppfyltRegel(),
            BarnsDødRegel()
    )

    internal fun fastsettUttaksplan(
            grunnlag: RegelGrunnlag,
            knektePerioder: Map<SøktUttak,Set<KnekkpunktType>>) : Uttaksplan {

        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        knektePerioder.forEach { (søktUttaksperiode, knekkpunktTyper) ->
            val ikkeOppfyltÅrsaker = mutableSetOf<Årsak>()
            PeriodeRegler.forEach { regel ->
                val utfall = regel.kjør(periode = søktUttaksperiode.periode, grunnlag = grunnlag)
                if (utfall is IkkeOppfylt) {
                    ikkeOppfyltÅrsaker.addAll(utfall.årsaker)
                }
            }
            val grader = finnGrader(søktUttaksperiode.periode, grunnlag)
            if (ikkeOppfyltÅrsaker.isNotEmpty()) {
                perioder[søktUttaksperiode.periode] = UttaksperiodeInfo.ikkeOppfylt(
                    utbetalingsgrader = grader.tilUtbetalingsgrader(false),
                    søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                    årsaker = ikkeOppfyltÅrsaker,
                    pleiebehov = grader.pleiebehov.prosent,
                    knekkpunktTyper = knekkpunktTyper,
                    kildeBehandlingUUID = grunnlag.behandlingUUID,
                    annenPart = grunnlag.annenPart(søktUttaksperiode.periode)
                )
            } else {

                if (grader.årsak.oppfylt) {
                    perioder[søktUttaksperiode.periode] = UttaksperiodeInfo.oppfylt(
                        uttaksgrad = grader.uttaksgrad,
                        utbetalingsgrader = grader.tilUtbetalingsgrader(true),
                        søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                        årsak = grader.årsak,
                        pleiebehov = grader.pleiebehov.prosent,
                        graderingMotTilsyn = GraderingMotTilsyn(
                            etablertTilsyn = grader.graderingMotTilsyn.etablertTilsyn,
                            overseEtablertTilsynÅrsak = grader.graderingMotTilsyn.overseEtablertTilsynÅrsak,
                            andreSøkeresTilsyn = grader.graderingMotTilsyn.andreSøkeresTilsyn,
                            tilgjengeligForSøker = grader.graderingMotTilsyn.tilgjengeligForSøker
                        ),
                        knekkpunktTyper = knekkpunktTyper,
                        kildeBehandlingUUID = grunnlag.behandlingUUID,
                        annenPart = grunnlag.annenPart(søktUttaksperiode.periode)
                    )
                } else {
                    perioder[søktUttaksperiode.periode] = UttaksperiodeInfo.ikkeOppfylt(
                        utbetalingsgrader = grader.tilUtbetalingsgrader(false),
                        søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                        årsaker = setOf(grader.årsak),
                        pleiebehov = grader.pleiebehov.prosent,
                        knekkpunktTyper = knekkpunktTyper,
                        kildeBehandlingUUID = grunnlag.behandlingUUID,
                        annenPart = grunnlag.annenPart(søktUttaksperiode.periode)
                    )
                }
            }
        }

        var uttaksplan = Uttaksplan(perioder = perioder)

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
        val andreSøkeresTilsyn = grunnlag.finnAndreSøkeresTilsyn(periode)
        val arbeidPerArbeidsforhold = grunnlag.finnArbeidPerArbeidsforhold(periode)
        val overseEtablertTilsynÅrsak = grunnlag.avklarOverseEtablertTilsynÅrsak(periode, etablertTilsyn)

        return BeregnGrader.beregn(
            pleiebehov = pleiebehov,
            etablertTilsyn = etablertTilsyn,
            oppgittTilsyn = oppgittTilsyn,
            andreSøkeresTilsyn = andreSøkeresTilsyn,
            arbeid = arbeidPerArbeidsforhold,
            overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak
        )
    }

    private fun RegelGrunnlag.avklarOverseEtablertTilsynÅrsak(periode: LukketPeriode, etablertTilsyn: Duration): OverseEtablertTilsynÅrsak? {
        val etablertTilsynsprosent = BigDecimal(etablertTilsyn.toMillis()).setScale(2) / BigDecimal(FULL_DAG.toMillis()) * HUNDRE_PROSENT
        if (etablertTilsynsprosent > Prosent.ZERO && etablertTilsynsprosent < TI_PROSENT) {
            return OverseEtablertTilsynÅrsak.FOR_LAVT
        }
        val overlappBeredskap = this.beredskapsperioder.overlappendePeriode(periode)
        val overlappNattevåk = nattevåksperioder.overlappendePeriode(periode)
        if (overlappBeredskap != null && overlappNattevåk != null) {
            return OverseEtablertTilsynÅrsak.NATTEVÅK_OG_BEREDSKAP
        } else if (overlappBeredskap != null) {
            return OverseEtablertTilsynÅrsak.BEREDSKAP
        } else if (overlappNattevåk != null) {
            return OverseEtablertTilsynÅrsak.NATTEVÅK
        }
        return null
    }

    private fun RegelGrunnlag.finnOppgittTilsyn(periode: LukketPeriode): Duration? {
        val søktUttak = this.søktUttak.firstOrNull {it.periode.overlapper(periode)}
        return søktUttak?.oppgittTilsyn
    }

    private fun RegelGrunnlag.finnEtablertTilsyn(periode: LukketPeriode): Duration {
        val etablertTilsynPeriode = this.tilsynsperioder.keys.firstOrNull {it.overlapper(periode)}
        return if (etablertTilsynPeriode != null) {
            this.tilsynsperioder[etablertTilsynPeriode] ?: Duration.ZERO
        } else {
            Duration.ZERO
        }
    }

    private fun RegelGrunnlag.finnAndreSøkeresTilsyn(periode: LukketPeriode): BigDecimal {
        var andreSøkeresTilsynsgrad = BigDecimal.ZERO
        this.andrePartersUttaksplan.values.forEach { uttaksplan ->
            val overlappendePeriode = uttaksplan.perioder.keys.firstOrNull {it.overlapper(periode)}
            if (overlappendePeriode != null) {
                val uttaksperiode = uttaksplan.perioder[overlappendePeriode]
                if (uttaksperiode != null && uttaksperiode.utfall  == Utfall.OPPFYLT) {
                    andreSøkeresTilsynsgrad += uttaksperiode.uttaksgrad
                }
            }
        }
        return andreSøkeresTilsynsgrad
    }


}
