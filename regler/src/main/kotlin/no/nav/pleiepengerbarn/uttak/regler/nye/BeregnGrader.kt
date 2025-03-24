package no.nav.pleiepengerbarn.uttak.regler.nye

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.*
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate

internal object BeregnGrader : IBeregnGrader {

    override fun beregn(beregnGraderGrunnlag: BeregnGraderGrunnlag): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(beregnGraderGrunnlag.etablertTilsyn)
        val søkersTapteArbeidstid = beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid()
        val uttaksgradResultat = avklarUttaksgrad(
            beregnGraderGrunnlag,
            etablertTilsynsprosent,
            finnØnsketUttaksgradProsent(beregnGraderGrunnlag.oppgittTilsyn)
        )
        val utbetalingsgrader = BeregnUtbetalingsgrader.beregn(
            uttaksgradResultat.uttaksgrad,
            uttaksgradResultat.overstyrtUttaksgrad,
            uttaksgradResultat.oppfyltÅrsak == Årsak.GRADERT_MOT_TILSYN,
            beregnGraderGrunnlag
        )
        val uttaksgradMedReduksjonGrunnetInntektsgradering =
            getUttaksgradJustertMotInntektsgradering(beregnGraderGrunnlag, uttaksgradResultat)
        val faktiskUttaksgrad = uttaksgradResultat.overstyrtUttaksgrad ?: uttaksgradMedReduksjonGrunnetInntektsgradering
        ?: uttaksgradResultat.uttaksgrad


        val årsak = finnFaktiskÅrsak(
            beregnGraderGrunnlag,
            uttaksgradResultat
        )

        return GraderBeregnet(
            pleiebehov = beregnGraderGrunnlag.pleiebehov,
            graderingMotTilsyn = GraderingMotTilsyn(
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = beregnGraderGrunnlag.andreSøkeresTilsyn,
                andreSøkeresTilsynReberegnet = beregnGraderGrunnlag.andreSøkeresTilsynReberegnet,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker,
                overseEtablertTilsynÅrsak = uttaksgradResultat.overseEtablertTilsynÅrsak
            ),
            søkersTapteArbeidstid = søkersTapteArbeidstid,
            oppgittTilsyn = beregnGraderGrunnlag.oppgittTilsyn,
            uttaksgrad = faktiskUttaksgrad.setScale(0, RoundingMode.HALF_UP),
            uttaksgradMedReduksjonGrunnetInntektsgradering = uttaksgradMedReduksjonGrunnetInntektsgradering,
            uttaksgradUtenReduksjonGrunnetInntektsgradering = uttaksgradResultat.uttaksgrad,
            utbetalingsgrader = utbetalingsgrader,
            årsak = årsak,
            manueltOverstyrt = uttaksgradResultat.overstyrtUttaksgrad != null || utbetalingsgrader.any { it.value.overstyrt == true }
        )
    }

    private fun finnFaktiskÅrsak(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        uttaksgradResultat: UttaksgradResultat
    ): Årsak {
        if (uttaksgradResultat.overstyrtUttaksgrad != null
        ) {
            if (uttaksgradResultat.overstyrtUttaksgrad.setScale(2, RoundingMode.HALF_UP).compareTo(TJUE_PROSENT) < 0) {
                return Årsak.OVERSTYRT_UTTAK_AVSLAG;
            }
            return Årsak.OVERSTYRT_UTTAKSGRAD;
        }

        if (skalNedjustereGrunnetInntekt(
                beregnGraderGrunnlag,
                uttaksgradResultat
            ) && uttaksgradResultat.uttaksgrad.setScale(2, RoundingMode.HALF_UP).compareTo(TJUE_PROSENT) >= 0
        ) {
            return Årsak.AVKORTET_MOT_INNTEKT;
        }

        return uttaksgradResultat.årsak();
    }

    private fun getUttaksgradJustertMotInntektsgradering(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        uttaksgradResultat: UttaksgradResultat
    ): Prosent? =
        if (beregnGraderGrunnlag.inntektsgradering != null && skalNedjustereGrunnetInntekt(
                beregnGraderGrunnlag,
                uttaksgradResultat
            )
        ) {
            if (beregnGraderGrunnlag.inntektsgradering.uttaksgrad < TJUE_PROSENT && beregnGraderGrunnlag.inntektsgradering.uttaksgrad > NULL_PROSENT) {
                TJUE_PROSENT
            } else {
                beregnGraderGrunnlag.inntektsgradering.uttaksgrad
            }
        } else null

    private fun skalNedjustereGrunnetInntekt(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        uttaksgradResultat: UttaksgradResultat
    ) = beregnGraderGrunnlag.inntektsgradering != null &&
            beregnGraderGrunnlag.inntektsgradering.uttaksgrad.setScale(2, RoundingMode.HALF_UP)
                .compareTo(uttaksgradResultat.uttaksgrad.setScale(2, RoundingMode.HALF_UP)) < 0

    override fun beregnMedMaksGrad(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        maksGradIProsent: Prosent
    ): GraderBeregnet {
        val etablertTilsynsprosent = finnEtablertTilsynsprosent(beregnGraderGrunnlag.etablertTilsyn)
        val søkersTapteArbeidstid =
            beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid()
        val ønsketUttaksgradProsent = finnØnsketUttaksgradProsent(beregnGraderGrunnlag.oppgittTilsyn);
        val ønsketUttaksgrad = minOf(ønsketUttaksgradProsent, maksGradIProsent)
        val uttaksgradResultat = avklarUttaksgrad(
            beregnGraderGrunnlag,
            etablertTilsynsprosent,
            ønsketUttaksgrad
        )
        val utbetalingsgrader = BeregnUtbetalingsgrader.beregn(
            uttaksgradResultat.uttaksgrad,
            uttaksgradResultat.overstyrtUttaksgrad,
            uttaksgradResultat.oppfyltÅrsak == Årsak.GRADERT_MOT_TILSYN,
            beregnGraderGrunnlag
        )

        val uttaksgradMedReduksjonGrunnetInntektsgradering =
            getUttaksgradJustertMotInntektsgradering(beregnGraderGrunnlag, uttaksgradResultat)
        val faktiskUttaksgrad = uttaksgradMedReduksjonGrunnetInntektsgradering ?: uttaksgradResultat.uttaksgrad

        return GraderBeregnet(
            pleiebehov = beregnGraderGrunnlag.pleiebehov,
            graderingMotTilsyn = GraderingMotTilsyn(
                etablertTilsyn = etablertTilsynsprosent,
                andreSøkeresTilsyn = beregnGraderGrunnlag.andreSøkeresTilsyn,
                andreSøkeresTilsynReberegnet = beregnGraderGrunnlag.andreSøkeresTilsynReberegnet,
                tilgjengeligForSøker = uttaksgradResultat.restTilSøker,
                overseEtablertTilsynÅrsak = uttaksgradResultat.overseEtablertTilsynÅrsak
            ),
            søkersTapteArbeidstid = søkersTapteArbeidstid,
            oppgittTilsyn = beregnGraderGrunnlag.oppgittTilsyn,
            uttaksgrad = faktiskUttaksgrad.setScale(0, RoundingMode.HALF_UP),
            uttaksgradUtenReduksjonGrunnetInntektsgradering = uttaksgradResultat.uttaksgrad,
            uttaksgradMedReduksjonGrunnetInntektsgradering = uttaksgradMedReduksjonGrunnetInntektsgradering,
            utbetalingsgrader = utbetalingsgrader,
            årsak = uttaksgradResultat.årsak()
        )
    }

    private fun avklarUttaksgrad(
        beregnGraderGrunnlag: BeregnGraderGrunnlag,
        etablertTilsynprosent: Prosent,
        ønsketUttaksgradProsent: Prosent
    ): UttaksgradResultat {

        val søkersTapteArbeidstid =
            beregnGraderGrunnlag.arbeid.finnSøkersTapteArbeidstid()

        val restTilSøker =
            finnRestTilSøker(
                beregnGraderGrunnlag.pleiebehov,
                etablertTilsynprosent,
                beregnGraderGrunnlag.andreSøkeresTilsyn,
                beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )

        val overstyrtUttak =
            if (beregnGraderGrunnlag.overstyrtInput != null) beregnGraderGrunnlag.overstyrtInput.overstyrtUttaksgrad else null

        if (restTilSøker < TJUE_PROSENT) {
            val forLavGradÅrsak =
                utledForLavGradÅrsak(
                    beregnGraderGrunnlag.pleiebehov,
                    etablertTilsynprosent,
                    beregnGraderGrunnlag.andreSøkeresTilsyn,
                    beregnGraderGrunnlag.overseEtablertTilsynÅrsak
                )
            if (!(beregnGraderGrunnlag.ytelseType == YtelseType.PLS && restTilSøker > Prosent.ZERO && Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE == forLavGradÅrsak)) {
                return UttaksgradResultat(
                    restTilSøker,
                    Prosent.ZERO,
                    overstyrtUttak,
                    ikkeOppfyltÅrsak = forLavGradÅrsak,
                    overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
                )
            }
        }

        if (søkersTapteArbeidstid < TJUE_PROSENT) {
            return UttaksgradResultat(
                restTilSøker,
                Prosent.ZERO,
                overstyrtUttak,
                ikkeOppfyltÅrsak = Årsak.FOR_LAV_TAPT_ARBEIDSTID,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }

        if (ønsketUttaksgradProsent < TJUE_PROSENT) {
            return UttaksgradResultat(
                restTilSøker,
                Prosent.ZERO,
                overstyrtUttak,
                ikkeOppfyltÅrsak = Årsak.FOR_LAV_ØNSKET_UTTAKSGRAD,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }

        if (ønsketUttaksgradProsent < restTilSøker && ønsketUttaksgradProsent < søkersTapteArbeidstid) {
            return UttaksgradResultat(
                restTilSøker,
                ønsketUttaksgradProsent,
                overstyrtUttak,
                oppfyltÅrsak = Årsak.AVKORTET_MOT_SØKERS_ØNSKE,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }
        if (restTilSøker < søkersTapteArbeidstid) {
            return UttaksgradResultat(
                restTilSøker,
                restTilSøker,
                overstyrtUttak,
                oppfyltÅrsak = Årsak.GRADERT_MOT_TILSYN,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }
        if (beregnGraderGrunnlag.arbeid.fulltFravær()) {
            return UttaksgradResultat(
                restTilSøker,
                søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP),
                overstyrtUttak,
                oppfyltÅrsak = Årsak.FULL_DEKNING,
                overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
            )
        }
        return UttaksgradResultat(
            restTilSøker,
            søkersTapteArbeidstid.setScale(2, RoundingMode.HALF_UP),
            overstyrtUttak,
            oppfyltÅrsak = Årsak.AVKORTET_MOT_INNTEKT,
            overseEtablertTilsynÅrsak = beregnGraderGrunnlag.overseEtablertTilsynÅrsak
        )
    }


    private fun finnØnsketUttaksgradProsent(ønsketUttaksgrad: Duration?): Prosent {
        if (ønsketUttaksgrad == null) {
            return HUNDRE_PROSENT
        }
        if (ønsketUttaksgrad > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (ønsketUttaksgrad < Duration.ZERO) {
            return Prosent.ZERO
        }
        return BigDecimal(ønsketUttaksgrad.toMillis()).setScale(2, RoundingMode.HALF_UP)
            .divide(BigDecimal(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
    }

    private fun finnRestTilSøker(
        pleiebehov: Pleiebehov,
        etablertTilsynsprosent: Prosent,
        andreSøkeresTilsyn: Prosent,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
    ): BigDecimal {
        if (pleiebehov == Pleiebehov.PROSENT_0) {
            return Prosent.ZERO
        }
        val pleiebehovprosent = pleiebehov.prosent
        if (overseEtablertTilsynÅrsak != null) {
            return pleiebehovprosent - andreSøkeresTilsyn
        }
        val gradertMotTilsyn = HUNDRE_PROSENT - etablertTilsynsprosent
        val restTilSøker =
            pleiebehovprosent - (etablertTilsynsprosent * (pleiebehovprosent.divide(
                HUNDRE_PROSENT,
                2,
                RoundingMode.HALF_UP
            ))) - andreSøkeresTilsyn
        val minsteAvRestTilSøkerOgGraderingMotTilsyn = minOf(gradertMotTilsyn, restTilSøker)
        if (minsteAvRestTilSøkerOgGraderingMotTilsyn < Prosent.ZERO) {
            return Prosent.ZERO
        }
        return minsteAvRestTilSøkerOgGraderingMotTilsyn
    }

    private fun utledForLavGradÅrsak(
        pleiebehov: Pleiebehov,
        etablertTilsynsprosent: Prosent,
        andreSøkeresTilsyn: Prosent,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak?
    ): Årsak? {
        if (pleiebehov == Pleiebehov.PROSENT_0) {
            return Årsak.UTENOM_PLEIEBEHOV
        }
        if (overseEtablertTilsynÅrsak != null) {
            if (andreSøkeresTilsyn > ÅTTI_PROSENT) {
                return Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE
            }
        } else {
            when {
                andreSøkeresTilsyn > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ANDRE_SØKERE
                }

                etablertTilsynsprosent > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN
                }

                andreSøkeresTilsyn + etablertTilsynsprosent > ÅTTI_PROSENT -> {
                    return Årsak.FOR_LAV_REST_PGA_ETABLERT_TILSYN_OG_ANDRE_SØKERE
                }
            }
        }
        return null
    }

    private fun finnEtablertTilsynsprosent(etablertTilsyn: Duration): Prosent {
        if (etablertTilsyn > FULL_DAG) {
            return HUNDRE_PROSENT
        }
        if (etablertTilsyn < Duration.ZERO) {
            return Prosent.ZERO
        }
        return BigDecimal(etablertTilsyn.toMillis()).setScale(2, RoundingMode.HALF_UP)
            .divide(BigDecimal(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
    }
}

private fun Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>.fulltFravær() = this.values.all { it.fulltFravær() }
private fun ArbeidsforholdPeriodeInfo.fulltFravær() = jobberNå == Duration.ZERO
private fun ArbeidsforholdPeriodeInfo.ikkeFravær() = jobberNormalt <= jobberNå
