package no.nav.pleiepengerbarn.uttak.regler.domene

import no.nav.pleiepengerbarn.uttak.kontrakter.KnekkpunktType
import java.time.LocalDate

data class Knekkpunkt(val knekk: LocalDate, val typer:Set<KnekkpunktType>)