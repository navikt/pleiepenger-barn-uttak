package no.nav.pleiepengerbarn.uttak.scenario

import no.nav.pleiepengerbarn.uttak.testklient.lagGrunnlag
import no.nav.pleiepengerbarn.uttak.testklient.testClientMotLokalServer
import java.time.LocalDate
import kotlin.random.Random

fun main() {
    val startenPåHeleTesten = System.currentTimeMillis()

    val behandlinger = mutableSetOf<String>()

    for (i in 1..10000) {
        val fom = LocalDate.now().plusDays(Random.nextInt(0, 99).toLong())
        val tom = fom.plusDays(Random.nextInt(0, 99).toLong())
        val grunnlag = lagGrunnlag(saksnummer = Random.nextInt(0, 999).toString(), periode = "$fom/$tom")
        val start = System.currentTimeMillis()
        testClientMotLokalServer().opprettUttaksplan(grunnlag)
        val end = System.currentTimeMillis()
        println("Opprettet uttaksplan for behandlingUUID = ${grunnlag.behandlingUUID} index=$i ${end-start} ms")
        behandlinger.add(grunnlag.behandlingUUID)
    }

    behandlinger.forEach {
        val start = System.currentTimeMillis()
        testClientMotLokalServer().hentUttaksplan(it)
        val end = System.currentTimeMillis()
        println("Hentet uttaksplan for behandlingUUID = $it ${end-start} ms")
    }

    println("Alt tok ${System.currentTimeMillis()-startenPåHeleTesten} ms")


}

