package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkIkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.UttaksperiodeAsserts.sjekkOppfylt
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

internal class ForeldrepengerRegelTest {

    private companion object {
        private val INGENTING = Duration.ZERO
        private val arbeidsforhold1 = UUID.randomUUID().toString()
        private const val aktørIdSøker = "123"
        private const val aktørIdBarn = "456"
    }

    @Test
    fun `Hele søknadsperioden overlapper med foreldrepengeperiode - skal gi avslag på hele perioden`() {
        val periode = LukketPeriode("2020-01-06/2020-01-10")

        val grunnlag = RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            behandlingUUID = nesteBehandlingId(),
            søker = Søker(aktørId = aktørIdSøker),
            barn = Barn(aktørId = aktørIdBarn),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100),
            søktUttak = listOf(SøktUttak(periode)),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(periode to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            foreldrepengeperioder = listOf(periode)
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkIkkeOppfylt(uttaksplan, periode, setOf(Årsak.INGEN_TAPT_INNTEKT_PGA_FP))
    }

    @Test
    fun `Søknadsperiode overlapper delvis med foreldrepengeperiode - skal gi avslag kun på overlappende del`() {
        val helePerioden = LukketPeriode("2020-01-06/2020-01-17")
        val foreldrepengePeriode = LukketPeriode("2020-01-13/2020-01-31")

        val grunnlag = RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            behandlingUUID = nesteBehandlingId(),
            søker = Søker(aktørId = aktørIdSøker),
            barn = Barn(aktørId = aktørIdBarn),
            pleiebehov = mapOf(helePerioden to Pleiebehov.PROSENT_100),
            søktUttak = listOf(SøktUttak(helePerioden)),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            foreldrepengeperioder = listOf(foreldrepengePeriode)
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(2)
        sjekkOppfylt(
            uttaksplan,
            LukketPeriode("2020-01-06/2020-01-10"),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING
        )
        sjekkIkkeOppfylt(
            uttaksplan,
            LukketPeriode("2020-01-13/2020-01-17"),
            setOf(Årsak.INGEN_TAPT_INNTEKT_PGA_FP)
        )
    }

    @Test
    fun `Ingen foreldrepengeperioder - skal gi innvilgelse`() {
        val periode = LukketPeriode("2020-01-06/2020-01-10")

        val grunnlag = RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            behandlingUUID = nesteBehandlingId(),
            søker = Søker(aktørId = aktørIdSøker),
            barn = Barn(aktørId = aktørIdBarn),
            pleiebehov = mapOf(periode to Pleiebehov.PROSENT_100),
            søktUttak = listOf(SøktUttak(periode)),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(periode to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            foreldrepengeperioder = listOf()
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(1)
        sjekkOppfylt(uttaksplan, periode, HUNDRE_PROSENT, mapOf(arbeidsforhold1 to HUNDRE_PROSENT), Årsak.FULL_DEKNING)
    }

    @Test
    fun `Foreldrepengeperiode overlapper med en del av søknadsperioden, resten er ikke berørt`() {
        val helePerioden = LukketPeriode("2020-01-06/2020-01-24")
        val foreldrepengePeriode = LukketPeriode("2020-01-13/2020-01-17")

        val grunnlag = RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            behandlingUUID = nesteBehandlingId(),
            søker = Søker(aktørId = aktørIdSøker),
            barn = Barn(aktørId = aktørIdBarn),
            pleiebehov = mapOf(helePerioden to Pleiebehov.PROSENT_100),
            søktUttak = listOf(SøktUttak(helePerioden)),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            foreldrepengeperioder = listOf(foreldrepengePeriode)
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(3)
        sjekkOppfylt(
            uttaksplan,
            listOf(
                LukketPeriode("2020-01-06/2020-01-10"),
                LukketPeriode("2020-01-20/2020-01-24")
            ),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING
        )
        sjekkIkkeOppfylt(
            uttaksplan,
            LukketPeriode("2020-01-13/2020-01-17"),
            setOf(Årsak.INGEN_TAPT_INNTEKT_PGA_FP)
        )
    }

    @Test
    fun `Søknadsperiode overlapper med foreldrepengeperiode og ferie - skal gi avslag med begge årsaker`() {
        val helePerioden = LukketPeriode("2020-01-06/2020-01-24")
        val foreldrepengePeriode = LukketPeriode("2020-01-13/2020-01-17")
        val feriePeriode = LukketPeriode("2020-01-20/2020-01-24")

        val grunnlag = RegelGrunnlag(
            saksnummer = nesteSaksnummer(),
            behandlingUUID = nesteBehandlingId(),
            søker = Søker(aktørId = aktørIdSøker),
            barn = Barn(aktørId = aktørIdBarn),
            pleiebehov = mapOf(helePerioden to Pleiebehov.PROSENT_100),
            søktUttak = listOf(SøktUttak(helePerioden)),
            arbeid = mapOf(
                arbeidsforhold1 to mapOf(helePerioden to ArbeidsforholdPeriodeInfo(FULL_DAG, INGENTING))
            ).somArbeid(),
            lovbestemtFerie = listOf(feriePeriode),
            foreldrepengeperioder = listOf(foreldrepengePeriode)
        )

        val uttaksplan = UttakTjeneste.uttaksplan(grunnlag)

        assertThat(uttaksplan.perioder).hasSize(3)
        sjekkOppfylt(
            uttaksplan,
            LukketPeriode("2020-01-06/2020-01-10"),
            HUNDRE_PROSENT,
            mapOf(arbeidsforhold1 to HUNDRE_PROSENT),
            Årsak.FULL_DEKNING
        )
        sjekkIkkeOppfylt(
            uttaksplan,
            LukketPeriode("2020-01-13/2020-01-17"),
            setOf(Årsak.INGEN_TAPT_INNTEKT_PGA_FP)
        )
        sjekkIkkeOppfylt(
            uttaksplan,
            LukketPeriode("2020-01-20/2020-01-24"),
            setOf(Årsak.LOVBESTEMT_FERIE)
        )
    }
}

private fun nesteSaksnummer(): Saksnummer = UUID.randomUUID().toString().takeLast(19)
private fun nesteBehandlingId() = UUID.randomUUID()

