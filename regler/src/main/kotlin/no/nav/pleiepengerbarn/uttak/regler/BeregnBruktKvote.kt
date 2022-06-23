package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*
import java.util.*

object BeregnBruktKvote {

    internal val FULL_DAG = Duration.ofHours(7).plusMinutes(30)
    internal val MAX_KVOTE = BigDecimal.valueOf(60)

    fun erKvotenOversteget(uttaksplan: Uttaksplan, andreParter: Map<UUID, Uttaksplan>): Pair<Boolean, BigDecimal> {
        val altUttak = mutableListOf<Uttaksplan>()
        altUttak.addAll(andreParter.values.toMutableList())
        altUttak.add(uttaksplan)

        val antallDagerBrukt = regnUtAntallDagerBrukt(altUttak)
        return Pair(erKvotenOversteget(antallDagerBrukt), antallDagerBrukt)
    }

    private fun regnUtAntallDagerBrukt(altUttak: List<Uttaksplan>): BigDecimal {
        val totaltBruktKvote = leggSammenUttak(altUttak)
        return BigDecimal.valueOf(totaltBruktKvote.toMillis()).setScale(2).divide(BigDecimal.valueOf(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP)
    }

    private fun erKvotenOversteget(antallDagerBrukt: BigDecimal): Boolean {
        val resterendeKvote = MAX_KVOTE.minus(antallDagerBrukt)
        return (resterendeKvote < BigDecimal.ZERO)
    }

    private fun leggSammenUttak(altUttak: List<Uttaksplan>): Duration {
        var totalTid = Duration.ZERO
        altUttak.forEach {
            it.perioder.forEach { (periode, info) ->
                totalTid += if (info.oppgittTilsyn != null) {
                    info.oppgittTilsyn
                } else {
                    leggSammenUttakPeriode(periode)
                }
            }
        }
        return totalTid
    }

    private fun leggSammenUttakPeriode(periode: LukketPeriode): Duration {
        var totalTid = Duration.ZERO
        var dato = periode.fom
        while (dato <= periode.tom) {
            if (dato.ukedag()) totalTid += FULL_DAG
            dato = dato.plusDays(1)
        }
        return totalTid
    }

    private fun LocalDate.ukedag() = (this.dayOfWeek == DayOfWeek.MONDAY || this.dayOfWeek == DayOfWeek.TUESDAY ||
            this.dayOfWeek == DayOfWeek.WEDNESDAY || this.dayOfWeek == DayOfWeek.THURSDAY || this.dayOfWeek == DayOfWeek.FRIDAY)
}