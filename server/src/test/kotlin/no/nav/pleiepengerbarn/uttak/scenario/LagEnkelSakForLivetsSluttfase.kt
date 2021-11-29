package no.nav.pleiepengerbarn.uttak.scenario

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode
import no.nav.pleiepengerbarn.uttak.testklient.lagLivetsSluttfaseGrunnlag
import no.nav.pleiepengerbarn.uttak.testklient.testClientMotLokalServer

fun main() {
    val grunnlag = lagLivetsSluttfaseGrunnlag(periode = "2020-01-06/2020-01-10")
            .copy(inngangsvilkår = mapOf("FP_VK_2" to listOf(Vilkårsperiode(LukketPeriode("2020-01-06/2020-01-10"), Utfall.OPPFYLT))))
    testClientMotLokalServer().opprettUttaksplan(grunnlag)
    println("Opprettet uttaksplan for behandlingUUID = ${grunnlag.behandlingUUID}")
}