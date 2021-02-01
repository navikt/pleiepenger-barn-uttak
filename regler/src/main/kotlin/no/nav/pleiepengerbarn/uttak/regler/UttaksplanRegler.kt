package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.regler.delregler.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.Avslått
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.MedlemskapRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilsynsbehovRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.math.BigDecimal
import java.time.Duration

internal object UttaksplanRegler {

    private val PeriodeRegler = linkedSetOf(
            MedlemskapRegel(),
            FerieRegel(),
            InngangsvilkårAvslåttRegel(),
            TilsynsbehovRegel()
    )

    private val UttaksplanRegler = linkedSetOf(
            BarnsDødRegel(), // Må kjøres først av uttaksplanreglene
            SøkersAlderRegel(),
            SøkersDødRegel()
    )

    internal fun fastsettUttaksplan(
            grunnlag: RegelGrunnlag,
            knektePerioder: Map<LukketPeriode,Set<KnekkpunktType>>) : Uttaksplan {

        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        knektePerioder.forEach { (periode, knekkpunktTyper) ->
            val avslåttÅrsaker = mutableSetOf<Årsak>()
            PeriodeRegler.forEach { regel ->
                val utfall = regel.kjør(periode = periode, grunnlag = grunnlag)
                if (utfall is Avslått) {
                    avslåttÅrsaker.addAll(utfall.årsaker)
                }
            }
            if (avslåttÅrsaker.isNotEmpty()) {
                perioder[periode] = UttaksperiodeInfo.avslag(
                    årsaker = avslåttÅrsaker,
                    knekkpunktTyper = knekkpunktTyper,
                    kildeBehandlingUUID = grunnlag.behandlingUUID
                )
            } else {
                val grader = finnGrader(periode, grunnlag)

                if (grader.årsak.innvilget) {
                    perioder[periode] = UttaksperiodeInfo.innvilgelse(
                        uttaksgrad = grader.uttaksgrad,
                        utbetalingsgrader = grader.utbetalingsgrader.map {Utbetalingsgrader(arbeidsforhold = it.key, utbetalingsgrad = it.value.utbetalingsgrad, normalArbeidstid = it.value.normalArbeidstid, faktiskArbeidstid = it.value.faktiskArbeidstid)},
                        årsak = grader.årsak,
                        graderingMotTilsyn = GraderingMotTilsyn(
                            pleiebehov = grader.graderingMotTilsyn.pleiebehov.prosent,
                            etablertTilsyn = grader.graderingMotTilsyn.etablertTilsyn,
                            andreSøkeresTilsyn = grader.graderingMotTilsyn.andreSøkeresTilsyn,
                            tilgjengeligForSøker = grader.graderingMotTilsyn.tilgjengeligForSøker
                        ),
                        knekkpunktTyper = knekkpunktTyper,
                        kildeBehandlingUUID = grunnlag.behandlingUUID

                    )
                } else {
                    perioder[periode] = UttaksperiodeInfo.avslag(
                        årsaker = setOf(grader.årsak),
                        knekkpunktTyper = knekkpunktTyper,
                        kildeBehandlingUUID = grunnlag.behandlingUUID
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

    private fun finnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): GraderBeregnet {
        val pleiebehov = grunnlag.finnPleiebehov(periode)
        val etablertTilsyn = grunnlag.finnEtablertTilsyn(periode)
        val andreSøkeresTilsyn = grunnlag.finnAndreSøkeresTilsyn(periode)
        val arbeidPerArbeidsforhold = grunnlag.finnArbeidPerArbeidsforhold(periode)

        return BeregnGrader.beregn(pleiebehov = pleiebehov, etablertTilsyn = etablertTilsyn, andreSøkeresTilsyn = andreSøkeresTilsyn, arbeid = arbeidPerArbeidsforhold)
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
                if (uttaksperiode != null && uttaksperiode.utfall  == Utfall.INNVILGET) {
                    andreSøkeresTilsynsgrad += uttaksperiode.uttaksgrad
                }
            }
        }
        return andreSøkeresTilsynsgrad
    }

    private fun RegelGrunnlag.finnArbeidPerArbeidsforhold(periode: LukketPeriode): Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo> {
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

}
