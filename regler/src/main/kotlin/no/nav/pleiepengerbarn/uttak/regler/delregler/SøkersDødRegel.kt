package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.annenPart
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import java.time.LocalDate
import java.util.*

internal class SøkersDødRegel : UttaksplanRegel {

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {

        val dødsdato = grunnlag.søker.dødsdato ?: return uttaksplan

        val perioder = uttaksplan.perioder.sortertPåFom()

        perioder.inneholder(dødsdato)
                ?.takeUnless { it.key.tom.isEqual(dødsdato) }
                ?.apply {
                    perioder.dødeIEnUttaksperiode(
                            kildeBehandlingUUID = grunnlag.behandlingUUID,
                            dødsdato = dødsdato,
                            uttaksperiode = this,
                            annenPart = grunnlag.annenPart(this.key)
                    )
                }

        perioder.avslåAllePerioderEtterDødsfall(
                grunnlag = grunnlag,
                dødsdato = dødsdato
        )

        return uttaksplan.copy(
                perioder = perioder
        )
    }
}

private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.dødeIEnUttaksperiode(
        kildeBehandlingUUID: BehandlingUUID,
        dødsdato: LocalDate,
        annenPart: AnnenPart,
        uttaksperiode: Uttaksperiode) {

    // Fjerner uttaksperioden dødsfallet skjedde
    remove(uttaksperiode.key)

    // Første delen av uttaksperioden beholdes som den var, men med ny TOM-dato
    val periodeFremTilDødsfall = LukketPeriode(
            fom = uttaksperiode.key.fom,
            tom = dødsdato
    )
    put(periodeFremTilDødsfall, uttaksperiode.value)

    // Andre delen av perioden avslås.
    // Legges til en ny avslagsårsak - SØKERS_DØDSFALL
    // Får alltid et nytt knekkpukt - SØKERS_DØDSFALL
    val periodeEtterDødsfall = LukketPeriode(
            fom = dødsdato.plusDays(1),
            tom = uttaksperiode.key.tom
    )

    val uttaksperiodeInfo = uttaksperiode.value

    val knekkpunktTyper = uttaksperiodeInfo
        .knekkpunktTyper
        .toMutableSet()
        .also { it.add(KnekkpunktType.SØKERS_DØDSFALL) }

    val avslåttÅrsaker = if (uttaksperiodeInfo.utfall == Utfall.IKKE_OPPFYLT) uttaksperiodeInfo.årsaker.toMutableSet() else mutableSetOf()
    avslåttÅrsaker.add(Årsak.SØKERS_DØDSFALL)

    put(periodeEtterDødsfall, UttaksperiodeInfo.avslag(avslåttÅrsaker, knekkpunktTyper, kildeBehandlingUUID, annenPart))
}

private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.avslåAllePerioderEtterDødsfall(
        grunnlag: RegelGrunnlag,
        dødsdato: LocalDate) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach { (periode, periodeInfo) ->
        /*
            1. Avslåtte perioder forblir avslåtte, men det blir lagt til en ny avslagsårsak - SØKERS_DØDSFALL
            2. Innnvilgede perioder blir avslått med avslagsårsak - SØKERS_DØDSFALL
         */
        if (periodeInfo.utfall == Utfall.IKKE_OPPFYLT) {
            val avslagsÅrsaker = periodeInfo
                    .årsaker
                    .toMutableSet()
                    .also { eksisterende -> eksisterende.add(Årsak.SØKERS_DØDSFALL)
            }

            put(periode, periodeInfo.copy(
                    årsaker = avslagsÅrsaker
            ))
        } else {
            put(periode, UttaksperiodeInfo.avslag(
                årsaker = setOf(Årsak.SØKERS_DØDSFALL),
                knekkpunktTyper = periodeInfo.knekkpunktTyper,
                kildeBehandlingUUID = grunnlag.behandlingUUID,
                annenPart = grunnlag.annenPart(periode))
            )
        }
    }
}