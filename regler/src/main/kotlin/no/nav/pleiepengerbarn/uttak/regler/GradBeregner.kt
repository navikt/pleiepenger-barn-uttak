package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration

/**
 * https://confluence.adeo.no/display/SIF/Beskrivelse+av+uttakskomponenten
 */
internal object GradBeregner {

    private val TilsynsGrad = Prosent(0)
    private val TakForYtelsePåGrunnAvTilsyn = Prosent(100)
    private val GraderingsfaktorPåGrunnAvTilsynIPerioden = Prosent(100)
    private val EnVirkedag = Duration.ofHours(7).plusMinutes(30)

    internal fun beregnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): Grader {
        val fraværsGrader = mutableMapOf<Arbeidsforhold, Prosent>()
        var sumAvFraværIPerioden: Duration = Duration.ZERO


        val antallVirketimerIPerioden = EnVirkedag.multipliedBy(periode.antallVirkedager())
        val maksimaltAntallVirketimerViKanGiYtelseForIPerioden = EnVirkedag.multipliedBy(periode.antallVirkedager()) //TODO ta hensyn til tilsyn

        grunnlag.arbeid.forEach { arbeidsforholdOgArbeidsperioder ->
            arbeidsforholdOgArbeidsperioder.perioder.entries.firstOrNull {
                it.key.overlapper(periode)
            }?.apply {
                val fraværIPerioden = this.value.fraværIPerioden(periode)
                sumAvFraværIPerioden = sumAvFraværIPerioden.plus(fraværIPerioden)
                fraværsGrader[arbeidsforholdOgArbeidsperioder.arbeidsforhold] = Prosent(fraværIPerioden.dividedBy(antallVirketimerIPerioden) * 100)
            }
        }

        val uavkortetGrad = beregnGrad(TakForYtelsePåGrunnAvTilsyn, TilsynsGrad, sumAvFraværIPerioden, antallVirketimerIPerioden, maksimaltAntallVirketimerViKanGiYtelseForIPerioden)
        val avkortetGrad = finnAvkortetGrad(periode, grunnlag, uavkortetGrad)
        return Grader(
                grad = avkortetGrad,
                utbetalingsgrader = fraværsGrader.onEach {
                    it.value * GraderingsfaktorPåGrunnAvTilsynIPerioden / Prosent(100)
                }
        )

    }


    private fun beregnGrad(takForYtelsePåGrunnAvTilsyn:Prosent, tilsynsGradIPerioden:Prosent, sumAvFraværIPerioden:Duration, antallVirketimerIPerioden:Duration, maksimaltAntallVirketimerViKanGiYtelseForIPerioden:Duration):Prosent {
        if (tilsynsGradIPerioden > Prosent(80)) {
            return Prosent(0)
        }
        if (sumAvFraværIPerioden < antallVirketimerIPerioden) {
            if (sumAvFraværIPerioden < maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
                return Prosent.valueOf(100) - BigDecimal(sumAvFraværIPerioden.toMillis()).setScale(2, RoundingMode.HALF_UP)/BigDecimal(antallVirketimerIPerioden.toMillis()).setScale(2, RoundingMode.HALF_UP)*BigDecimal(100).setScale(2, RoundingMode.HALF_UP)
            } else {
                return BigDecimal(maksimaltAntallVirketimerViKanGiYtelseForIPerioden.toMillis())/BigDecimal(antallVirketimerIPerioden.toMillis())*BigDecimal(100).setScale(2, RoundingMode.HALF_UP)
            }

        } else {
            return takForYtelsePåGrunnAvTilsyn
        }

    }

    private fun finnAvkortetGrad(periode:LukketPeriode, grunnlag: RegelGrunnlag, uavkortetGrad:Prosent):Prosent {
        val tilsynsbehov = finnTilsynsbehov(periode, grunnlag)
        val sumAndreParter =  finnSumAndreParter(periode, grunnlag)
        val tilgjengeligRestGrad = tilsynsbehov - sumAndreParter
        if (tilgjengeligRestGrad < Prosent(0)) {
            return Prosent(0)
        }
        if (uavkortetGrad >= tilgjengeligRestGrad) {
            return tilgjengeligRestGrad }
        return uavkortetGrad
    }


    private fun finnSumAndreParter(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        var sumAndreParter = Prosent.ZERO
        grunnlag.andrePartersUttaksplan.forEach {uttaksplan ->

        val annenPartsPeriode = uttaksplan.perioder
                    .filter { overlapper(it.key, periode) }
                    .filter { it.value is InnvilgetPeriode }
                    .values.firstOrNull() as InnvilgetPeriode?

            if (annenPartsPeriode != null) {
                sumAndreParter += annenPartsPeriode.grad
            }
        }
        return sumAndreParter
    }


    private fun finnTilsyn(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsyn = grunnlag.tilsynsperioder.entries.find { overlapper(it.key, periode) }
        if (tilsyn != null) {
            return tilsyn.value.grad.setScale(2, RoundingMode.HALF_EVEN)
        }
        return Prosent.ZERO.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun finnTilsynsbehov(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsynsbehovSomOverlapperMedPeriode = grunnlag.tilsynsbehov.entries.find { overlapper(it.key, periode) }
        return when (tilsynsbehovSomOverlapperMedPeriode?.value?.prosent) {
            TilsynsbehovStørrelse.PROSENT_100 -> Prosent(100)
            TilsynsbehovStørrelse.PROSENT_200 -> Prosent(200)
            else -> Prosent.ZERO
        }.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun max(a:Prosent, b:Prosent):Prosent {
        if (b > a) {
            return b
        }
        return a
    }

    private fun overlapper(periode1: LukketPeriode, periode2: LukketPeriode) =
            (periode1.fom == periode2.fom || periode1.fom.isBefore(periode2.fom)) &&
                    (periode1.tom == periode2.tom || periode1.tom.isAfter(periode2.tom))



}

private fun LukketPeriode.antallVirkedager(): Long {
    var nåværende = fom
    var antall = 0L
    while (!nåværende.isAfter(tom)) {
        if (!(nåværende.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))) {
            antall++
        }
        nåværende = nåværende.plusDays(1)
    }
    return antall
}

private fun ArbeidInfo.fraværIPerioden(periode:LukketPeriode): Duration {
    val jobberNormaltPerVirkedag = jobberNormalt.dividedBy(5)
    val kunneJobbetIPerioden = jobberNormaltPerVirkedag.multipliedBy(periode.antallVirkedager())
    val millis = skalJobbe.longValueExact() * kunneJobbetIPerioden.toMillis() / 100
    return if (millis < 0) Duration.ZERO else Duration.ofMillis(millis)
}

data class Grader(
        val grad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Prosent>
)