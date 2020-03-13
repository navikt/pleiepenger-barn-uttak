package no.nav.pleiepengerbarn.uttak.kontrakter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate

data class LukketPeriode (
        val fom: LocalDate,
        val tom: LocalDate) {
    @get:JsonValue
    val iso8601: String = "$fom/$tom"

    private companion object {
        private fun fom(iso8601: String) = LocalDate.parse(iso8601.split("/")[0])
        private fun tom(iso8601: String) = LocalDate.parse(iso8601.split("/")[1])
    }

    @JsonCreator
    constructor(iso8601: String) : this(
            fom = fom(iso8601),
            tom = tom(iso8601)
    )

    override fun toString() = iso8601

    init {
        require(!fom.isAfter(tom)) {"Fom ($fom) må være før eller lik Tom ($tom)."}
    }
}