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
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.math.BigDecimal
import java.time.Duration

internal object UttaksplanRegler {

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
                        graderingMotTilsyn = GraderingMotTilsyn(
                            pleiebehov = grader.graderingMotTilsyn.pleiebehov.prosent,
                            etablertTilsyn = grader.graderingMotTilsyn.etablertTilsyn,
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

        return BeregnGrader.beregn(pleiebehov = pleiebehov, etablertTilsyn = etablertTilsyn, oppgittTilsyn = oppgittTilsyn, andreSøkeresTilsyn = andreSøkeresTilsyn, arbeid = arbeidPerArbeidsforhold)
    }

    private fun RegelGrunnlag.finnOppgittTilsyn(periode: LukketPeriode): Duration? {
        val søktUttak = this.søktUttak.firstOrNull {it.periode.overlapper(periode)}
        return søktUttak?.oppgittTilsyn
    }

    private fun RegelGrunnlag.finnPleiebehov(periode: LukketPeriode): Pleiebehov {
        val pleiebehovPeriode = this.pleiebehov.keys.firstOrNull {it.overlapper(periode)}
        return if (pleiebehovPeriode != null) {
            this.pleiebehov[pleiebehovPeriode] ?: Pleiebehov.PROSENT_0
        } else {
            Pleiebehov.PROSENT_0
        }
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
