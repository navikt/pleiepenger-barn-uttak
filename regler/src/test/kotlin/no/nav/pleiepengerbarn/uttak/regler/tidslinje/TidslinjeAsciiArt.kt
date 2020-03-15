package no.nav.pleiepengerbarn.uttak.regler.tidslinje

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.erLikEllerFør
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.LinkedHashSet

internal object TidslinjeAsciiArt {
    private val decimalFormat = DecimalFormat().apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 1
    }

    internal fun printTidslinje(tidslinjer: LinkedHashSet<Tidslinje>){
        val førsteDag = tidslinjer.førsteDato()
        val sisteDag = tidslinjer.sisteDato()

        println()
        println("- - - - - - - - - - - - - -")
        println("Fra og med ${førsteDag.dayOfWeek.name} $førsteDag")
        println("Til og med ${sisteDag.dayOfWeek.name} $sisteDag")
        println()

        printHeader(førsteDag, sisteDag)

        tidslinjer.forEach {
            it.beskrivelse.printBeskrivelse()
            it.perioder.print(førsteDag)
            println()
        }
    }

    private fun printDivider() = print("|")

    private fun printHeader(
            førsteDato: LocalDate,
            sisteDato: LocalDate) {
        "".printBeskrivelse()
        var current = førsteDato
        while (current.erLikEllerFør(sisteDato)) {
            printDivider()
            print("  ")
            print(current.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0,1))
            print("  ")
            current = current.plusDays(1)

        }
        printDivider()
        println()
    }

    private fun String.printBeskrivelse() {
        if (length > 20) print(substring(0, 20))
        else print(padEnd(20, ' '))
    }

    private fun Prosent.padded() = decimalFormat.format(this).padEnd(5, '-')

    private fun LukketPeriode.print(grad: Prosent?) {
        var current = fom
        var print = ""
        while(current.erLikEllerFør(tom)) {
            print += "------"
            current = current.plusDays(1)
        }
        if (print.isEmpty()) return
        if (grad != null) print = print.replaceFirst("-----", grad.padded())
        print(print.substring(0, print.length - 1))
        printDivider()
    }

    private fun LukketPeriode.tomPeriode(inkluderFraOgMed: Boolean = false) {
        var antallDager = ChronoUnit.DAYS.between(fom, tom).toInt()
        if (!inkluderFraOgMed) antallDager--

        if (antallDager == 0) return
        var print = ""

        for (x in 1..antallDager) {
            print += "      "
        }

        print(print.substring(1, print.length))
        printDivider()
    }

    private fun Map<LukketPeriode, Prosent?>.print(førsteDato: LocalDate) {
        var current = førsteDato
        printDivider()
        forEach { periode, grad ->
            LukketPeriode(current, periode.fom).tomPeriode(current.isEqual(førsteDato))
            periode.print(grad)
            current = periode.tom
        }
    }
}

private fun LinkedHashSet<Tidslinje>.førsteDato(): LocalDate {
    var førsteDato = LocalDate.MAX
    forEach {
        it.perioder.forEach { (periode, _) ->
            if (periode.fom.isBefore(førsteDato)) {
                førsteDato = periode.fom
            }
        }
    }
    return førsteDato
}
private fun LinkedHashSet<Tidslinje>.sisteDato(): LocalDate {
    var sisteDato = LocalDate.MIN
    forEach {
        it.perioder.forEach { (periode, _) ->
            if (periode.tom.isAfter(sisteDato)) {
                sisteDato = periode.tom
            }
        }
    }
    return sisteDato
}