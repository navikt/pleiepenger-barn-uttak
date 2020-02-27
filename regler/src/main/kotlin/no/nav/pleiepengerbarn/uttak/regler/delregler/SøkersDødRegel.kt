package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
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
                            dødsdato = dødsdato,
                            uttaksperiode = this
                    )
                }

        perioder.avslåAllePerioderEtterDødsfall(
                dødsdato = dødsdato
        )

        return uttaksplan.copy(
                perioder = perioder
        )
    }
}

private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.dødeIEnUttaksperiode(
        dødsdato: LocalDate,
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
    // Om periden allerede var avlsått får den en ny avslagsårsak - SØKERS_DØDSFALL
    // Får alltid et nytt knekkpukt - SØKERS_DØDSFALL
    val periodeEtterDødsfall = LukketPeriode(
            fom = dødsdato.plusDays(1),
            tom = uttaksperiode.key.tom
    )

    val uttaksPeriodeInfo = uttaksperiode.value
    val knekkpunktTyper = uttaksPeriodeInfo
            .knekkpunktTyper()
            .toMutableSet()
            .apply { this.add(KnekkpunktType.SØKERS_DØDSFALL) }

    val avslagsÅrsaker = (if (uttaksPeriodeInfo is AvslåttPeriode) uttaksPeriodeInfo.avslagsÅrsaker.toMutableSet() else mutableSetOf()).also {
        it.add(AvslåttPeriodeÅrsak.SØKERS_DØDSFALL)
    }
    put(periodeEtterDødsfall, AvslåttPeriode(
            knekkpunktTyper = knekkpunktTyper,
            avslagsÅrsaker = avslagsÅrsaker
    ))
}

private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.avslåAllePerioderEtterDødsfall(
        dødsdato: LocalDate) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach { (periode, periodeInfo) ->
        /*
            1. Avslåtte perioder forblir avslåtte, men det blir lagt til en ny avslagsårsak (SØKERS_DØDSFALL)
            2. Innnvilgede perioder blir avslått med avslagsårsak SØKERS_DØDSFALL
         */
        if (periodeInfo is AvslåttPeriode) {
            val avslagsÅrsaker = periodeInfo
                    .avslagsÅrsaker
                    .toMutableSet()
                    .also { eksisterende -> eksisterende.add(AvslåttPeriodeÅrsak.SØKERS_DØDSFALL) }

            put(periode, periodeInfo.copy(
                    avslagsÅrsaker = avslagsÅrsaker)
            )
        } else {
            put(periode, AvslåttPeriode(
                    knekkpunktTyper = periodeInfo.knekkpunktTyper(),
                    avslagsÅrsaker = setOf(AvslåttPeriodeÅrsak.SØKERS_DØDSFALL)
            ))
        }
    }
}