package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.time.Duration
import java.time.LocalDate


data class BeregnGraderGrunnlag (
    val pleiebehov: Pleiebehov,
    val etablertTilsyn: Duration,
    val oppgittTilsyn: Duration? = null,
    val andreSøkeresTilsyn: Prosent,
    val andreSøkeresTilsynReberegnet: Boolean,
    val overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak? = null,
    val arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>,
    val ytelseType: YtelseType,
    val periode: LukketPeriode,
    val nyeReglerUtbetalingsgrad: LocalDate?
)