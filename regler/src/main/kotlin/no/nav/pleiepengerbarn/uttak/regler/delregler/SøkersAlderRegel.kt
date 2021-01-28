package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåTom
import java.lang.IllegalStateException
import java.time.LocalDate
import java.util.*

internal class SøkersAlderRegel : UttaksplanRegel {

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {
        val perioder = uttaksplan.perioder.sortertPåTom()

        val søkersSøttiårsdag = grunnlag.søker.søttiårsdag()

        if (!perioder.fyllerSøkerSøttiILøpetAvUttaksplanen(søkersSøttiårsdag)) return uttaksplan


        perioder.inneholder(søkersSøttiårsdag)
                ?.takeUnless { it.key.tom.isEqual(søkersSøttiårsdag) }
                ?.apply {
                    perioder.fyllerSøttiIEnUttaksperiode(
                            søkersSøttiårsdag = søkersSøttiårsdag,
                            uttaksperiode = this
                    )
                }

        perioder.avslåAllePerioderEtterSøttiårsdag(
            kildeBehandlingUUID = grunnlag.behandlingUUID,
            søkersSøttiårsdag = søkersSøttiårsdag
        )

        return uttaksplan.copy(
                perioder = perioder
        )
    }

}

private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.avslåAllePerioderEtterSøttiårsdag(
    kildeBehandlingUUID: BehandlingUUID,
    søkersSøttiårsdag: LocalDate
) {
    filterKeys { it.fom.isAfter(søkersSøttiårsdag) }
    .forEach { (periode, periodeInfo) ->
        if (periodeInfo.utfall == Utfall.AVSLÅTT) {
            val avslagsÅrsaker = periodeInfo
                    .årsaker
                    .toMutableSet()
                    .also { eksisterende ->
                        eksisterende.add(Årsak.SØKERS_ALDER)
                    }

            put(periode, periodeInfo.copy(
                    årsaker = avslagsÅrsaker
            ))
        } else {
            put(periode, UttaksperiodeInfo.avslag(
                årsaker = setOf(Årsak.SØKERS_ALDER),
                knekkpunktTyper = periodeInfo.knekkpunktTyper,
                kildeBehandlingUUID = kildeBehandlingUUID
            ))
        }
    }
}

private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.fyllerSøttiIEnUttaksperiode(
        søkersSøttiårsdag: LocalDate,
        uttaksperiode: Uttaksperiode) {
    val periode = uttaksperiode.key
    val periodeInfo = uttaksperiode.value

    // Fjerner perioden søker fyller 70 år
    remove(periode)

    // Legger til knekk FOM - søkersSøttiårsdag
    put(LukketPeriode(
            fom = periode.fom,
            tom = søkersSøttiårsdag
    ), periodeInfo)

    val periodeInfoMedKnekkpunkt = when (periodeInfo.utfall) {
        Utfall.INNVILGET -> {
            periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.SØKERS_ALDER))
        }
        Utfall.AVSLÅTT -> {
            periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.SØKERS_ALDER))
        }
        else -> throw IllegalStateException("Må være en innvilget eller avslått periode.")
    }
    // Legger til knekk (søkersSøttiårsdag+1) - TOM
    put(LukketPeriode(
            fom = søkersSøttiårsdag.plusDays(1),
            tom = periode.tom
    ), periodeInfoMedKnekkpunkt)
}


private fun SortedMap<LukketPeriode, UttaksperiodeInfo>.fyllerSøkerSøttiILøpetAvUttaksplanen(
        søkersSøttiårsdag: LocalDate) : Boolean {
    val sisteDagISistePeriode = lastKey().tom
    return sisteDagISistePeriode.isAfter(søkersSøttiårsdag)
}

private fun Søker.søttiårsdag() = fødselsdato.plusYears(70)