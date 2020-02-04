package no.nav.pleiepengerbarn.uttak.kontrakter

import org.threeten.extra.YearWeek
import java.time.DayOfWeek
import java.time.LocalDate

data class Uke(val ukeNr: Int, val år: Int) {

    fun fom(): LocalDate {
        return YearWeek.of(år, ukeNr).atDay(DayOfWeek.MONDAY)
    }

    fun tom(): LocalDate {
        return YearWeek.of(år, ukeNr).atDay(DayOfWeek.SUNDAY)
    }

}
