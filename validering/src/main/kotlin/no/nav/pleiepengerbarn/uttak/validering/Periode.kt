package no.nav.pleiepengerbarn.uttak.validering

import io.konform.validation.ValidationBuilder
import no.nav.pleiepengerbarn.uttak.kontrakter.Periode

internal fun ValidationBuilder<Periode>.gyldigPeriode() {
    addConstraint("tilOgMed må være etter fraOgMed") {
        it.tilOgMed.isAfter(it.fraOgMed)
    }
}