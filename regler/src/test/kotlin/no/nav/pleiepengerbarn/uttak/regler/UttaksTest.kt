package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Periode
import no.nav.pleiepengerbarn.uttak.regler.tidslinje.Tidslinje
import no.nav.pleiepengerbarn.uttak.regler.tidslinje.TidslinjeAsciiArt
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class UttaksTest {
    @Test
    fun `manuelle tidslinjer`() {
        val start = LocalDate.parse("2020-02-04")
        val slutt = start.plusDays(15)

        TidslinjeAsciiArt.printTidslinje(linkedSetOf(
                Tidslinje("Tilsynbehov", mapOf(
                        Periode(start, start.plusDays(7)) to BigDecimal(200),
                        Periode(start.plusDays(8), slutt) to BigDecimal(100)
                )),
                Tidslinje("Mors uttak", mapOf(
                        Periode(start.plusDays(1), start.plusDays(9)) to BigDecimal(100),
                        Periode(start.plusDays(10), start.plusDays(15)) to BigDecimal.valueOf(80)
                )),
                Tidslinje("Søknadsperioder", mapOf(
                        Periode(start.plusDays(2), start.plusDays(14)) to null
                )),
                Tidslinje("Ikke medlem", mapOf(
                        Periode(start.plusDays(11), start.plusDays(14)) to null
                )),
                Tidslinje("Lovbestemt ferie", mapOf(
                        Periode(start.plusDays(6), start.plusDays(7)) to null
                )),
                Tidslinje("Arb.g. 1", mapOf(
                        Periode(start.plusDays(5), start.plusDays(10)) to BigDecimal(30.5)
                )),
                Tidslinje("Arb.g. 2", mapOf(
                        Periode(start.plusDays(6), start.plusDays(13)) to BigDecimal(30.0)
                )),
                Tidslinje("Resultat", mapOf(
                        Periode(start.plusDays(2), start.plusDays(4)) to BigDecimal(100),
                        Periode(start.plusDays(6), start.plusDays(7)) to BigDecimal(0),
                        Periode(start.plusDays(8), start.plusDays(9)) to BigDecimal(0),
                        Periode(start.plusDays(10), start.plusDays(10)) to BigDecimal(20),
                        Periode(start.plusDays(11), start.plusDays(14)) to BigDecimal(0)
                ))
        ))
    }
}