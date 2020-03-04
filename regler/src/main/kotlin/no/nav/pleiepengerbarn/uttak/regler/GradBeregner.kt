package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.*
import no.nav.pleiepengerbarn.uttak.regler.domene.Desimaltall
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import java.time.DayOfWeek
import java.time.Duration

/**
 * https://confluence.adeo.no/display/SIF/Beskrivelse+av+uttakskomponenten
 */

internal object GradBeregner {

    private const val AntallVirkedagerIUken = 5
    private val EnVirkedag = Duration.ofHours(7).plusMinutes(30)
    private val TiProsent = Desimaltall.fraDouble(10.00)
    private val ÅttiProsent = Desimaltall.fraDouble(80.00)
    private val ToHundreProsent = Desimaltall.fraDouble(200.00)

    internal fun beregnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): Grader {
        val fraværsGrader = mutableMapOf<Arbeidsforhold, Desimaltall>()
        var sumAvFraværIPerioden: Duration = Duration.ZERO
        var sumVirketimerIPerioden: Duration = Duration.ZERO
        val antallVirketimerIPerioden = EnVirkedag.multipliedBy(periode.antallVirkedager())

        val tilsynsordninggrad = grunnlag.finnTilsynsordningsgrad(periode)
        val takForYtelsePåGrunnAvTilsyn = finnTakForYtelsePåGrunnAvTilsyn(tilsynsordninggrad)

        grunnlag.arbeid.forEach { arbeidsforholdOgArbeidsperioder ->
            arbeidsforholdOgArbeidsperioder.perioder.entries.firstOrNull {
                it.key.overlapper(periode)
            }?.apply {
                val jobberISnittPerVirkedag = this.value.jobberNormalt / AntallVirkedagerIUken
                val kunneJobbetIPerioden = jobberISnittPerVirkedag * periode.antallVirkedager()

                sumVirketimerIPerioden = sumVirketimerIPerioden.plus(kunneJobbetIPerioden)

                val fraværIPerioden = this.value.fravær(
                        kunneJobbetIPerioden = kunneJobbetIPerioden
                )

                sumAvFraværIPerioden = sumAvFraværIPerioden.plus(fraværIPerioden)

                fraværsGrader[arbeidsforholdOgArbeidsperioder.arbeidsforhold] = fraværIPerioden / kunneJobbetIPerioden
            }
        }

        val maksimaltAntallVirketimerViKanGiYtelseForIPerioden = sumVirketimerIPerioden * takForYtelsePåGrunnAvTilsyn.fraProsentTilFaktor()

        val uavkortetGrad = beregnUavkortetGrad(
                takForYtelsePåGrunnAvTilsyn = takForYtelsePåGrunnAvTilsyn,
                tilsynsordninggrad = tilsynsordninggrad,
                sumAvFraværIPerioden = sumAvFraværIPerioden,
                antallVirketimerIPerioden = antallVirketimerIPerioden,
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden = maksimaltAntallVirketimerViKanGiYtelseForIPerioden
        )
        val gradetMotTilsyn = grunnlag.gradertMotTilsyn(
                periode = periode,
                uavkortetGrad = uavkortetGrad,
                tilsynsordninggrad = tilsynsordninggrad
        )

        val graderingsfaktorPåGrunnAvTilsynIPerioden = finnGraderingsfaktorPåGrunnAvTilsynIPerioden(
                takForYtelsePåGrunnAvTilsyn = takForYtelsePåGrunnAvTilsyn,
                sumAvFraværIPerioden = sumAvFraværIPerioden,
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden = maksimaltAntallVirketimerViKanGiYtelseForIPerioden,
                antallVirketimerIPerioden = sumVirketimerIPerioden
        )

        val justeringsFaktor = if (uavkortetGrad.erNull()) Desimaltall.Null else gradetMotTilsyn / uavkortetGrad

        return Grader(
                grad = gradetMotTilsyn.resultat,
                utbetalingsgrader = fraværsGrader.mapValues { (_, fraværsgrad) ->
                    fraværsgrad
                            .times(graderingsfaktorPåGrunnAvTilsynIPerioden)
                            .times(justeringsFaktor)
                            .fraFaktorTilProsent()
                            .resultat
                }
        )

    }

    private fun beregnUavkortetGrad(
            takForYtelsePåGrunnAvTilsyn: Desimaltall,
            tilsynsordninggrad: Desimaltall,
            sumAvFraværIPerioden: Duration,
            antallVirketimerIPerioden: Duration,
            maksimaltAntallVirketimerViKanGiYtelseForIPerioden : Duration
    ) : Desimaltall {
        if (tilsynsordninggrad > ÅttiProsent) {
            return Desimaltall.Null
        }
        return if (sumAvFraværIPerioden <= antallVirketimerIPerioden) {
            if (sumAvFraværIPerioden <= maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
                sumAvFraværIPerioden
                        .div(antallVirketimerIPerioden)
                        .fraFaktorTilProsent()
            } else {
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden
                        .div(antallVirketimerIPerioden)
                        .fraFaktorTilProsent()
            }
        } else {
            takForYtelsePåGrunnAvTilsyn
        }
    }


    private fun RegelGrunnlag.gradertMotTilsyn(
            periode: LukketPeriode,
            uavkortetGrad: Desimaltall,
            tilsynsordninggrad: Desimaltall
    ) : Desimaltall {
        val tilsynsbehov = finnTilsynsbehov(periode)
        val tilsynsbehovDekketAvAndreParter = finnTilsynsbehovDekketAvAndreParter(periode)
        val tilgjengeligGrad = tilsynsbehov - tilsynsbehovDekketAvAndreParter - tilsynsordninggrad
        if (tilgjengeligGrad < Desimaltall.Null) {
            return Desimaltall.Null
        }
        return if (uavkortetGrad >= tilgjengeligGrad) {
            tilgjengeligGrad
        } else {
            uavkortetGrad
        }.min(Desimaltall.Null).maks(Desimaltall.EtHundre)
    }

    private fun RegelGrunnlag.finnTilsynsbehovDekketAvAndreParter(periode: LukketPeriode) : Desimaltall {
        var sumAndreParter = Desimaltall.Null
        andrePartersUttaksplan.forEach {uttaksplan ->

            val annenPartsPeriode = uttaksplan.perioder
                    .filter { it.key.overlapper(periode) }
                    .filter { it.value is InnvilgetPeriode }
                    .values.firstOrNull() as InnvilgetPeriode?

            if (annenPartsPeriode != null) {
                sumAndreParter += annenPartsPeriode.grad.somDesimaltall()
            }
        }
        return sumAndreParter
    }


    private fun finnTakForYtelsePåGrunnAvTilsyn(tilsynsordninggrad: Desimaltall) : Desimaltall {
        return if (tilsynsordninggrad < TiProsent) {
            Desimaltall.EtHundre
        } else {
            if (tilsynsordninggrad > ÅttiProsent) {
                Desimaltall.Null
            } else  {
                Desimaltall.EtHundre - tilsynsordninggrad
            }
        }
    }

    private fun finnGraderingsfaktorPåGrunnAvTilsynIPerioden(
            takForYtelsePåGrunnAvTilsyn: Desimaltall,
            sumAvFraværIPerioden: Duration,
            maksimaltAntallVirketimerViKanGiYtelseForIPerioden: Duration,
            antallVirketimerIPerioden:Duration): Desimaltall {

        if (takForYtelsePåGrunnAvTilsyn.erNull()) {
            return Desimaltall.Null
        }
        if (takForYtelsePåGrunnAvTilsyn.erEtHundre()) {
            return Desimaltall.En
        }
        if (sumAvFraværIPerioden <= maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
            return Desimaltall.En
        }
        if (sumAvFraværIPerioden <= antallVirketimerIPerioden) {
            return maksimaltAntallVirketimerViKanGiYtelseForIPerioden.div(sumAvFraværIPerioden)
        }
        return takForYtelsePåGrunnAvTilsyn.fraProsentTilFaktor()
    }


    private fun RegelGrunnlag.finnTilsynsordningsgrad(periode: LukketPeriode) : Desimaltall {
        val tilsyn = tilsynsperioder.entries.find { it.key.overlapper(periode)}
        return tilsyn?.value?.grad?.somDesimaltall()?.maks(Desimaltall.EtHundre)?.min(Desimaltall.Null) ?: Desimaltall.Null
    }

    private fun RegelGrunnlag.finnTilsynsbehov(periode: LukketPeriode): Desimaltall {
        val tilsynsbehov = tilsynsbehov.entries.find { it.key.overlapper(periode) }
        return when (tilsynsbehov?.value?.prosent) {
            TilsynsbehovStørrelse.PROSENT_100 -> Desimaltall.EtHundre
            TilsynsbehovStørrelse.PROSENT_200 -> ToHundreProsent
            else -> Desimaltall.Null
        }
    }
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

private fun ArbeidInfo.fravær(
        kunneJobbetIPerioden: Duration) : Duration {
    val fraværsfaktor = Desimaltall
            .EtHundre
            .minus(skalJobbe.somDesimaltall())
            .maks(Desimaltall.EtHundre)
            .min(Desimaltall.Null)
            .fraProsentTilFaktor()

    return Desimaltall
            .fraDuration(kunneJobbetIPerioden)
            .times(fraværsfaktor)
            .tilDuration()
}

data class Grader(
        val grad: Prosent,
        val utbetalingsgrader: Map<Arbeidsforhold, Prosent>
)