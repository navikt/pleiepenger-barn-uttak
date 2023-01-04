package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis
import java.math.RoundingMode
import java.time.Duration

object EndringsstatusOppdaterer {

    fun oppdater(forrigeUttaksplan: Uttaksplan?, nyUttaksplan: Uttaksplan): Uttaksplan {

        val oppdatertePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        nyUttaksplan.perioder.forEach { (periode, infoFraNyUttaksplan) ->

            val periodeFraForrigeUttaksplan =
                forrigeUttaksplan?.perioder?.keys?.firstOrNull { periode.overlapperDelvis(it) }

            val endringsstatus =
                utledEndringsstatus(periodeFraForrigeUttaksplan, forrigeUttaksplan, infoFraNyUttaksplan)
            oppdatertePerioder[periode] = infoFraNyUttaksplan.copy(endringsstatus = endringsstatus)
        }

        return nyUttaksplan.copy(perioder = oppdatertePerioder)
    }

    private fun utledEndringsstatus(
        periodeFraForrigeUttaksplan: LukketPeriode?,
        forrigeUttaksplan: Uttaksplan?,
        infoFraNyUttaksplan: UttaksperiodeInfo
    ): Endringsstatus {
        if (periodeFraForrigeUttaksplan != null) {
            val infoFraForrigeUttaksplan = forrigeUttaksplan!!.perioder[periodeFraForrigeUttaksplan]
            return if (infoFraNyUttaksplan.sammenlign(infoFraForrigeUttaksplan!!)) {
                Endringsstatus.UENDRET
            } else if (FeatureToggle.isActive("ENDRINGER_ENDRINGSUTLEDER")
                && infoFraNyUttaksplan.sammenlignUtenArbeidstid(infoFraForrigeUttaksplan!!)
            ) {
                Endringsstatus.UENDRET_RESULTAT
            } else {
                Endringsstatus.ENDRET
            }
        }
        return Endringsstatus.NY

    }

    /**
     * Sammenlign uttakperiode info uten å ta med felter som ikke er viktige ifm setting av uttaksperiodetype.
     */
    private fun UttaksperiodeInfo.sammenlign(infoFraForrige: UttaksperiodeInfo) =
        this.nullstillUviktigeFelt() == infoFraForrige.nullstillUviktigeFelt()

    /**
     * Sammenlign uttakperiode info uten å ta med felter som ikke er viktige ifm setting av uttaksperiodetype.
     */
    private fun UttaksperiodeInfo.sammenlignUtenArbeidstid(infoFraForrige: UttaksperiodeInfo) =
        this.nullstillUviktigeFeltUtvidet() == infoFraForrige.nullstillUviktigeFeltUtvidet()


    /**
     * Setter alle BigDecimal felter til 2 desimaler og nullstiller felter som ikke er viktig for sammenligning av perioder ifm setting av uttaksperiodetype.
     */
    private fun UttaksperiodeInfo.nullstillUviktigeFelt(): UttaksperiodeInfo {
        val oppdaterteUtbetalingsgrader =
            this.utbetalingsgrader.map {
                it.copy(
                    utbetalingsgrad = it.utbetalingsgrad.setScale(
                        2,
                        RoundingMode.HALF_UP
                    )
                )
            }
        if (FeatureToggle.isActive("ENDRINGER_ENDRINGSUTLEDER")) {
            return this.copy(
                // Nullstilles da disse feltene ikke er viktig for denne sammenligningen
                kildeBehandlingUUID = "",
                knekkpunktTyper = setOf(),
                endringsstatus = null,
                annenPart = AnnenPart.ALENE,
                // Setter alle grader til 2 desimaler slik at de kan sammenlignes.
                uttaksgrad = this.uttaksgrad.setScale(2, RoundingMode.HALF_UP),
                søkersTapteArbeidstid = this.søkersTapteArbeidstid?.setScale(2, RoundingMode.HALF_UP),
                pleiebehov = this.pleiebehov.setScale(2, RoundingMode.HALF_UP),
                utbetalingsgrader = oppdaterteUtbetalingsgrader,
                graderingMotTilsyn = null,
                oppgittTilsyn = null,
                beredskap = null,
                nattevåk = null,
                inngangsvilkår = mapOf(),
                utenlandsopphold = null,
                utenlandsoppholdUtenÅrsak = false
            )
        } else {
            val oppdatertGraderingMotTilsyn = if (this.graderingMotTilsyn != null) {
                val gmt = this.graderingMotTilsyn!!
                gmt.copy(
                    etablertTilsyn = gmt.etablertTilsyn.setScale(2, RoundingMode.HALF_UP),
                    andreSøkeresTilsyn = gmt.andreSøkeresTilsyn.setScale(2, RoundingMode.HALF_UP),
                    tilgjengeligForSøker = gmt.tilgjengeligForSøker.setScale(2, RoundingMode.HALF_UP)
                )
            } else {
                null
            }
            return this.copy(
                // Nullstilles da disse feltene ikke er viktig for denne sammenligningen
                kildeBehandlingUUID = "",
                knekkpunktTyper = setOf(),
                endringsstatus = null,
                annenPart = AnnenPart.ALENE,
                // Setter alle grader til 2 desimaler slik at de kan sammenlignes.
                uttaksgrad = this.uttaksgrad.setScale(2, RoundingMode.HALF_UP),
                søkersTapteArbeidstid = this.søkersTapteArbeidstid?.setScale(2, RoundingMode.HALF_UP),
                pleiebehov = this.pleiebehov.setScale(2, RoundingMode.HALF_UP),
                utbetalingsgrader = oppdaterteUtbetalingsgrader,
                graderingMotTilsyn = oppdatertGraderingMotTilsyn,
            )
        }
    }

    /**
     * Setter alle BigDecimal felter til 2 desimaler og nullstiller felter som ikke er viktig for sammenligning av perioder ifm setting av uttaksperiodetype.
     */
    private fun UttaksperiodeInfo.nullstillUviktigeFeltUtvidet(): UttaksperiodeInfo {
        val oppdaterteUtbetalingsgrader =
            this.utbetalingsgrader.map {
                it.copy(
                    utbetalingsgrad = it.utbetalingsgrad.setScale(
                        2,
                        RoundingMode.HALF_UP
                    ),
                    faktiskArbeidstid = Duration.ZERO,
                    normalArbeidstid = Duration.ZERO
                )
            }
        return this.copy(
            // Nullstilles da disse feltene ikke er viktig for denne sammenligningen
            kildeBehandlingUUID = "",
            knekkpunktTyper = setOf(),
            endringsstatus = null,
            annenPart = AnnenPart.ALENE,
            // Setter alle grader til 2 desimaler slik at de kan sammenlignes.
            uttaksgrad = this.uttaksgrad.setScale(2, RoundingMode.HALF_UP),
            søkersTapteArbeidstid = this.søkersTapteArbeidstid?.setScale(2, RoundingMode.HALF_UP),
            pleiebehov = this.pleiebehov.setScale(2, RoundingMode.HALF_UP),
            utbetalingsgrader = oppdaterteUtbetalingsgrader,
            graderingMotTilsyn = null,
            oppgittTilsyn = null,
            beredskap = null,
            nattevåk = null,
            inngangsvilkår = mapOf(),
            utenlandsopphold = null,
            utenlandsoppholdUtenÅrsak = false
        )
    }
}

