package no.nav.pleiepengerbarn.uttak.regler.domene

import java.math.BigDecimal
import java.time.Duration
import java.math.RoundingMode.HALF_UP

internal class Desimal(bigDecimal: BigDecimal) {
    internal companion object {
        internal const val Scale = 2
        internal val RoundingMode = HALF_UP

        internal fun fraBigDecimal(bigDecimal: BigDecimal) = Desimal(bigDecimal)
        internal fun fraDouble(double: Double) = Desimal(BigDecimal.valueOf(double))
        internal fun fraDuration(duration: Duration) = Desimal(BigDecimal.valueOf(duration.toMillis()))

        private val Null = fraDouble(0.00)
        private val EtHundre = fraDouble(100.00)
        private val ToHundre = fraDouble(200.00)
    }

    // Gjør at vi kan stole på bruk av == mellom Desimal
    override fun equals(other: Any?): Boolean {
        return if (other == null || other !is Desimal) false
        else compareTo(other) == 0
    }
    override fun hashCode() = value?.hashCode() ?: 0
    override fun toString(): String = value.toPlainString()

    // Bruke <=> mellom Desimal
    internal operator fun compareTo(other: Desimal) = value.compareTo(other.value)
    // Bruke + mellom Desimal
    internal operator fun plus(annen: Desimal) = Desimal(value.add(annen.value))
    // Bruke - mellom Desimal
    internal operator fun minus(annen: Desimal) = Desimal(value.subtract(annen.value))
    // Bruke * mellom Desimal
    internal operator fun times(annen: Desimal) = Desimal(value.multiply(annen.value))
    // Bruke / mellom Desimal
    internal operator fun div(annen: Desimal) = Desimal(value.divide(annen.value, Scale, RoundingMode))

    internal fun fraFaktorTilProsent() = this * EtHundre
    internal fun fraProsentTilFaktor() = this / EtHundre

    internal fun erNull() = equals(Null)
    internal fun erEtHundre() = equals(EtHundre)
    internal fun erToHundre() = equals(ToHundre)

    private val value = bigDecimal.setScale(Scale, RoundingMode)
}

// Kunne dele duration på en annen Duration
internal operator fun Duration.div(annen: Duration) = Desimal.fraDuration(this) / Desimal.fraDuration(annen)
// Unngå å måtte bruke dividedBy på Duration
internal operator fun Duration.div(long: Long) = dividedBy(long)