package no.nav.pleiepengerbarn.uttak.validering

import io.konform.validation.ValidationBuilder
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode

internal fun ValidationBuilder<Uttaksperiode>.gyldigPeriode() {
    addConstraint("tilOgMed må være etter fraOgMed") {
        it.tom.isAfter(it.fom)
    }
}