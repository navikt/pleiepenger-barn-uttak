package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.NULL_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.stream
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.tilVirkedager
import java.time.LocalDate

private const val MAX_DAGER_PER_ÅR = 8 * 5

internal class UtenlandsoppholdRegel : UttaksplanRegel {


    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {

        val featureToggleUtenlandsoppholdRgel = System.getenv("UTENLANDSOPPHOLD_REGEL").toBoolean()
        if (!featureToggleUtenlandsoppholdRgel) {
            return uttaksplan
        }

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        val utenlandsdagerFraForrigeUttaksplan = grunnlag.finnUtenlandsdager()

        val sortertePerioder = uttaksplan.perioder.keys.toList().sortedBy { it.fom }
        sortertePerioder.forEach { periode ->
            val info = uttaksplan.perioder[periode]
                ?: throw IllegalStateException("Dette skal ikke kunne skje. Alle perioder skal finnes i map.")
            if (info.utfall == Utfall.OPPFYLT && grunnlag.overlapperMedUtenlandsoppholdUtenGyldigÅrsakUtenforEøs(periode)) {
                val avklartePerioder = periode.avklarPeriode(utenlandsdagerFraForrigeUttaksplan)
                avklartePerioder.forEach { (nyPeriode, utenlandsoppholdInnvilget) ->
                    if (utenlandsoppholdInnvilget) {
                        nyePerioder[nyPeriode] = info.copy(utenlandsoppholdUtenÅrsak = true)
                        utenlandsdagerFraForrigeUttaksplan.addAll(nyPeriode.tilVirkedager())
                    } else {
                        nyePerioder[nyPeriode] = info.settIkkeInnvilgetPgaUtenlandsopphold()
                        utenlandsdagerFraForrigeUttaksplan.removeAll(nyPeriode.tilVirkedager())
                    }
                }
            } else {
                nyePerioder[periode] = info
                utenlandsdagerFraForrigeUttaksplan.removeAll(periode.tilVirkedager())
            }
        }
        return uttaksplan.copy(perioder = nyePerioder)
    }

}

private fun RegelGrunnlag.overlapperMedUtenlandsoppholdUtenGyldigÅrsakUtenforEøs(periode: LukketPeriode): Boolean {
    val eøsLand: List<String> = listOf("ALA", "AUT", "BEL", "BGR", "CYP", "CZE",
        "DEU", "DNK", "ESP", "EST", "FIN", "FRA", "FRO", "GBR", "GRC", "GRL",
        "HRV", "HUN", "IRL", "ISL", "ITA", "LIE", "LTU", "LUX", "LVA", "MLT",
        "NLD", "NOR", "POL", "PRT", "ROU", "SVK", "SVN", "SWE")
    return this.utenlandsoppholdperioder.any {
        (periode.overlapperDelvis(it.key)
                && it.value.utenlandsoppholdÅrsak == UtenlandsoppholdÅrsak.INGEN)
                && !eøsLand.contains(it.value.landkode)
    }
}

private fun UttaksperiodeInfo.settIkkeInnvilgetPgaUtenlandsopphold(): UttaksperiodeInfo {
    val årsaker = setOf(Årsak.FOR_MANGE_DAGER_UTENLANDSOPPHOLD)
    val oppdaterteUtbetalingsgrader = this.utbetalingsgrader.map {it.copy(utbetalingsgrad = NULL_PROSENT)}
    return this.copy(utfall = Utfall.IKKE_OPPFYLT, uttaksgrad = NULL_PROSENT, årsaker = årsaker, utbetalingsgrader = oppdaterteUtbetalingsgrader)
}

private fun LukketPeriode.avklarPeriode(utenlandsdager: Set<LocalDate>): Map<LukketPeriode, Boolean> {

    val segmenter = mutableListOf<LocalDateSegment<Boolean>>()

    val antallFraTidligereUttaksplan = utenlandsdager.mellom(this.fom.minusYears(1), this.fom.minusDays(1))

    var brukteDagerPåDennePerioden = 0
    this.stream().forEach { dato ->
        val forbrukteDager = antallFraTidligereUttaksplan + brukteDagerPåDennePerioden
        if (forbrukteDager < MAX_DAGER_PER_ÅR) {
            segmenter.add(LocalDateSegment(dato, dato, true))
            brukteDagerPåDennePerioden++
        } else {
            segmenter.add(LocalDateSegment(dato, dato, false))
        }
    }
    val nyePerioder = LocalDateTimeline(segmenter).compress()

    return nyePerioder.toSegments().associate {LukketPeriode(it.fom, it.tom) to it.value}
}

private fun Set<LocalDate>.mellom(fom: LocalDate, tom: LocalDate) = this.count { it in fom..tom }

private fun RegelGrunnlag.finnUtenlandsdager(): MutableSet<LocalDate> {
    if (this.forrigeUttaksplan == null) {
        return mutableSetOf()
    }
    return this.forrigeUttaksplan.perioder
        .filter {it.value.utenlandsoppholdUtenÅrsak}
        .flatMap {it.key.tilVirkedager()}
        .toMutableSet()
}
