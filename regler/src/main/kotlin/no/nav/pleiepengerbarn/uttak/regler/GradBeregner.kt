package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.*
import no.nav.pleiepengerbarn.uttak.regler.domene.Desimaltall
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.antallVirkedager
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.antallVirketimer
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.somTekst
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.BorteFraArbeidet
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.FastsettingAvTilsynsgradOgPleiepengegrad
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.GraderesNedForHverTimeBarnetHarTilsynAvAndre
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.InntilEtHundreProsent
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.InntilToHundreProsent
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.MaksÅttiProsentTilsynAvAndre
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.NormalArbeidsdag
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.TilsynPåMindreEnn10ProsentSkalIkkeMedregnes
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.TilsynsordningDelerAvPerioden
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.YtelsenKanGraderesNedTil20Prosent
import java.time.Duration

internal object GradBeregner {
    private const val AntallVirkedagerIUken = 5
    private val TiProsent = Desimaltall.fraDouble(10.00)
    private val TjueProsent = Desimaltall.fraDouble(20.00)
    private val ÅttiProsent = Desimaltall.fraDouble(80.00)

    internal fun beregnGrader(
            periode: LukketPeriode,
            grunnlag: RegelGrunnlag,
            årsakbygger: Årsaksbygger): Grader {
        val fraværsfaktorer = mutableMapOf<ArbeidsforholdReferanse, Desimaltall>()
        var sumAvFraværIPerioden: Duration = Duration.ZERO
        var sumKunneJobbetIPerioden: Duration = Duration.ZERO

        val antallVirkedagerIPerioden = periode.antallVirkedager()
        val antallVirketimerIPerioden = periode.antallVirketimer(antallVirkedagerIPerioden)
        årsakbygger.hjemmel(NormalArbeidsdag.anvend(
                "Fastsatt $antallVirkedagerIPerioden virkedager, som tilsvarer ${antallVirketimerIPerioden.somTekst()}"
        ))

        val tilsynsgrad = grunnlag.finnTilsynsgrad(
                periode = periode,
                antallVirketimerIPerioden = antallVirketimerIPerioden
        )
        val pleiepengegrad = Desimaltall.EtHundre - tilsynsgrad
        årsakbygger.hjemmel(FastsettingAvTilsynsgradOgPleiepengegrad.anvend(
                "Fastsatt tilsynsgrad til ${tilsynsgrad.formatertProsent()}, og pleiepengegrad til ${pleiepengegrad.formatertProsent()}"
        ))

        val takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad(
                tilsynsgrad = tilsynsgrad,
                årsakbygger = årsakbygger
        )

        val maksimaltAntallVirketimerViKanGiYtelseForIPerioden = antallVirketimerIPerioden * takForYtelsePåGrunnAvTilsynsgrad.fraProsentTilFaktor()

        årsakbygger.hjemmel(GraderesNedForHverTimeBarnetHarTilsynAvAndre.anvend( // TODO ?
                "Fastsatt tak for ytelse på grunn av tilsynsgrad til ${takForYtelsePåGrunnAvTilsynsgrad.formatertProsent()} " +
                        "og maksimalt antall virketimer vi kan gi ytelse for til ${maksimaltAntallVirketimerViKanGiYtelseForIPerioden.somTekst()}"
        ))

        grunnlag.arbeid.forEach { (arbeidsforholdRef, perioderMedArbeid) ->
            perioderMedArbeid.entries.firstOrNull {
                it.key.overlapper(periode)
            }?.apply {
                val jobberISnittPerVirkedag = this.value.jobberNormaltPerUke / AntallVirkedagerIUken
                val kunneJobbetIPerioden = jobberISnittPerVirkedag * antallVirkedagerIPerioden

                sumKunneJobbetIPerioden = sumKunneJobbetIPerioden.plus(kunneJobbetIPerioden)

                val fraværIPerioden = this.value.fravær(
                        kunneJobbetIPerioden = kunneJobbetIPerioden
                )

                sumAvFraværIPerioden = sumAvFraværIPerioden.plus(fraværIPerioden)

                fraværsfaktorer[arbeidsforholdRef] = fraværIPerioden / kunneJobbetIPerioden
            }
        }

        årsakbygger.hjemmel(BorteFraArbeidet.anvend(
                "Fastsatt fravær til ${sumAvFraværIPerioden.somTekst()} av normalt ${sumKunneJobbetIPerioden.somTekst()}"
        ))

        val beregnetGrad = beregnGrad(
                takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad,
                tilsynsgrad = tilsynsgrad,
                sumAvFraværIPerioden = sumAvFraværIPerioden,
                antallVirketimerIPerioden = antallVirketimerIPerioden,
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden = maksimaltAntallVirketimerViKanGiYtelseForIPerioden
        )

        val avkortetMotTilsynsordningOgAndreOmsorgspersoner = grunnlag.avkortMotTilsynsgradOgAndreOmsorgspersoner(
                periode = periode,
                beregnetGrad = beregnetGrad,
                tilsynsgrad = tilsynsgrad,
                årsakbygger = årsakbygger
        )

        val endeligGrad = fastsettEndeligGrad(
                avkortetMotTilsynsordningOgAndreOmsorgspersoner = avkortetMotTilsynsordningOgAndreOmsorgspersoner,
                årsakbygger = årsakbygger
        )

        årsakbygger.avgjørÅrsak(
                beregnetGrad = beregnetGrad,
                endeligGrad = endeligGrad,
                takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad
        )

        val graderingsfaktorPåGrunnAvTilsynIPerioden = finnGraderingsfaktorPåGrunnAvTilsynIPerioden(
                takForYtelsePåGrunnAvTilsynsgrad = takForYtelsePåGrunnAvTilsynsgrad,
                sumAvFraværIPerioden = sumAvFraværIPerioden,
                maksimaltAntallVirketimerViKanGiYtelseForIPerioden = maksimaltAntallVirketimerViKanGiYtelseForIPerioden,
                antallVirketimerIPerioden = sumKunneJobbetIPerioden
        )

        val justeringsFaktor = endeligGrad / beregnetGrad

        return Grader(
                grad = endeligGrad.resultat,
                utbetalingsgrader = fraværsfaktorer.mapValues { (_, fraværsfaktor) ->
                    fraværsfaktor
                            .times(graderingsfaktorPåGrunnAvTilsynIPerioden)
                            .times(justeringsFaktor)
                            .fraFaktorTilProsent()
                            .normaliserProsent()
                            .resultat
                }.map {(referanse,utbetalingsgrad ) -> Utbetalingsgrader(
                        arbeidsforhold = referanse,
                        utbetalingsgrad = utbetalingsgrad
                )},
                årsak = årsakbygger.byggOgTillattKunEn()
        )
    }

    private fun fastsettEndeligGrad(
            avkortetMotTilsynsordningOgAndreOmsorgspersoner: Desimaltall,
            årsakbygger: Årsaksbygger): Desimaltall {
        return if (avkortetMotTilsynsordningOgAndreOmsorgspersoner < TjueProsent) {
            årsakbygger.hjemmel(YtelsenKanGraderesNedTil20Prosent.anvend( // TODO Dette er mot tilsyn. Avkortet mot inntekt erm 9-15 jf. 8-13 første ledd andre punktum
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
            beregnetGrad: Desimaltall,
            tilsynsgrad: Desimaltall,
            årsakbygger: Årsaksbygger
    ) : Desimaltall {
        val tilsynsbehov = finnTilsynsbehov(periode)
        val tilsynsbehovDekketAvAndreParter = finnTilsynsbehovDekketAvAndreParter(periode)
        val tilgjengeligGrad = tilsynsbehov - tilsynsbehovDekketAvAndreParter - tilsynsgrad.tilsynsgradTilAvkorting()
        val avkortet = if (tilgjengeligGrad < Desimaltall.Null) {
            return Desimaltall.Null
        } else if (beregnetGrad >= tilgjengeligGrad) {
            tilgjengeligGrad
        } else {
            beregnetGrad
        }.normaliserProsent()

        val tilgjengeligGradLovhenvisning = when {
            tilsynsbehov.erEtHundre() -> {
                årsakbygger.hjemmel(InntilEtHundreProsent.anvend(
                        "Fastsatt at barnet har behov for opp til 100% tilsyn i perioden."
                ))
                InntilEtHundreProsent
            }
            else -> {
                årsakbygger.hjemmel(InntilToHundreProsent.anvend(
                        "Fastsatt at barnet har behov for opp til 200% tilsyn i perioden."
                ))
                InntilToHundreProsent
            }
        }

        årsakbygger.hjemmel(TilsynsordningDelerAvPerioden.anvend(
                "Fastsatt at ${tilsynsgrad.formatertProsent()} av barnets behov for tilsyn dekkes i etablert tilsynsordning."
        ))

        årsakbygger.hjemmel(tilgjengeligGradLovhenvisning.anvend(
                "Fastsatt at ${tilsynsbehovDekketAvAndreParter.formatertProsent()} av barnets behov for tilsyn dekkes av andre omsorgspersoner. " +
                        "Tilsynsbehovet som ikke er dekket av andre er fastsatt til ${tilgjengeligGrad.formatertProsent()}"
        ))

        return avkortet
    }

    private fun Desimaltall.tilsynsgradTilAvkorting() = if (this < TiProsent) {
        Desimaltall.Null
    } else {
        this
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
            årsakbygger.hjemmel(TilsynPåMindreEnn10ProsentSkalIkkeMedregnes.anvend(
                    "Beregnet tilsynsgrad på ${tilsynsgrad.formatertProsent()} regnes ikke med da den er under 10%"
            ))
            Desimaltall.EtHundre
        } else {
            if (tilsynsgrad > ÅttiProsent) {
                årsakbygger.hjemmel(MaksÅttiProsentTilsynAvAndre.anvend(
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


    private fun RegelGrunnlag.finnTilsynsgrad(
            periode: LukketPeriode,
            antallVirketimerIPerioden: Duration) : Desimaltall {
        val tilsyn = tilsynsperioder.entries.find { it.key.overlapper(periode)}
        return tilsyn?.value?.lengde?.div(antallVirketimerIPerioden)?.fraFaktorTilProsent()?.normaliserProsent()?: Desimaltall.Null
    }

    private fun RegelGrunnlag.finnTilsynsbehov(periode: LukketPeriode): Desimaltall {
        val tilsynsbehov = tilsynsbehov.entries.find { it.key.overlapper(periode) }
        return tilsynsbehov?.value?.prosent?.prosent?.somDesimaltall() ?: throw IllegalStateException("Periode uten tilsynsbehov")
    }

    private fun ArbeidsforholdPeriodeInfo.fravær(
            kunneJobbetIPerioden: Duration) : Duration {
        val fraværsfaktor = Desimaltall
                .EtHundre
                .minus(skalJobbeProsent.somDesimaltall())
                .fraProsentTilFaktor()
                .normaliserFaktor()

        return Desimaltall
                .fraDuration(kunneJobbetIPerioden)
                .times(fraværsfaktor)
                .tilDuration()
    }
}



private const val BeregningAvGrader = "BeregningAvGrader"
internal fun Årsaksbygger.startBeregningAvGraderMed(hjemler: Set<Hjemmel>) = hjemler(BeregningAvGrader, hjemler)
private fun Årsaksbygger.hjemmel(hjemmel: Hjemmel) = hjemmel(BeregningAvGrader, hjemmel)
private fun Årsaksbygger.byggOgTillattKunEn(): Årsak {
    val årsaker = bygg()
    if (årsaker.size != 1) throw IllegalStateException("Forventer bare en årsak etter beregning av grader. Fikk ${årsaker.size}")
    return årsaker.first()
}
private fun Årsaksbygger.avgjørÅrsak(
        beregnetGrad: Desimaltall,
        endeligGrad: Desimaltall,
        takForYtelsePåGrunnAvTilsynsgrad: Desimaltall) {
    when {
        takForYtelsePåGrunnAvTilsynsgrad.erNull() -> {
            avslått(BeregningAvGrader, AvslåttÅrsaker.FOR_HØY_TILSYNSGRAD)
        }
        endeligGrad.erNull() -> {
            avslått(BeregningAvGrader, AvslåttÅrsaker.FOR_LAV_GRAD)
        }
        endeligGrad.erEtHundre() -> {
            innvilget(BeregningAvGrader, InnvilgetÅrsaker.FULL_DEKNING)
        }
        else -> {
            if (beregnetGrad < takForYtelsePåGrunnAvTilsynsgrad) {
                innvilget(BeregningAvGrader, InnvilgetÅrsaker.AVKORTET_MOT_INNTEKT)
            } else {
                innvilget(BeregningAvGrader, InnvilgetÅrsaker.GRADERT_MOT_TILSYN)

            }
        }
    }
}

data class Grader(
        val grad: Prosent,
        val utbetalingsgrader: List<Utbetalingsgrader>,
        val årsak: Årsak
)