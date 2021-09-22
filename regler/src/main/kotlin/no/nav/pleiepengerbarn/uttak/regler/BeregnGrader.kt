package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import java.time.Duration

object BeregnGrader {

    internal fun beregn(
        pleiebehov: Pleiebehov,
        etablertTilsyn: Duration,
        oppgittTilsyn: Duration? = null,
        andreSøkeresTilsyn: Prosent,
        andreSøkeresTilsynReberegnet: Boolean,
        overseEtablertTilsynÅrsak: OverseEtablertTilsynÅrsak? = null,
        arbeid: Map<Arbeidsforhold, ArbeidsforholdPeriodeInfo>
    ): GraderBeregnet {
        val featureToggleBeregnGrader = System.getenv("BEREGN_GRADER_EXPERIMENTAL").toBoolean()
        if (featureToggleBeregnGrader) {
            return BeregnGraderExperimental.beregn(pleiebehov, etablertTilsyn, oppgittTilsyn, andreSøkeresTilsyn, andreSøkeresTilsynReberegnet, overseEtablertTilsynÅrsak, arbeid)
        }
        return BeregnGraderLegacy.beregn(pleiebehov, etablertTilsyn, oppgittTilsyn, andreSøkeresTilsyn, andreSøkeresTilsynReberegnet, overseEtablertTilsynÅrsak, arbeid)

    }

}