package no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak

internal fun LukketPeriode.overlapperHelt(annen: LukketPeriode) =
        (fom == annen.fom || fom.isBefore(annen.fom)) &&
        (tom == annen.tom || tom.isAfter(annen.tom))

internal fun LukketPeriode.overlapperDelvis(annen: LukketPeriode) =
        (fom == annen.tom || fom.isBefore(annen.tom)) &&
        (tom == annen.fom || tom.isAfter(annen.fom))


internal fun <T> Map<LukketPeriode, T>.sortertPåFom() = toSortedMap(compareBy { it.fom })
internal fun Collection<SøktUttak>.sortertPåFom() = sortedBy { it.periode.fom }