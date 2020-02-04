package no.nav.pleiepengerbarn.uttak.regler.tidslinje

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.text.DecimalFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.LinkedHashSet

internal object TidslinjeAsciiArt {
    private val df = DecimalFormat().apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 1
    }

    internal fun printTidslinje(tidslinjer: LinkedHashSet<Tidslinje>){
        val førsteHverdag = tidslinjer.førsteDato().førsteHverdag()
        val sisteHverdag = tidslinjer.sisteDato().sisteHverdag()

        println("Fra og med ${førsteHverdag.dayOfWeek.name} $førsteHverdag")
        println("Til og med ${sisteHverdag.dayOfWeek.name} $sisteHverdag")
        println()

        printHeader(førsteHverdag, sisteHverdag)

        tidslinjer.forEach {
            it.beskrivelse.printBeskrivelse()
            it.perioder.print(førsteHverdag)
            println()
        }
    }

    private fun printDivider() = print("|")

    private fun printHeader(førsteDato: LocalDate, sisteDato: LocalDate) {
        "".printBeskrivelse()
        var current = førsteDato
        while (current.isBefore(sisteDato) || current.isEqual(sisteDato)) {
            if (current.erHverdag()) {
                printDivider()
                print("  ")
                print(current.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0,1))
                print("  ")
            }
            current = current.plusDays(1)

        }
        printDivider()
        println()
    }

    private fun String.printBeskrivelse() {
        if (length > 20) print(substring(0, 20))
        else print(padEnd(20, ' '))
    }

    private fun LocalDate.erHverdag() = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

    private fun Grad.padded() = df.format(this).padEnd(5, '-')

    private fun Periode.print(grad: Grad?) {
        var current = fraOgMed
        var print = ""
        while(current.isBefore(tilOgMed) || current.isEqual(tilOgMed)) {
            if (current.erHverdag()) print += "------"
            current = current.plusDays(1)
        }
        if (print.isEmpty()) return
        if (grad != null) print = print.replaceFirst("-----", grad.padded())
        print(print.substring(0, print.length - 1))
        printDivider()
    }

    private fun LocalDate.førsteHverdag() : LocalDate {
        var current = this
        while (!current.erHverdag()) {
            current = current.plusDays(1)
        }
        return current
    }

    private fun LocalDate.sisteHverdag() : LocalDate {
        var current = this
        while (!current.erHverdag()) {
            current = current.minusDays(1)
        }
        return current
    }

    private fun Periode.antallHverdagerMellom(inkluderFraOgMed: Boolean = false) : Int {
        var antall = 0
        val førsteHverdag = fraOgMed.førsteHverdag()
        val andreHverdag = førsteHverdag.plusDays(1).førsteHverdag()
        var current = if (inkluderFraOgMed) førsteHverdag else andreHverdag
        while (current.isBefore(tilOgMed)) {
            if (current.erHverdag()) antall++
            current = current.plusDays(1)
        }
        return antall
    }

    private fun Periode.tomPeriode(inkluderFraOgMed: Boolean = false) {
        val antallHverdager = antallHverdagerMellom(inkluderFraOgMed)
        if (antallHverdager == 0) return
        var print = ""

        for (x in 1..antallHverdager) {
            print += "      "
        }

        print(print.substring(1, print.length))
        printDivider()
    }

    private fun Map<Periode, Grad?>.print(førsteDato: LocalDate) {
        var current = førsteDato
        printDivider()
        forEach { periode, grad ->
            Periode(current, periode.fraOgMed).tomPeriode(current.isEqual(førsteDato))
            periode.print(grad)
            current = periode.tilOgMed
        }
    }
}

private fun LinkedHashSet<Tidslinje>.førsteDato(): LocalDate {
    var førsteDato = LocalDate.MAX
    forEach {
        it.perioder.forEach { (periode, _) ->
            if (periode.fraOgMed.isBefore(førsteDato)) {
                førsteDato = periode.fraOgMed
            }
        }
    }
    return førsteDato
}
private fun LinkedHashSet<Tidslinje>.sisteDato(): LocalDate {
    var sisteDato = LocalDate.MIN
    forEach {
        it.perioder.forEach { (periode, _) ->
            if (periode.tilOgMed.isAfter(sisteDato)) {
                sisteDato = periode.tilOgMed
            }
        }
    }
    return sisteDato
}