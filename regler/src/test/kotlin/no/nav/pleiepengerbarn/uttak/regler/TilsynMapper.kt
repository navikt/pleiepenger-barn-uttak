package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent

internal fun Map<LukketPeriode, Prosent>.somTilsynperioder() = mapValues { (_, prosent) -> FULL_DAG.prosent(prosent) }

