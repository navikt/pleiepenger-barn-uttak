package no.nav.pleiepengerbarn.uttak.kontrakter

import java.time.LocalDate

data class Knekkpunkt(val knekk:LocalDate, val typer:Set<KnekkpunktType>)