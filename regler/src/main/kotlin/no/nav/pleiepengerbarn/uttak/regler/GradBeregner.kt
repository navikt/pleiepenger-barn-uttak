package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.*
import no.nav.pleiepengerbarn.uttak.regler.domene.Desimaltall
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.antallVirkedager
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.somTekst
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.BorteFraArbeidet
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.FastsettingAvTilsynsgradOgPleiepengegrad
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.GraderesNedForHverTimeBarnetHarTilsynAvAndre
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.InntilToOmsorgspersoner
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.NormalArbeidsdag
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.YtelsenKanGraderesNedTil20Prosent
import java.time.Duration

internal object GradBeregner {
    private const val AntallVirkedagerIUken = 5
    private val EnVirkedag = Duration.ofHours(7).plusMinutes(30)
    private val TiProsent = Desimaltall.fraDouble(10.00)
    private val TjueProsent = Desimaltall.fraDouble(20.00)
    private val ÅttiProsent = Desimaltall.fraDouble(80.00)
    private val ToHundreProsent = Desimaltall.fraDouble(200.00)

    internal fun beregnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): Grader {
        val årsakbygger = Årsaksbygger()
        val fraværsfaktorer = mutableMapOf<ArbeidsforholdRef, Desimaltall>()
        var sumAvFraværIPerioden: Duration = Duration.ZERO
        var sumKunneJobbetIPerioden: Duration = Duration.ZERO

        val antallVirkedagerIPerioden = periode.antallVirkedager()
        val antallVirketimerIPerioden = EnVirkedag.multipliedBy(antallVirkedagerIPerioden)
        årsakbygger.hjemmel(NormalArbeidsdag.anvend(
                "Fastsatt $antallVirkedagerIPerioden virkedager, som tilsvarer ${antallVirketimerIPerioden.somTekst()}"
        ))

        val tilsynsgrad = grunnlag.finnTilsynsgrad(periode)
        val pleiepengegrad = Desimaltall.EtHundre - tilsynsgrad
        årsakbygger.hjemmel(FastsettingAvTilsynsgradOgPleiepengegrad.anvend(
                "Fastsatt tilsynsgrad til ${tilsynsgrad.formatertProsent()}, og pleiepengegrad til ${pleiepengegrad.formatertProsent()}"
        ))

        val takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad(
                tilsynsgrad = tilsynsgrad,
                årsakbygger = årsakbygger
        )

        val maksimaltAntallVirketimerViKanGiYtelseForIPerioden = antallVirketimerIPerioden * takForYtelsePåGrunnAvTilsynsgrad.fraProsentTilFaktor()

        årsakbygger.hjemmel(GraderesNedForHverTimeBarnetHarTilsynAvAndre.anvend(
                "Fastsatt tak for ytelse på grunn av tilsynsgrad til ${takForYtelsePåGrunnAvTilsynsgrad.formatertProsent()} " +
                        "og maksimalt antall virketimer vi kan gi ytelse for til ${maksimaltAntallVirketimerViKanGiYtelseForIPerioden.somTekst()}"
        ))

        grunnlag.arbeid.forEach { (arbeidsforholdRef, perioderMedArbeid) ->
            perioderMedArbeid.entries.firstOrNull {
                it.key.overlapper(periode)
            }?.apply {
                val jobberISnittPerVirkedag = this.value.jobberNormalt / AntallVirkedagerIUken
                val kunneJobbetIPerioden = jobberISnittPerVirkedag * antallVirkedagerIPerioden

                sumKunneJobbetIPerioden = sumKunneJobbetIPerioden.plus(kunneJobbetIPerioden)

                val fraværIPerioden = this.value.fravær(
                        kunneJobbetIPerioden = kunneJobbetIPerioden
                )

                sumAvFraværIPerioden = sumAvFraværIPerioden.plus(fraværIPerioden)

                fraværsFaktorer[arbeidsforholdRef] = fraværIPerioden / kunneJobbetIPerioden
            }
        }

        årsakbygger.hjemmel(BorteFraArbeidet.anvend(
                "Fastsatt fravær til ${sumAvFraværIPerioden.somTekst()} av normalt ${sumKunneJobbetIPerioden.somTekst()}"
        ))

        val grad = beregnGrad(
                takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad,
                tilsynsgrad = tilsynsgrad,
                sumAvFraværIPerioden = sumAvFraværIPerioden,
                antallVirketimerIPerioden = antallVirketimerIPerioden,
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden = maksimaltAntallVirketimerViKanGiYtelseForIPerioden
        )

        val avkortetMotTilsynsordningOgAndreOmsorgspersoner = grunnlag.avkortMotTilsynsgradOgAndreOmsorgspersoner(
                periode = periode,
                grad = grad,
                tilsynsgrad = tilsynsgrad,
                årsakbygger = årsakbygger
        )

        val endeligGrad = fastsettEndeligGrad(
                avkortetMotTilsynsordningOgAndreOmsorgspersoner = avkortetMotTilsynsordningOgAndreOmsorgspersoner,
                årsakbygger = årsakbygger
        )

        årsakbygger.avgjørÅrsak(
                endeligGrad = endeligGrad,
                takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad
        )

        val graderingsfaktorPåGrunnAvTilsynIPerioden = finnGraderingsfaktorPåGrunnAvTilsynIPerioden(
                takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad,
                sumAvFraværIPerioden = sumAvFraværIPerioden,
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden = maksimaltAntallVirketimerViKanGiYtelseForIPerioden,
                antallVirketimerIPerioden = sumKunneJobbetIPerioden
        )

        val justeringsFaktor = endeligGrad / grad

        return Grader(
                grad = endeligGrad.resultat,
                utbetalingsgrader = fraværsfaktorer.mapValues { (_, fraværsfaktor) ->
                    fraværsfaktor
                            .times(graderingsfaktorPåGrunnAvTilsynIPerioden)
                            .times(justeringsFaktor)
                            .fraFaktorTilProsent()
                            .normaliserProsent()
                            .resultat
                },
                årsak = årsakbygger.byggOgTillattKunEn()
        )

    }

    private fun fastsettEndeligGrad(
            avkortetMotTilsynsordningOgAndreOmsorgspersoner: Desimaltall,
            årsakbygger: Årsaksbygger): Desimaltall {
        return if (avkortetMotTilsynsordningOgAndreOmsorgspersoner < TjueProsent) {
            årsakbygger.hjemmel(YtelsenKanGraderesNedTil20Prosent.anvend(
                    "Ikke rett ytelsen da ${avkortetMotTilsynsordningOgAndreOmsorgspersoner.formatertProsent()} er under 20%"
            ))
            Desimaltall.Null
        } else {
            avkortetMotTilsynsordningOgAndreOmsorgspersoner
        }
    }

    private fun beregnGrad(
            takForYtelsePåGrunnAvTilsynsgrad: Desimaltall,
            tilsynsgrad: Desimaltall,
            sumAvFraværIPerioden: Duration,
            antallVirketimerIPerioden: Duration,
            maksimaltAntallVirketimerViKanGiYtelseForIPerioden : Duration
    ) : Desimaltall {
        if (tilsynsgrad > ÅttiProsent) {
            return Desimaltall.Null
        }
        return if (sumAvFraværIPerioden <= antallVirketimerIPerioden) {
            if (sumAvFraværIPerioden <= maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
                sumAvFraværIPerioden
                        .div(antallVirketimerIPerioden)
                        .fraFaktorTilProsent()
                        .normaliserProsent()
            } else {
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden
                        .div(antallVirketimerIPerioden)
                        .fraFaktorTilProsent()
                        .normaliserProsent()
            }
        } else {
            takForYtelsePåGrunnAvTilsynsgrad
        }

        // TODO: Grad før gradering mot tilsynsgrad og andre omsorgspersoner i årsaksbygger.
    }


    private fun RegelGrunnlag.avkortMotTilsynsgradOgAndreOmsorgspersoner(
            periode: LukketPeriode,
            grad: Desimaltall,
            tilsynsgrad: Desimaltall,
            årsakbygger: Årsaksbygger
    ) : Desimaltall {
        val tilsynsbehov = finnTilsynsbehov(periode)
        val tilsynsbehovDekketAvAndreParter = finnTilsynsbehovDekketAvAndreParter(periode)
        val tilgjengeligGrad = tilsynsbehov - tilsynsbehovDekketAvAndreParter - tilsynsgrad
        val avkortet = if (tilgjengeligGrad < Desimaltall.Null) {
            return Desimaltall.Null
        } else if (grad >= tilgjengeligGrad) {
            tilgjengeligGrad
        } else {
            grad
        }.normaliserProsent()

        årsakbygger.hjemmel(InntilToOmsorgspersoner.anvend(
                "Fastsatt tilsynsbehov til ${tilsynsbehov.formatertProsent()} hvor ${tilsynsbehovDekketAvAndreParter.formatertProsent()} " +
                        "er dekket av andre omsorgspersoner og ${tilsynsgrad.formatertProsent()} i tilsynsordninger. " +
                        "Tilgjengelig ${avkortet.formatertProsent()}"
        ))

        return avkortet
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


    private fun takForYtelsePåGrunnAvTilsynsgrad(
            tilsynsgrad: Desimaltall,
            årsakbygger: Årsaksbygger) : Desimaltall {
        return if (tilsynsgrad < TiProsent) {
            årsakbygger.hjemmel(Lovhenvisninger.TilsynPåMindreEnn10ProsentSkalIkkeMedregnes.anvend(
                    "Beregnet tilsynsgrad på ${tilsynsgrad.formatertProsent()} regnes ikke med da den er under 10%"
            ))
            Desimaltall.EtHundre
        } else {
            if (tilsynsgrad > ÅttiProsent) {
                årsakbygger.hjemmel(Lovhenvisninger.MaksÅttiProsentTilsynAvAndre.anvend(
                        "Beregnet tilsynsgrad på ${tilsynsgrad.formatertProsent()} gjør at det ikke foreligger rett til pleiepenger."
                ))
                Desimaltall.Null
            } else  {
                Desimaltall.EtHundre
                        .minus(tilsynsgrad)
                        .normaliserProsent()
            }
        }
    }

    private fun finnGraderingsfaktorPåGrunnAvTilsynIPerioden(
            takForYtelsePåGrunnAvTilsynsgrad: Desimaltall,
            sumAvFraværIPerioden: Duration,
            maksimaltAntallVirketimerViKanGiYtelseForIPerioden: Duration,
            antallVirketimerIPerioden:Duration): Desimaltall {

        if (takForYtelsePåGrunnAvTilsynsgrad.erNull()) {
            return Desimaltall.Null
        }
        if (takForYtelsePåGrunnAvTilsynsgrad.erEtHundre()) {
            return Desimaltall.En
        }
        if (sumAvFraværIPerioden <= maksimaltAntallVirketimerViKanGiYtelseForIPerioden) {
            return Desimaltall.En
        }
        if (sumAvFraværIPerioden <= antallVirketimerIPerioden) {
            return maksimaltAntallVirketimerViKanGiYtelseForIPerioden
                    .div(sumAvFraværIPerioden)
                    .normaliserFaktor()
        }
        return takForYtelsePåGrunnAvTilsynsgrad.fraProsentTilFaktor()
    }


    private fun RegelGrunnlag.finnTilsynsgrad(periode: LukketPeriode) : Desimaltall {
        val tilsyn = tilsynsperioder.entries.find { it.key.overlapper(periode)}
        return tilsyn?.value?.grad?.somDesimaltall()?.normaliserProsent() ?: Desimaltall.Null
    }

    private fun RegelGrunnlag.finnTilsynsbehov(periode: LukketPeriode): Desimaltall {
        val tilsynsbehov = tilsynsbehov.entries.find { it.key.overlapper(periode) }
        return when (tilsynsbehov?.value?.prosent) {
            TilsynsbehovStørrelse.PROSENT_100 -> Desimaltall.EtHundre
            TilsynsbehovStørrelse.PROSENT_200 -> ToHundreProsent
            else -> Desimaltall.Null
        }
    }

    private fun ArbeidInfo.fravær(
            kunneJobbetIPerioden: Duration) : Duration {
        val fraværsfaktor = Desimaltall
                .EtHundre
                .minus(skalJobbe.somDesimaltall())
                .fraProsentTilFaktor()
                .normaliserFaktor()

        return Desimaltall
                .fraDuration(kunneJobbetIPerioden)
                .times(fraværsfaktor)
                .tilDuration()
    }
}



private const val BeregningAvGrader = "BeregningAvGrader"
private fun Årsaksbygger.hjemmel(hjemmel: Hjemmel) = hjemmel(BeregningAvGrader, hjemmel)
private fun Årsaksbygger.byggOgTillattKunEn(): Årsak {
    val årsaker = bygg()
    if (årsaker.size != 1) throw IllegalStateException("Forventer bare en årsak etter beregning av grader. Fikk ${årsaker.size}")
    return årsaker.first()
}
private fun Årsaksbygger.avgjørÅrsak(
        endeligGrad: Desimaltall,
        takForYtelsePåGrunnAvTilsynsgrad: Desimaltall) {
    when {
        takForYtelsePåGrunnAvTilsynsgrad.erNull() -> {
            avslått(BeregningAvGrader, AvslåttÅrsaker.ForHøyTilsynsgrad)
        }
        takForYtelsePåGrunnAvTilsynsgrad.erEtHundre() -> {
            innvilget(BeregningAvGrader, InnvilgetÅrsaker.AvkortetMotInntekt)
        }
        endeligGrad.erNull() -> {
            avslått(BeregningAvGrader, AvslåttÅrsaker.ForLavGrad)
        }
        else -> {
            innvilget(BeregningAvGrader, InnvilgetÅrsaker.GradertMotTilsyn)
        }
    }
}

data class Grader(
        val grad: Prosent,
        val utbetalingsgrader: Map<ArbeidsforholdRef, Prosent>,
        val årsak: Årsak
)