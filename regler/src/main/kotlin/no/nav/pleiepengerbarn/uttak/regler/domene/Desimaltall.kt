package no.nav.pleiepengerbarn.uttak.regler.domene

import java.math.BigDecimal
import java.time.Duration
import java.math.RoundingMode.HALF_UP

internal class Desimaltall(bigDecimal: BigDecimal) {
    internal companion object {
        private const val ResultatScale = 2
        private const val BeregningScale = 10
        private val RoundingMode = HALF_UP

        internal fun fraBigDecimal(bigDecimal: BigDecimal) = Desimaltall(bigDecimal)
        internal fun fraDouble(double: Double) = Desimaltall(BigDecimal.valueOf(double))
        internal fun fraDuration(duration: Duration) = Desimaltall(BigDecimal.valueOf(duration.toMillis()))

        internal val En = fraDouble(1.00)
        internal val Null = fraDouble(0.00)
        internal val EtHundre = fraDouble(100.00)
    }

    // Gjør at vi kan stole på bruk av == mellom Desimaltall
    override fun equals(other: Any?): Boolean {
        return if (other == null || other !is Desimaltall) false
        else this.resultat.compareTo(other.resultat) == 0
    }
    override fun hashCode() = resultat?.hashCode() ?: 0
    override fun toString(): String = "${resultat.toPlainString()} (${beregning.toPlainString()})"

    // Bruke <=> mellom Desimaltall
    internal operator fun compareTo(other: Desimaltall) = beregning.compareTo(other.beregning)
    // Bruke + mellom Desimaltall
    internal operator fun plus(annen: Desimaltall) = Desimaltall(beregning.add(annen.beregning))
    // Bruke - mellom Desimaltall
    internal operator fun minus(annen: Desimaltall) = Desimaltall(beregning.subtract(annen.beregning))
    // Bruke * mellom Desimaltall
    internal operator fun times(annen: Desimaltall) = Desimaltall(beregning.multiply(annen.beregning))
    // Bruke / mellom Desimaltall
    internal operator fun div(annen: Desimaltall) = Desimaltall(beregning.divide(annen.beregning, BeregningScale, RoundingMode))

    internal infix fun maks(maks: Desimaltall) = if (this > maks) maks else this

    internal fun fraFaktorTilProsent() = this * EtHundre
    internal fun fraProsentTilFaktor() = this / EtHundre

    private fun tilLong() = beregning.longValueExact()
    internal fun tilDuration() : Duration {
        val millis = tilLong()
        return if (millis <= 0) Duration.ZERO
        else Duration.ofMillis(millis)
    }

    internal fun erNull() = equals(Null)
    internal fun erEtHundre() = equals(EtHundre)

    internal val resultat = bigDecimal.setScale(ResultatScale, RoundingMode)
    internal val beregning = bigDecimal.setScale(BeregningScale, RoundingMode)
}

// Kunne dele duration på en annen Duration
internal operator fun Duration.div(annen: Duration) = Desimaltall.fraDuration(this) / Desimaltall.fraDuration(annen)
// Unngå å måtte bruke dividedBy på Duration
internal operator fun Duration.div(long: Long) = dividedBy(long)
// Unngå å måtte bruke multipliedBy på Duration
internal operator fun Duration.times(long: Long) = multipliedBy(long)
// Gjøre en BigDecimal til Desimaltall
internal fun BigDecimal.somDesimaltall() = Desimaltall.fraBigDecimal(this)