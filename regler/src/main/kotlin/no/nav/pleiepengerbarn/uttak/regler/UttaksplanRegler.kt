package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.Avslått
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.MedlemskapRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilBeregningAvGrad
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilsynsbehovRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.domene.Årsaksbygger
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.math.BigDecimal
import java.time.Duration

internal object UttaksplanRegler {

    private val PeriodeRegler = linkedSetOf(
            MedlemskapRegel(),
            FerieRegel(),
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

        val perioder = mutableMapOf<LukketPeriode, UttaksPeriodeInfo>()

        knektePerioder.forEach { (periode, knekkpunktTyper) ->
            val avslåttÅrsaker = mutableSetOf<AvslåttÅrsak>()
            val årsakbygger = Årsaksbygger()
            PeriodeRegler.forEach { regel ->
                val utfall = regel.kjør(periode = periode, grunnlag = grunnlag)
                if (utfall is Avslått) {
                    avslåttÅrsaker.addAll(utfall.årsaker)
                } else if (utfall is TilBeregningAvGrad) {
                    årsakbygger.startBeregningAvGraderMed(utfall.hjemler)
                }
            }
            if (avslåttÅrsaker.isNotEmpty()) {
                perioder[periode] = AvslåttPeriode(
                        knekkpunktTyper = knekkpunktTyper,
                        kildeBehandlingUUID = grunnlag.behandlingUUID,
                        årsaker = avslåttÅrsaker
                )
            } else {
                val grader = finnGrader(periode, grunnlag)

                if (grader.årsak is AvslåttÅrsak) {
                    perioder[periode] = AvslåttPeriode(
                            knekkpunktTyper = knekkpunktTyper,
                            kildeBehandlingUUID = grunnlag.behandlingUUID,
                            årsaker = setOf(grader.årsak)
                    )
                } else if (grader.årsak is InnvilgetÅrsak) {
                    perioder[periode] = InnvilgetPeriode(
                            knekkpunktTyper = knekkpunktTyper,
                            kildeBehandlingUUID = grunnlag.behandlingUUID,
                            uttaksgrad = grader.uttaksgrad,
                            utbetalingsgrader = grader.utbetalingsgrader.map {Utbetalingsgrader(it.key, it.value)},
                            årsak = grader.årsak.årsak,
                            hjemler = grader.årsak.hjemler
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

    private fun finnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): AvklarteGrader {
        val tilsynsbehov = grunnlag.finnTilsynsbehov(periode)
        val etablertTilsyn = grunnlag.finnEtablertTilsyn(periode)
        val andreSøkeresTilsyn = grunnlag.finnAndreSøkeresTilsyn(periode)
        val arbeidPerArbeidsforhold = grunnlag.finnArbeidPerArbeidsforhold(periode)

        return AvklarGrader.avklarGrader(tilsynsbehov = tilsynsbehov, etablertTilsyn = etablertTilsyn, andreSøkeresTilsyn = andreSøkeresTilsyn, arbeid = arbeidPerArbeidsforhold)
    }

    private fun RegelGrunnlag.finnTilsynsbehov(periode: LukketPeriode): TilsynsbehovStørrelse {
        val tilsynsbehovPeriode = this.tilsynsbehov.keys.firstOrNull {it.overlapper(periode)}
        return if (tilsynsbehovPeriode != null) {
            this.tilsynsbehov[tilsynsbehovPeriode]?.prosent ?: TilsynsbehovStørrelse.PROSENT_0
        } else {
            TilsynsbehovStørrelse.PROSENT_0
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
                if (uttaksperiode is InnvilgetPeriode) {
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