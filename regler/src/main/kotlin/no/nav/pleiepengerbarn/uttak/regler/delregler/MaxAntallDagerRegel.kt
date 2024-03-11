package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.HUNDRE_PROSENT
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.virkedager
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

internal class MaxAntallDagerRegel : UttaksplanRegel {

    companion object {
        val KVOTER = mapOf(
            YtelseType.PLS to 60
        )
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun kjør(uttaksplan: Uttaksplan, grunnlag: RegelGrunnlag): Uttaksplan {
        if (grunnlag.ytelseType != YtelseType.PLS) {
            return uttaksplan
        }
        val maxDager = KVOTER[grunnlag.ytelseType] ?: throw IllegalArgumentException("Ulovlig ytelsestype ${grunnlag.ytelseType}")

        val (forBrukteDagerHittil, maxDatoHittil) = grunnlag.finnForbrukteDagerHittil()

        var rest = BigDecimal(maxDager) - forBrukteDagerHittil

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        uttaksplan.perioder.forEach { (periode, info) ->
            if (info.utfall == Utfall.OPPFYLT) {
                val uttaksgrad = info.uttaksgrad.divide(HUNDRE_PROSENT, 2, RoundingMode.HALF_UP)
                val forbrukteDagerDennePerioen = BigDecimal(periode.virkedager()) * uttaksgrad

                if (rest <= BigDecimal.ZERO) {
                    // ingenting igjen på kvoten
                    nyePerioder[periode] = info.settIkkeoppfylt()
                } else if (forbrukteDagerDennePerioen <= rest) {
                    // nok dager igjen, setter hele perioden til oppfylt
                    nyePerioder[periode] = info
                    rest -= forbrukteDagerDennePerioen
                } else {
                    // delvis nok dager igjen.
                    val restHeleVirkedager = rest.divide(uttaksgrad, 0, RoundingMode.DOWN).toInt() // ingen #div/0, treffer kode over om det er 0 uttaksgrad
                    rest -= uttaksgrad * BigDecimal(restHeleVirkedager)

                    val fårDagerHeltOppfylt = restHeleVirkedager > 0
                    val fårDagMedDelvisOppfylt = rest > Prosent.ZERO

                    var nestePeriodeFom = periode.fom;
                    if (fårDagerHeltOppfylt) {
                        val tomInnvilget = if (fårDagMedDelvisOppfylt)
                            plussVirkedager(periode.fom, restHeleVirkedager).minusDays(1) //for å ta med helg når påfølgende mandag innvilges delvis
                        else
                            plussVirkedager(periode.fom, restHeleVirkedager - 1)
                        nyePerioder[LukketPeriode(periode.fom, tomInnvilget)] = info
                        nestePeriodeFom = tomInnvilget.plusDays(1)
                    }
                    if (fårDagMedDelvisOppfylt) {
                        val restIProsenter = BigDecimal.valueOf(100) * rest
                        nyePerioder[LukketPeriode(nestePeriodeFom, nestePeriodeFom)] = info.settDelvisOppfyltAvkortetMotKvote(restIProsenter)
                        rest = BigDecimal.ZERO
                        nestePeriodeFom = nestePeriodeFom.plusDays(1)
                    }
                    if (nestePeriodeFom <= periode.tom) {
                        nyePerioder[LukketPeriode(nestePeriodeFom, periode.tom)] = info.settIkkeoppfylt()
                    }
                }
            } else {
                // Gjør ingenting med perioder som ikke er oppfylt
                nyePerioder[periode] = info
            }
        }
        val forbrukteDager = nyePerioder.finnForbrukteDager().first
        val totaltForbruktKvote = forbrukteDager + forBrukteDagerHittil
        val kvoteInfo = KvoteInfo(
            maxDato = skalKunSetteMaxDatoHvisKvotenErbruktOpp(
                forBrukteDagerHittil,
                maxDatoHittil,
                BigDecimal(maxDager).setScale(2)
            ),
            totaltForbruktKvote = totaltForbruktKvote
        )

        if (forbrukteDager > BigDecimal.valueOf(61)) {
            throw java.lang.IllegalStateException("Forbrukt kvote for denne behandlingen er mer enn 61 dager. $forbrukteDager antall forbrukte dager.")
        }

        if (forbrukteDager > BigDecimal.valueOf(60) && forbrukteDager < BigDecimal.valueOf(61)) {
            logger.info("Forbrukt kvote for denne behandlingen er mellom 60 og 61 dager: $forbrukteDager antall forbrukte dager.")
        }

        if (totaltForbruktKvote >= BigDecimal.valueOf(61)) {
            logger.info("Totalt forbrukt kvote er mer enn 61 dager: $totaltForbruktKvote")
        }

        return uttaksplan.copy(perioder = nyePerioder, kvoteInfo = kvoteInfo)
    }

    private fun plussVirkedager(inputDato: LocalDate, antallVirkedager: Int): LocalDate {
        var restVirkedager = antallVirkedager
        var dato = inputDato
        while (restVirkedager > 0) {
            dato = dato.plusDays(1);
            if (dato.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                restVirkedager--;
            }
        }
        return dato
    }

    private fun skalKunSetteMaxDatoHvisKvotenErbruktOpp(
        forBrukteDagerHittil: BigDecimal,
        maxDatoHittil: LocalDate?,
        maxDager: BigDecimal
    ): LocalDate? {
        if (maxDatoHittil == null) {
            return null
        } else if (forBrukteDagerHittil == maxDager || forBrukteDagerHittil > maxDager) {
            return maxDatoHittil
        }
        return null
    }

}

private fun UttaksperiodeInfo.settIkkeoppfylt(): UttaksperiodeInfo {
    return this.copy(
        årsaker = setOf(Årsak.MAKS_DAGER_OVERSTEGET),
        utfall = Utfall.IKKE_OPPFYLT,
        inngangsvilkår = inngangsvilkår,
        uttaksgrad = Prosent(0).setScale(2, RoundingMode.HALF_UP),
        utbetalingsgrader = this.utbetalingsgrader.map {
            it.copy(
                utbetalingsgrad = Prosent(0).setScale(
                    2,
                    RoundingMode.HALF_UP
                )
            )
        }
    )
}

private fun UttaksperiodeInfo.settDelvisOppfyltAvkortetMotKvote(uttaksgrad: Prosent): UttaksperiodeInfo {
    check(uttaksgrad > Prosent.ZERO) { "Uttakgrad må være over 0 for delvis oppfylt, var $uttaksgrad" }
    check(uttaksgrad < Prosent.valueOf(100)) { "Uttakgrad må være under 100% for delvis oppfylt, var $uttaksgrad" }
    check(uttaksgrad == uttaksgrad.setScale(2, RoundingMode.UP)) { "Uttaksgrad skal være avrundet til 2 desimaler" }
    return this.copy(
        årsaker = setOf(Årsak.AVKORTET_MOT_KVOTE),
        utfall = Utfall.OPPFYLT,
        uttaksgrad = uttaksgrad,
        utbetalingsgrader = this.utbetalingsgrader.map {
            it.copy(
                utbetalingsgrad = uttaksgrad
            )
        }
    )
}

private fun RegelGrunnlag.finnForbrukteDagerHittil(): Pair<BigDecimal, LocalDate?> {
    var antallDager = BigDecimal.ZERO
    val relevantePerioder = mutableMapOf<LukketPeriode, UUID>()

    this.kravprioritetForBehandlinger.forEach { (kravprioritetsperiode, behandlingsUUIDer) ->
        for (behandlingUUID in behandlingsUUIDer) {
            if (behandlingUUID != this.behandlingUUID) {
                //Skal ikke telle med nåværende behandling

                val annenPartsUttaksplan = this.andrePartersUttaksplanPerBehandling[behandlingUUID]
                    ?: continue // Uttaksplan kan mangle ved simulering.

                annenPartsUttaksplan.perioder.forEach { (annenPartsPeriode, info) ->
                    if (annenPartsPeriode.overlapperDelvis(kravprioritetsperiode)) {
                        if (relevantePerioder.containsKey(annenPartsPeriode) && relevantePerioder[annenPartsPeriode] == behandlingUUID) {
                            // Ignorer.
                        } else if (info.utfall == Utfall.OPPFYLT) {
                            antallDager += (info.uttaksgrad.divide(HUNDRE_PROSENT, 2, RoundingMode.HALF_UP) * BigDecimal(annenPartsPeriode.virkedager()))
                            relevantePerioder[annenPartsPeriode] = behandlingUUID
                        }
                    }
                }
            } else {
                val forrigePlan = forrigeUttaksplan ?: continue

                if (this.søktUttak.any { søktUttak -> søktUttak.periode.overlapperDelvis(kravprioritetsperiode) }) {
                    continue
                }

                antallDager += forrigePlan.perioder.filter {
                    it.key.overlapperDelvis(kravprioritetsperiode)
                }.finnForbrukteDager().first
            }
        }
    }

    val maxDatoHittil = relevantePerioder.maxOfOrNull { it.key.tom }

    return Pair(antallDager, maxDatoHittil)
}

private fun Map<LukketPeriode, UttaksperiodeInfo>.finnForbrukteDager(): Pair<BigDecimal, List<LukketPeriode>> {
    var antallDager = BigDecimal.ZERO
    val relevantePerioder = mutableListOf<LukketPeriode>()

    this.forEach { (periode, info) ->
        if (info.utfall == Utfall.OPPFYLT) {
            val uttaksgrad = info.uttaksgrad.divide(HUNDRE_PROSENT, 2, RoundingMode.HALF_UP)
            antallDager += uttaksgrad * BigDecimal(periode.virkedager())
            relevantePerioder.add(periode)
        }
    }
    return Pair(antallDager, relevantePerioder)
}
