package no.nav.pleiepengerbarn.uttak.regler

import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.lang.IllegalArgumentException
import java.util.*


fun Uttaksgrunnlag.sjekk(): Set<Valideringsfeil> {
    val valideringsfeil = mutableSetOf<Valideringsfeil>()
    // Sjekk andre parters saksnummer
    andrePartersSaksnummer.apply { if (this.toSet().size != this.size) valideringsfeil.add(Valideringsfeil.ANDRE_PARTERS_SAKSNUMMER_DUPLIKAT) }
    // Sjekk behandling UUID
    sjekkUUID(behandlingUUID) {valideringsfeil.add(Valideringsfeil.BEHANDLING_UUID_FORMATFEIL)}
    // Sjekk arbeid
    sjekkUnikeArbeidsforhold(arbeid) {valideringsfeil.add(Valideringsfeil.ARBEIDSFORHOLD_ER_IKKE_UNIKE)}
    sjekkOverlappArbeidsperioder(arbeid) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_ARBEIDSPERIODER)}
    // Sjekk innngangsvilkår
    sjekkOverlappInngangsvilkår(inngangsvilkår) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_INNGANGSVILKÅRPERIODER)}
    // Sjekk ferie
    if (sjekkOmDetFinnesOverlappendePerioder(lovbestemtFerie)) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_FERIEPERIODER)}
    // Sjekk pleiebehov
    if (sjekkOmDetFinnesOverlappendePerioder(pleiebehov.keys)) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_PLEIEBEHOVPERIODER)}
    // Sjekk tilsynsperioder
    if (sjekkOmDetFinnesOverlappendePerioder(tilsynsperioder.keys)) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_TILSYNSPERIODER)}
    // Sjekk søkt uttak
    sjekkSøktUttakOverlappendePerioder(søktUttak) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_SØKT_UTTAK)}
    // Sjekk beredskap
    if (sjekkOmDetFinnesOverlappendePerioder(beredskapsperioder.keys)) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_BEREDSKAPSPERIODER)}
    // Sjekk nattevåk
    if (sjekkOmDetFinnesOverlappendePerioder(nattevåksperioder.keys)) {valideringsfeil.add(Valideringsfeil.OVERLAPP_MELLOM_NATTEVÅKSPERIODER)}

    return valideringsfeil
}

private fun sjekkSøktUttakOverlappendePerioder(søktUttak: List<SøktUttak>, f: () -> Unit) {
    val perioder = søktUttak.map {it.periode}
    val overlappFinnes = sjekkOmDetFinnesOverlappendePerioder(perioder)
    if (overlappFinnes) f()
}

private fun sjekkOverlappInngangsvilkår(inngangsvilkår: Map<String, List<Vilkårsperiode>>, f: () -> Unit) {
    val overlappFinnes = inngangsvilkår.any { entry ->
        val perioder = entry.value.map {LukketPeriode(it.periode.fom, it.periode.tom)}
        sjekkOmDetFinnesOverlappendePerioder(perioder)
    }
    if (overlappFinnes) f()
}

private fun sjekkUUID(uuidStrig: String, f: () -> Unit) {
    try {
        UUID.fromString(uuidStrig)
    } catch(e: IllegalArgumentException) {
        f()
    }
}

private fun sjekkOverlappArbeidsperioder(arbeidListe: List<Arbeid>, f: () -> Unit) {
    val overlappFinnes = arbeidListe.any { sjekkOmDetFinnesOverlappendePerioder(it.perioder.map { entry -> entry.key}) }
    if (overlappFinnes) f()
}

private fun sjekkOmDetFinnesOverlappendePerioder(perioder: Collection<LukketPeriode>): Boolean {
    val segmenter = perioder.map {LocalDateSegment(it.fom, it.tom, true)}
    try {
        LocalDateTimeline(segmenter)
    } catch (e: IllegalArgumentException) {
        return true
    }
    return false
}

private fun sjekkUnikeArbeidsforhold(arbeidListe: List<Arbeid>, f: () -> Unit) {
    val antallUnikeArbeidsforhold = arbeidListe.map {it.arbeidsforhold} .toSet().size
    if (arbeidListe.size != antallUnikeArbeidsforhold) {
        f()
    }
}






