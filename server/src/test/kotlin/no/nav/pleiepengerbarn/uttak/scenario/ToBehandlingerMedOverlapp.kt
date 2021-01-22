package no.nav.pleiepengerbarn.uttak.scenario

import no.nav.pleiepengerbarn.uttak.testklient.lagGrunnlag
import no.nav.pleiepengerbarn.uttak.testklient.nesteSaksnummer
import no.nav.pleiepengerbarn.uttak.testklient.testClientMotLokalServer


fun main() {
    val saksnummer = nesteSaksnummer()
    val grunnlag1 = lagGrunnlag(saksnummer = saksnummer, periode = "2020-01-01/2020-01-10")

    testClientMotLokalServer().opprettUttaksplan(grunnlag1)

    val grunnlag2 = lagGrunnlag(saksnummer = saksnummer, periode = "2020-01-05/2020-01-15")

    println("Opprettet uttaksplan for behandlingUUID = ${grunnlag1.behandlingUUID}")

    testClientMotLokalServer().opprettUttaksplan(grunnlag2)

    println("Opprettet uttaksplan for behandlingUUID = ${grunnlag2.behandlingUUID}")
}

