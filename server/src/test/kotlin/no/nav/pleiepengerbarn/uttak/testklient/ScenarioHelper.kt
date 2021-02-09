package no.nav.pleiepengerbarn.uttak.testklient

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal val FULL_DAG = Duration.ofHours(7).plusMinutes(30)
internal val INGENTING = Duration.ZERO

internal val HUNDREPROSENT = Prosent(100)

internal val HELE_2020 = LukketPeriode("2020-01-01/2020-12-31")

internal val ARBEIDSFORHOLD1 = Arbeidsforhold(type="arbeidsgiver", organisasjonsnummer = "123456789")
internal val ARBEIDSFORHOLD2 = Arbeidsforhold(type="arbeidsgiver", organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID().toString())
internal val ARBEIDSFORHOLD3 = Arbeidsforhold(type="arbeidsgiver", organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID().toString())
//internal val ARBEIDSFORHOLD4 = Arbeidsforhold(type="arbeidsgiver", organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID().toString())
//internal val ARBEIDSFORHOLD5 = Arbeidsforhold(type="arbeidsgiver", organisasjonsnummer = "987654321", arbeidsforholdId = UUID.randomUUID().toString())

internal fun lagGrunnlag(saksnummer: Saksnummer = nesteSaksnummer(), periode: String): Uttaksgrunnlag {
    val søknadsperiode = LukketPeriode(periode)
    return lagGrunnlag(
        saksnummer = saksnummer,
        søknadsperiode = søknadsperiode,
        arbeid = listOf(
            Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
        ),
        pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
    )
}

internal fun lagGrunnlag(
    søknadsperiode: LukketPeriode,
    arbeid: List<Arbeid>,
    pleiebehov: Map<LukketPeriode, Pleiebehov>,
    tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    søker: Søker = Søker(
        aktørId = "123",
        fødselsdato = LocalDate.parse("2000-01-01")
    ),
    barn: Barn  = Barn(
        aktørId = "456"
    ),
    saksnummer: Saksnummer = nesteSaksnummer(),
    behandlingUUID: BehandlingUUID = nesteBehandlingId()
): Uttaksgrunnlag {
    return Uttaksgrunnlag(
        søker = søker,
        barn = barn,
        saksnummer = saksnummer,
        behandlingUUID = behandlingUUID,
        søknadsperioder = listOf(søknadsperiode),
        arbeid = arbeid,
        pleiebehov = pleiebehov,
        tilsynsperioder = tilsynsperioder
    )
}


internal fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
internal fun nesteBehandlingId(): BehandlingUUID = UUID.randomUUID().toString()