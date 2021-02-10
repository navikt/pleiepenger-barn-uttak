package no.nav.pleiepengerbarn.uttak.scenario

import no.nav.pleiepengerbarn.uttak.testklient.lagGrunnlag
import no.nav.pleiepengerbarn.uttak.testklient.testClientMotLokalServer

fun main() {
    val grunnlag = lagGrunnlag(periode = "2020-01-01/2020-01-10")
    testClientMotLokalServer().opprettUttaksplan(grunnlag)
    println("Opprettet uttaksplan for behandlingUUID = ${grunnlag.behandlingUUID}")
}

