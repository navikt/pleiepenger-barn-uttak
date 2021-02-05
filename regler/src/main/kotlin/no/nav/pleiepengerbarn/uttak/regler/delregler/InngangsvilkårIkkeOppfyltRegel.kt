package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapper
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom

internal class InngangsvilkårIkkeOppfyltRegel : UttaksplanRegel {

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        val perioder = uttaksplan.perioder.sortertPåFom()
        perioder.forEach {
            val (utfall, inngangsvilkår) = sjekkInngangsvilkår(it.key, grunnlag)
            if (utfall == Utfall.IKKE_OPPFYLT) {
                //Inngangsvilkår ikke oppfylt
                if (it.value.utfall == Utfall.OPPFYLT) {
                    //Sett til ikke oppfylt dersom oppfylt
                    nyePerioder[it.key] = it.value.copy(årsaker = setOf(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT), utfall = Utfall.IKKE_OPPFYLT)
                } else {
                    //Legg til inngangsvilkår ikke oppfylt dersom perioden allerede er ikke oppfylt
                    val årsaker = it.value.årsaker.toMutableSet()
                    årsaker.add(Årsak.INNGANGSVILKÅR_IKKE_OPPFYLT)
                    nyePerioder[it.key] = it.value.copy(årsaker = årsaker)
                }
            } else {
                nyePerioder[it.key] = it.value.copy(inngangsvilkår = inngangsvilkår)
            }
        }

        return uttaksplan.copy(perioder = nyePerioder)
    }

    private fun sjekkInngangsvilkår(periode: LukketPeriode, grunnlag: RegelGrunnlag): Pair<Utfall, Map<String, Utfall>> {
        val inngangsvilkårForPeriode = grunnlag.inngangsvilkår.inngangsvilkårForPeriode(periode)
        val utfallInngangsvikår = inngangsvilkårForPeriode.utfallInngangsvilkår()
        return Pair(utfallInngangsvikår, inngangsvilkårForPeriode)
    }

}

private fun Map<String, List<Vilkårsperiode>>.inngangsvilkårForPeriode(periode: LukketPeriode): Map<String, Utfall> {
    val inngangsvilkår = mutableMapOf<String, Utfall>()
    this.forEach { (vilkårskode, perioder) ->
        perioder.forEach { vilkårsperiode ->
            if (vilkårsperiode.periode.overlapper(periode)) {
                inngangsvilkår[vilkårskode] = vilkårsperiode.utfall
            }
        }
    }
    return inngangsvilkår
}

private fun Map<String, Utfall>.utfallInngangsvilkår(): Utfall {
    if (this.values.any { it == Utfall.IKKE_OPPFYLT }) {
        return Utfall.IKKE_OPPFYLT
    }
    return Utfall.OPPFYLT
}