package no.nav.pleiepengerbarn.uttak.testklient

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.Arbeidstype
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal val FULL_DAG = Duration.ofHours(7).plusMinutes(30)
internal val INGENTING = Duration.ZERO

internal val HELE_2020 = LukketPeriode("2020-01-01/2020-12-31")

internal val ARBEIDSFORHOLD1 = Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789")
internal val IKKE_YRKESAKTIV = Arbeidsforhold(type = "IKKE_YRKESAKTIV", organisasjonsnummer = "987654321")
internal val KUN_YTELSE = Arbeidsforhold(type = Arbeidstype.KUN_YTELSE.kode)
internal val ARBEIDSFORHOLD2 = Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID().toString())
internal val ARBEIDSFORHOLD3 = Arbeidsforhold(type = "AT", organisasjonsnummer = "123456789", arbeidsforholdId = UUID.randomUUID().toString())
internal val SELVSTENDIG1 = Arbeidsforhold(type = "SN",organisasjonsnummer = "121212121")
internal val FRILANS1 = Arbeidsforhold(type = "FL")
internal val ARBEIDSFORHOLD4 = Arbeidsforhold(type="AT", organisasjonsnummer = "987654321")

internal val mapper = ObjectMapper().registerModule(JavaTimeModule())
    .setVisibility(com.fasterxml.jackson.annotation .PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
    .setVisibility(com.fasterxml.jackson.annotation .PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
    .setVisibility(com.fasterxml.jackson.annotation .PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setVisibility(com.fasterxml.jackson.annotation .PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)

internal fun lagGrunnlag(saksnummer: Saksnummer = nesteSaksnummer(), periode: String, ytelseType: YtelseType = YtelseType.PSB): Uttaksgrunnlag {
    val søknadsperiode = LukketPeriode(periode)
    return lagGrunnlag(
        ytelseType = ytelseType,
        saksnummer = saksnummer,
        søknadsperiode = søknadsperiode,
        arbeid = listOf(
            Arbeid(ARBEIDSFORHOLD1, mapOf(søknadsperiode to ArbeidsforholdPeriodeInfo(jobberNormalt = FULL_DAG, jobberNå = INGENTING)))
        ),
        pleiebehov = mapOf(søknadsperiode to Pleiebehov.PROSENT_100),
    )
}

internal fun lagGrunnlag(
    ytelseType: YtelseType = YtelseType.PSB,
    søknadsperiode: LukketPeriode,
    arbeid: List<Arbeid>,
    pleiebehov: Map<LukketPeriode, Pleiebehov>,
    tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    søker: Søker = Søker(
        aktørId = "123"
    ),
    barn: Barn  = Barn(
        aktørId = "456"
    ),
    nattevåk: Map<LukketPeriode, Utfall> = mapOf(),
    bereskap: Map<LukketPeriode, Utfall> = mapOf(),
    saksnummer: Saksnummer = nesteSaksnummer(),
    behandlingUUID: BehandlingUUID = nesteBehandlingId(),
    nyeReglerUtbetalingsgrad: LocalDate? = null
): Uttaksgrunnlag {
    return Uttaksgrunnlag(
        ytelseType = ytelseType,
        søker = søker,
        barn = barn,
        saksnummer = saksnummer,
        behandlingUUID = behandlingUUID,
        søktUttak = listOf(SøktUttak(søknadsperiode)),
        arbeid = arbeid,
        pleiebehov = pleiebehov,
        tilsynsperioder = tilsynsperioder,
        nattevåksperioder = nattevåk,
        beredskapsperioder = bereskap,
        nyeReglerUtbetalingsgrad = nyeReglerUtbetalingsgrad
    )
}

internal fun lagGrunnlagFraJson(
    json: String
): Uttaksgrunnlag {
    return mapper.readerFor(Uttaksgrunnlag::class.java).readValue(json)
}

internal fun lagUttaksplanFraJson(
    json: String
): Uttaksplan {
    return mapper.readerFor(Uttaksplan::class.java).readValue(json)
}

internal fun lagGrunnlag(
    ytelseType: YtelseType = YtelseType.PSB,
    arbeid: List<Arbeid>,
    pleiebehov: Map<LukketPeriode, Pleiebehov>,
    tilsynsperioder: Map<LukketPeriode, Duration> = mapOf(),
    søker: Søker = Søker(
        aktørId = "123"
    ),
    barn: Barn = Barn(
        aktørId = "456"
    ),
    nattevåk: Map<LukketPeriode, Utfall> = mapOf(),
    bereskap: Map<LukketPeriode, Utfall> = mapOf(),
    saksnummer: Saksnummer = nesteSaksnummer(),
    behandlingUUID: BehandlingUUID = nesteBehandlingId(),
    søktUttak: List<SøktUttak>
): Uttaksgrunnlag {
    return Uttaksgrunnlag(
        ytelseType = ytelseType,
        søker = søker,
        barn = barn,
        saksnummer = saksnummer,
        behandlingUUID = behandlingUUID,
        søktUttak = søktUttak,
        arbeid = arbeid,
        pleiebehov = pleiebehov,
        tilsynsperioder = tilsynsperioder,
        nattevåksperioder = nattevåk,
        beredskapsperioder = bereskap
    )
}


internal fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
internal fun nesteBehandlingId(): BehandlingUUID = UUID.randomUUID().toString()
