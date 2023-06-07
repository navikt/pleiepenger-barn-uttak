package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.OverseEtablertTilsynÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.OverstyrtInput
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.YtelseType
import java.time.Duration


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
    val overstyrtInput: OverstyrtInput? = null
)