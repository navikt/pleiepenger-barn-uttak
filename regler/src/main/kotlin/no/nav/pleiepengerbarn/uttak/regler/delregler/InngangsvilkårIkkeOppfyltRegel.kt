package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom

internal class InngangsvilkårIkkeOppfyltRegel : UttaksplanRegel {

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        val perioder = uttaksplan.perioder.sortertPåFom()
        perioder.forEach { (periode, info) ->
            val (utfallInngangsvilkår, inngangsvilkår) = grunnlag.sjekkInngangsvilkår(periode)
            if (utfallInngangsvilkår == Utfall.IKKE_OPPFYLT) {
                //Inngangsvilkår ikke oppfylt
                if (info.utfall == Utfall.OPPFYLT) {
                    //Sett til ikke oppfylt dersom oppfylt
                    nyePerioder[periode] = info.copy(årsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT), utfall = Utfall.IKKE_OPPFYLT, inngangsvilkår = inngangsvilkår)
                } else {
                    //Legg til inngangsvilkår ikke oppfylt dersom perioden allerede er ikke oppfylt
                    val årsaker = info.årsaker.toMutableSet()
                    årsaker.add(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
                    nyePerioder[periode] = info.copy(årsaker = årsaker, inngangsvilkår = inngangsvilkår)
                }
            } else {
                nyePerioder[periode] = info.copy(inngangsvilkår = inngangsvilkår)
            }
        }

        return uttaksplan.copy(perioder = nyePerioder)
    }

}
