package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.UttaksplanMedBehandlingUuid
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperHelt

internal class TilsynForAndrePleietrengendeRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Regelutfall {
        if (grunnlag.finnesOppfyltPlanMedHøyerePrioritet(periode))
            return IkkeOppfylt(årsaker = setOf(Årsak.ANNEN_PLEIETRENGENDE_MED_HØYERE_PRIO))
        return TilBeregningAvGrad()
    }

    private fun RegelGrunnlag.finnesOppfyltPlanMedHøyerePrioritet(periode: LukketPeriode): Boolean {
        val kravprioritetPeriode = kravprioritetForEgneBehandlinger.keys.firstOrNull { it.overlapperHelt(periode) }
            ?: return false

        val kravprioritetListe = kravprioritetForEgneBehandlinger[kravprioritetPeriode] ?: return false

        for (behandlingMedKrav in kravprioritetListe) {
            if (behandlingMedKrav == this.behandlingUUID) {
                return false
            }
            val uttaksplanMedKrav = egneUttaksplanerAllePleietrengendePerBehandling[behandlingMedKrav]
            if (uttaksplanMedKrav != null) {
                val erOppfylt = uttaksplanMedKrav.perioder.entries.any({
                    it.key.overlapperHelt(kravprioritetPeriode) && it.value.årsaker.all({ it.oppfylt })
                })
                if (erOppfylt) {
                    return true
                }
            }
        }
        return false
    }

}
