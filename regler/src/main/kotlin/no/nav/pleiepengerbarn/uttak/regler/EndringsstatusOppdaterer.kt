package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis

object EndringsstatusOppdaterer {

    fun oppdater(forrigeUttaksplan: Uttaksplan?, nyUttaksplan: Uttaksplan): Uttaksplan {

        val oppdatertePerioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        nyUttaksplan.perioder.forEach { (periode, infoFraNyUttaksplan) ->

            val periodeFraForrigeUttaksplan = forrigeUttaksplan?.perioder?.keys?.firstOrNull { periode.overlapperDelvis(it) }

            val endringsstatus = if (periodeFraForrigeUttaksplan != null) {
                val infoFraForrigeUttaksplan = forrigeUttaksplan.perioder[periodeFraForrigeUttaksplan]
                if (infoFraNyUttaksplan.sammenlign(infoFraForrigeUttaksplan!!)) {
                    Endringsstatus.UENDRET
                } else {
                    Endringsstatus.ENDRET
                }
            } else {
                Endringsstatus.NY
            }
            oppdatertePerioder[periode] = infoFraNyUttaksplan.copy(endringsstatus = endringsstatus)
        }

        return Uttaksplan(perioder = oppdatertePerioder)
    }

}

/**
 * Sammenlign uttakperiode info uten å ta med felter som ikke er viktige ifm setting av uttaksperiodetype.
 */
private fun UttaksperiodeInfo.sammenlign(infoFraForrige: UttaksperiodeInfo) =
    this.nullstillUviktigeFelt() == infoFraForrige.nullstillUviktigeFelt()


/**
 * Setter alle BigDecimal felter til 2 desimaler og nullstiller felter som ikke er viktig for sammenligning av perioder ifm setting av uttaksperiodetype.
 */
private fun UttaksperiodeInfo.nullstillUviktigeFelt(): UttaksperiodeInfo {
    val oppdaterteUtbetalingsgrader = this.utbetalingsgrader.map { it.copy(utbetalingsgrad = it.utbetalingsgrad.setScale(2)) }
    val oppdatertGraderingMotTilsyn = if (this.graderingMotTilsyn != null) {
        val gmt = this.graderingMotTilsyn!!
        gmt.copy(
            etablertTilsyn = gmt.etablertTilsyn.setScale(2),
            andreSøkeresTilsyn = gmt.andreSøkeresTilsyn.setScale(2),
            tilgjengeligForSøker = gmt.tilgjengeligForSøker
        )
    } else {
        null
    }
    return this.copy(
        kildeBehandlingUUID = "",
        knekkpunktTyper = setOf(),
        endringsstatus = null,
        uttaksgrad = this.uttaksgrad.setScale(2),
        søkersTapteArbeidstid = this.søkersTapteArbeidstid?.setScale(2),
        pleiebehov = this.pleiebehov.setScale(2),
        utbetalingsgrader = oppdaterteUtbetalingsgrader,
        graderingMotTilsyn = oppdatertGraderingMotTilsyn
    )
}

