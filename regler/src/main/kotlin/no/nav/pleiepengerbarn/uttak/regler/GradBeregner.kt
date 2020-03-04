package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration

private val HUNDRE_PROSENT = Prosent.valueOf(100).setScale(2, RoundingMode.HALF_UP)


/**
 * https://confluence.adeo.no/display/SIF/Beskrivelse+av+uttakskomponenten
 */
internal object GradBeregner {

    private val EnVirkedag = Duration.ofHours(7).plusMinutes(30)

    internal fun beregnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): Grader {
        val fraværsGrader = mutableMapOf<Arbeidsforhold, Prosent>()
        var sumAvFraværIPerioden: Duration = Duration.ZERO
        var sumVirketimerIPeriode: Duration = Duration.ZERO


        val antallVirketimerIPerioden = EnVirkedag.multipliedBy(periode.antallVirkedager())
        val takForYtelsePåGrunnAvTilsyn = finnTakForYtelsePåGrunnAvTilsyn(periode, grunnlag)
        grunnlag.arbeid.forEach { arbeidsforholdOgArbeidsperioder ->
            arbeidsforholdOgArbeidsperioder.perioder.entries.firstOrNull {
                it.key.overlapper(periode)
            }?.apply {
                val jobberNormaltPerVirkedag = this.value.jobberNormalt.dividedBy(5)
                val kunneJobbetIPerioden = jobberNormaltPerVirkedag.multipliedBy(periode.antallVirkedager())
                sumVirketimerIPeriode = sumAvFraværIPerioden.plus(kunneJobbetIPerioden)
                val fraværIPerioden = this.value.fraværIPerioden(periode)
                sumAvFraværIPerioden = sumAvFraværIPerioden.plus(fraværIPerioden)
                fraværsGrader[arbeidsforholdOgArbeidsperioder.arbeidsforhold] = BigDecimal(fraværIPerioden.toMillis()).setScale(2, RoundingMode.HALF_UP)/BigDecimal(kunneJobbetIPerioden.toMillis()) * HUNDRE_PROSENT
            }
        }


        val maksimaltAntallVirketimerViKanGiYtelseForIPerioden = Duration.ofMillis((BigDecimal(sumVirketimerIPeriode.toMillis()) * takForYtelsePåGrunnAvTilsyn / HUNDRE_PROSENT).toLong())
        val tilsynsGrad = finnTilsyn(periode, grunnlag)
        val uavkortetGrad = beregnGrad(takForYtelsePåGrunnAvTilsyn, tilsynsGrad, sumAvFraværIPerioden, antallVirketimerIPerioden/*sumVirketimerIPeriode*/, maksimaltAntallVirketimerViKanGiYtelseForIPerioden)
        val avkortetGrad = finnAvkortetGrad(periode, grunnlag, uavkortetGrad)
        val graderingsfaktorPåGrunnAvTilsynIPerioden = finnGraderingsfaktorPåGrunnAvTilsynIPerioden(takForYtelsePåGrunnAvTilsyn, sumAvFraværIPerioden, maksimaltAntallVirketimerViKanGiYtelseForIPerioden, sumVirketimerIPeriode)
        val justeringForAvkortetGrad = if (uavkortetGrad.compareTo(Prosent(0)) == 0) Prosent(0) else avkortetGrad / uavkortetGrad
        return Grader(
                grad = avkortetGrad,
                utbetalingsgrader = fraværsGrader.onEach {
                    fraværsGrader[it.key] = it.value.setScale(2, RoundingMode.HALF_UP) * graderingsfaktorPåGrunnAvTilsynIPerioden.setScale(2, RoundingMode.HALF_UP) / HUNDRE_PROSENT * justeringForAvkortetGrad
                }
        )

    }

    private fun beregnGrad(takForYtelsePåGrunnAvTilsyn:Prosent, tilsynsGradIPerioden:Prosent, sumAvFraværIPerioden:Duration, antallVirketimerIPerioden:Duration, maksimaltAntallVirketimerViKanGiYtelseForIPerioden:Duration):Prosent {
        if (tilsynsGradIPerioden > Prosent(80)) {
            return Prosent(0)
        }
        if (sumAvFraværIPerioden <= antallVirketimerIPerioden) {
            if (sumAvFraværIPerioden <= maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
                return HUNDRE_PROSENT * BigDecimal(sumAvFraværIPerioden.toMillis()) / BigDecimal(antallVirketimerIPerioden.toMillis())
            } else {
                return BigDecimal(maksimaltAntallVirketimerViKanGiYtelseForIPerioden.toMillis()).setScale(2, RoundingMode.HALF_UP)/BigDecimal(antallVirketimerIPerioden.toMillis()).setScale(2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
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

    private fun finnTakForYtelsePåGrunnAvTilsyn(periode:LukketPeriode, grunnlag: RegelGrunnlag):Prosent {
        val tilsyn = finnTilsyn(periode, grunnlag)
        if (tilsyn < Prosent(10)) {
            return Prosent(100).setScale(2, RoundingMode.HALF_EVEN)
        } else {
            if (tilsyn > Prosent(80)) {
                return Prosent.ZERO.setScale(2, RoundingMode.HALF_EVEN)
            }
            return Prosent(100).setScale(2, RoundingMode.HALF_EVEN) - tilsyn
        }
    }


    private fun finnGraderingsfaktorPåGrunnAvTilsynIPerioden(takForYtelsePåGrunnAvTilsyn:Prosent, sumAvFraværIPerioden: Duration, maksimaltAntallVirketimerViKanGiYtelseForIPerioden:Duration, antallVirketimerIPerioden:Duration):Prosent {
        if (takForYtelsePåGrunnAvTilsyn.compareTo(Prosent.ZERO)==0) {
            return Prosent.ZERO
        }
        if (takForYtelsePåGrunnAvTilsyn.compareTo(HUNDRE_PROSENT)==0) {
            return HUNDRE_PROSENT
        }
        if (sumAvFraværIPerioden <= maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
            return HUNDRE_PROSENT
        }
        if (sumAvFraværIPerioden <= antallVirketimerIPerioden) {
            return BigDecimal(maksimaltAntallVirketimerViKanGiYtelseForIPerioden.toMillis()).setScale(2, RoundingMode.HALF_UP) / (BigDecimal(sumAvFraværIPerioden.toMillis())).setScale(2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
        }
        return takForYtelsePåGrunnAvTilsyn
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
            TilsynsbehovStørrelse.PROSENT_100 -> HUNDRE_PROSENT
            TilsynsbehovStørrelse.PROSENT_200 -> Prosent(200)
            else -> Prosent.ZERO
        }.setScale(2, RoundingMode.HALF_EVEN)
    }

    private fun overlapper(periode1: LukketPeriode, periode2: LukketPeriode) =
            (periode1.fom == periode2.fom || periode1.fom.isBefore(periode2.fom)) &&
                    (periode1.tom == periode2.tom || periode1.tom.isAfter(periode2.tom))



}

private fun LukketPeriode.antallVirkedager(): Long {
    var nåværende = fom
    var antall = 0L
    while (!nåværende.isAfter(tom)) {
        if (nåværende.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            antall++
        }
        nåværende = nåværende.plusDays(1)
    }
    return antall
}

private fun ArbeidInfo.fraværIPerioden(periode:LukketPeriode): Duration {
    val jobberNormaltPerVirkedag = jobberNormalt.dividedBy(5)
    val kunneJobbetIPerioden = jobberNormaltPerVirkedag.multipliedBy(periode.antallVirkedager())
    val fraværMillis = (HUNDRE_PROSENT - skalJobbe)/ HUNDRE_PROSENT * BigDecimal(kunneJobbetIPerioden.toMillis())
    return if (fraværMillis < BigDecimal.ZERO) Duration.ZERO else Duration.ofMillis(fraværMillis.longValueExact())
}

data class Grader(
        val grad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Prosent>
)