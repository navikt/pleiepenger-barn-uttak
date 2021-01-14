package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.delregler.SøkersAlderRegel.Companion.søkerfyllerSøtiAvslåttÅrsak
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.inneholder
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåTom
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger
import java.lang.IllegalStateException
import java.time.LocalDate
import java.util.*

internal class SøkersAlderRegel : UttaksplanRegel {

    internal companion object {
        internal fun søkerfyllerSøtiAvslåttÅrsak(søkersSøttiårsdag: LocalDate) = AvslåttÅrsak(
                årsak = AvslåttÅrsaker.SØKERS_ALDER,
                hjemler = setOf(Lovhenvisninger.SøkerFyllerSøtti.anvend(
                        "Fastsatt at søker fyller 70 år $søkersSøttiårsdag. " +
                        "Perioden avslås derfor ettersom det ikke foreligger rett til pleiepenger."
                ))
        )
    }

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

private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.avslåAllePerioderEtterSøttiårsdag(
    kildeBehandlingUUID: BehandlingUUID,
    søkersSøttiårsdag: LocalDate
) {
    filterKeys { it.fom.isAfter(søkersSøttiårsdag) }
    .forEach { (periode, periodeInfo) ->
        if (periodeInfo is AvslåttPeriode) {
            val avslagsÅrsaker = periodeInfo
                    .årsaker
                    .toMutableSet()
                    .also { eksisterende ->
                        eksisterende.add(søkerfyllerSøtiAvslåttÅrsak(søkersSøttiårsdag))
                    }

            put(periode, periodeInfo.copy(
                    årsaker = avslagsÅrsaker
            ))
        } else {
            put(periode, AvslåttPeriode(
                    knekkpunktTyper = periodeInfo.knekkpunktTyper(),
                    kildeBehandlingUUID = kildeBehandlingUUID,
                    årsaker = setOf(søkerfyllerSøtiAvslåttÅrsak(søkersSøttiårsdag))
            ))
        }
    }
}

private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.fyllerSøttiIEnUttaksperiode(
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

    val periodeInfoMedKnekkpunkt = when (periodeInfo) {
        is InnvilgetPeriode -> {
            periodeInfo.copy(knekkpunktTyper = setOf(KnekkpunktType.SØKERS_ALDER))
        }
        is AvslåttPeriode -> {
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


private fun SortedMap<LukketPeriode, UttaksPeriodeInfo>.fyllerSøkerSøttiILøpetAvUttaksplanen(
        søkersSøttiårsdag: LocalDate) : Boolean {
    val sisteDagISistePeriode = lastKey().tom
    return sisteDagISistePeriode.isAfter(søkersSøttiårsdag)
}

private fun Søker.søttiårsdag() = fødselsdato.plusYears(70)