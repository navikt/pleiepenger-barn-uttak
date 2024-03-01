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
import java.util.UUID
import kotlin.math.min

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
        val maxDager =
            KVOTER[grunnlag.ytelseType] ?: throw IllegalArgumentException("Ulovlig ytelsestype ${grunnlag.ytelseType}")

        val (forBrukteDagerHittil, maxDatoHittil) = grunnlag.finnForbrukteDagerHittil()

        var rest = BigDecimal(maxDager) - forBrukteDagerHittil

        val nyePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()

        uttaksplan.perioder.forEach { (periode, info) ->
            if (info.utfall == Utfall.OPPFYLT) {
                val uttaksgrad = info.uttaksgrad.divide(HUNDRE_PROSENT, 2, RoundingMode.HALF_UP)
                val forbrukteDagerDennePerioen = BigDecimal(periode.virkedager()) * uttaksgrad

                if (rest <= BigDecimal.ZERO) {
                    // Hvis ingenting igjen på kvoten så må undersøke om det fremdeles kan innvilges
                    nyePerioder[periode] = info.settIkkeoppfylt()
                } else if (forbrukteDagerDennePerioen <= rest) {
                    // Hvis det er nok dager igjen, så settes hele periode til oppfylt
                    nyePerioder[periode] = info
                    rest -= forbrukteDagerDennePerioen
                } else {
                    // Bare delvis nok dager igjen, så deler derfor opp perioden i en oppfylt og en ikke oppfylt periode
                    val restVirkedagerGradert = rest.divide(uttaksgrad, 0, RoundingMode.UP).toInt() // ingen #div/0, treffer kode over om det er 0 uttaksgrad
                    val tomInnvilget = plussVirkedager(periode.fom, restVirkedagerGradert-1)
                    check(tomInnvilget <= periode.tom) { "Post-condition feilet: tomInnvilget $tomInnvilget er utenfor $periode. Uttaksgrad var $uttaksgrad og rest var $rest" }
                    nyePerioder[LukketPeriode(periode.fom, tomInnvilget)] = info
                    if (tomInnvilget < periode.tom) {
                        nyePerioder[LukketPeriode(tomInnvilget.plusDays(1), periode.tom)] = info.settIkkeoppfylt()
                    }
                    rest = BigDecimal.ZERO
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
                            antallDager += (info.uttaksgrad.divide(
                                HUNDRE_PROSENT,
                                2,
                                RoundingMode.HALF_UP
                            ) * BigDecimal(annenPartsPeriode.virkedager()))
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

    this.forEach { (annenPartsPeriode, info) ->
        if (info.utfall == Utfall.OPPFYLT) {
            antallDager += (info.uttaksgrad.divide(HUNDRE_PROSENT, 2, RoundingMode.HALF_UP) * BigDecimal(
                annenPartsPeriode.virkedager()
            ))
            relevantePerioder.add(annenPartsPeriode)
        }
    }
    return Pair(antallDager, relevantePerioder)
}
