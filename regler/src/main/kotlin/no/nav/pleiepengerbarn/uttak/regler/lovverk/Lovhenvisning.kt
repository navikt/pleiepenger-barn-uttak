package no.nav.pleiepengerbarn.uttak.regler.lovverk

import no.nav.pleiepengerbarn.uttak.kontrakter.Anvendelse
import no.nav.pleiepengerbarn.uttak.kontrakter.Hjemmel

// https://uit.no/Content/275629/Veiledning%20om%20henvisninger%20m.m.%20i%20juridiske%20tekster.pdf
internal interface Lovhenvisning {
    fun anvend(anvendelse: Anvendelse) : Hjemmel
}

internal typealias Paragraf = String
internal typealias Ledd = Int
internal typealias Punktum = Int

internal fun Ledd.leddTilTekst() = "${somTekst()} ledd"
internal fun Punktum.punktumTilTekst() = "${somTekst()} punktum"
internal fun Int.somTekst() = when (this) {
    1 -> "første"
    2 -> "annet"
    3 -> "tredje"
    4 -> "fjerde"
    5 -> "femte"
    6 -> "sjette"
    else -> throw IllegalArgumentException("Støtter ikke $this")
}