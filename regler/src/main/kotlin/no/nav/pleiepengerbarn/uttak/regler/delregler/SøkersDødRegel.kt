package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.TapAvInntekt
import java.time.LocalDate
import java.util.*

internal class SøkersDødRegel : UttaksplanRegel {
    internal companion object {
        internal fun søkersDødAvslåttÅrsak(dødsdato: LocalDate) = AvslåttÅrsak(
                årsak = AvslåttÅrsaker.SØKERS_DØDSFALL,
                hjemler = setOf(TapAvInntekt.anvend("Fastsatt at søker døde $dødsdato."))
        )
    }
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
    // Legges til en ny avslagsårsak - SØKERS_DØDSFALL
    // Får alltid et nytt knekkpukt - SØKERS_DØDSFALL
    val periodeEtterDødsfall = LukketPeriode(
            fom = dødsdato.plusDays(1),
            tom = uttaksperiode.key.tom
    )

    val uttaksPeriodeInfo = uttaksperiode.value
    val knekkpunktTyper = uttaksPeriodeInfo
            .knekkpunktTyper()
            .toMutableSet()
            .also { it.add(KnekkpunktType.SØKERS_DØDSFALL) }

    val avslåttÅrsaker = (if (uttaksPeriodeInfo is AvslåttPeriode) uttaksPeriodeInfo.årsaker.toMutableSet() else mutableSetOf()).also {
        it.add(SøkersDødRegel.søkersDødAvslåttÅrsak(dødsdato))
    }
    put(periodeEtterDødsfall, AvslåttPeriode(
            knekkpunktTyper = knekkpunktTyper,
            årsaker = avslåttÅrsaker
    ))
}

private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.avslåAllePerioderEtterDødsfall(
        dødsdato: LocalDate) {
    filterKeys { it.fom.isAfter(dødsdato) }.forEach { (periode, periodeInfo) ->
        /*
            1. Avslåtte perioder forblir avslåtte, men det blir lagt til en ny avslagsårsak - SØKERS_DØDSFALL
            2. Innnvilgede perioder blir avslått med avslagsårsak - SØKERS_DØDSFALL
         */
        if (periodeInfo is AvslåttPeriode) {
            val avslagsÅrsaker = periodeInfo
                    .årsaker
                    .toMutableSet()
                    .also { eksisterende -> eksisterende.add(SøkersDødRegel.søkersDødAvslåttÅrsak(dødsdato))
            }

            put(periode, periodeInfo.copy(
                    årsaker = avslagsÅrsaker
            ))
        } else {
            put(periode, AvslåttPeriode(
                    knekkpunktTyper = periodeInfo.knekkpunktTyper(),
                    årsaker = setOf(SøkersDødRegel.søkersDødAvslåttÅrsak(dødsdato))
            ))
        }
    }
}