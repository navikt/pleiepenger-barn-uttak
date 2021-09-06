package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

internal class KontraktValidatorTest {

    companion object {
        val helePerioden = LukketPeriode("2021-01-01/2021-01-31")
        val fullDag: Duration = Duration.ofHours(7).plusMinutes(30)
    }

    @Test
    internal fun `Normalt grunnlag som ikke skal gi valideringsfeil`() {
        val grunnlag = grunnlag()

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).isEmpty()
    }


    @Test
    internal fun `Samme saksnummer for andre søkere`() {
        val grunnlag = grunnlag()
            .copy(andrePartersSaksnummer = listOf("987", "987"))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.ANDRE_PARTERS_SAKSNUMMER_DUPLIKAT)
    }

    @Test
    internal fun `Behandling UUID med ugyldig UUID`() {
        val grunnlag = grunnlag()
            .copy(behandlingUUID= "tullball")

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.BEHANDLING_UUID_FORMATFEIL)
    }

    @Test
    internal fun `To sett med arbeidsperioder på samme arbeidsgiver`() {
        val grunnlag = grunnlag()
            .copy(arbeid = listOf(
                Arbeid(arbeidsgiver1(), mapOf(
                    helePerioden to ArbeidsforholdPeriodeInfo(jobberNormalt = fullDag, jobberNå = Duration.ZERO)
                )),
                Arbeid(arbeidsgiver1(), mapOf(
                    helePerioden to ArbeidsforholdPeriodeInfo(jobberNormalt = fullDag, jobberNå = Duration.ZERO)
                ))
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.ARBEIDSFORHOLD_ER_IKKE_UNIKE)
    }

    @Test
    internal fun `Overlappende arbeidsperioder`() {
        val grunnlag = grunnlag()
            .copy(arbeid = listOf(
                Arbeid(arbeidsgiver1(), mapOf(
                    helePerioden to ArbeidsforholdPeriodeInfo(jobberNormalt = fullDag, jobberNå = Duration.ZERO),
                    helePerioden.plusDager(5) to ArbeidsforholdPeriodeInfo(jobberNormalt = fullDag, jobberNå = Duration.ZERO),
                ))
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_ARBEIDSPERIODER)
    }

    @Test
    internal fun `Overlappende inngangsvilkårperioder`() {
        val grunnlag = grunnlag()
            .copy(inngangsvilkår = mapOf(
                "FP_VK_2" to listOf(
                    Vilkårsperiode(periode = helePerioden, utfall = Utfall.OPPFYLT),
                    Vilkårsperiode(periode = helePerioden.plusDager(10), utfall = Utfall.OPPFYLT),
                )
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_INNGANGSVILKÅRPERIODER)
    }

    @Test
    internal fun `Overlappende ferieperioder`() {
        val grunnlag = grunnlag()
            .copy(lovbestemtFerie = listOf(
                helePerioden,
                helePerioden.plusDager(12)
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_FERIEPERIODER)
    }

    @Test
    internal fun `Overlappende pleiebehovperioder`() {
        val grunnlag = grunnlag()
            .copy(pleiebehov = mapOf(
                helePerioden to Pleiebehov.PROSENT_100,
                helePerioden.plusDager(15) to Pleiebehov.PROSENT_200
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_PLEIEBEHOVPERIODER)
    }

    @Test
    internal fun `Overlappende tilsynsperioder`() {
        val grunnlag = grunnlag()
            .copy(tilsynsperioder = mapOf(
                helePerioden to Duration.ZERO,
                helePerioden.plusDager(16) to Duration.ZERO
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_TILSYNSPERIODER)
    }

    @Test
    internal fun `Overlappende søkt uttak`() {
        val grunnlag = grunnlag()
            .copy(søktUttak = listOf(
                SøktUttak(periode = helePerioden),
                SøktUttak(periode = helePerioden.plusDager(20))
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_SØKT_UTTAK)
    }

    @Test
    internal fun `Overlapp beredskapsperioder`() {
        val grunnlag = grunnlag()
            .copy(beredskapsperioder = mapOf(
                helePerioden to Utfall.OPPFYLT,
                helePerioden.plusDager(2) to Utfall.OPPFYLT
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_BEREDSKAPSPERIODER)

    }

    @Test
    internal fun `Overlapp nattevåksperioder`() {
        val grunnlag = grunnlag()
            .copy(nattevåksperioder = mapOf(
                helePerioden to Utfall.OPPFYLT,
                helePerioden.plusDager(3) to Utfall.OPPFYLT
            ))

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_NATTEVÅKSPERIODER)

    }

    @Test
    internal fun `Grunnlag med flere valideringsfeil`() {
        val grunnlag = grunnlag().copy(
            andrePartersSaksnummer = listOf("987", "987"),
            behandlingUUID= "tullball"
        )

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(2)
        assertThat(valideringsfeil).contains(Valideringsfeil.ANDRE_PARTERS_SAKSNUMMER_DUPLIKAT)
        assertThat(valideringsfeil).contains(Valideringsfeil.BEHANDLING_UUID_FORMATFEIL)
    }

    @Test
    internal fun `Grunnlag med overlappende trukket uttaksperioder`() {
        val grunnlag = grunnlag().copy(
            trukketUttak = listOf(LukketPeriode("2021-02-01/2021-02-10"), LukketPeriode("2021-02-05/2021-02-15"))
        )

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_TRUKKET_UTTAKSPERIODER)
    }

    @Test
    internal fun `Grunnlag med overlapp mellom søkt og trukket uttaksperioder`() {
        val grunnlag = grunnlag().copy(
            trukketUttak = listOf(LukketPeriode("2021-01-05/2021-02-15"))
        )

        val valideringsfeil = grunnlag.sjekk()

        assertThat(valideringsfeil).hasSize(1)
        assertThat(valideringsfeil).contains(Valideringsfeil.OVERLAPP_MELLOM_SØKT_UTTAK_OG_TRUKKET_UTTAK)
    }

    private fun grunnlag(): Uttaksgrunnlag {
        return Uttaksgrunnlag(
            barn = Barn(aktørId = "123"),
            søker = Søker(aktørId = "456"),
            saksnummer = "123456",
            behandlingUUID = UUID.randomUUID().toString(),
            andrePartersSaksnummer = listOf(),
            søktUttak = listOf(
                SøktUttak(helePerioden)
            ),
            arbeid = listOf(
                Arbeid(arbeidsgiver1(), mapOf(
                    helePerioden to ArbeidsforholdPeriodeInfo(jobberNormalt = fullDag, jobberNå = Duration.ZERO)
                ))
            ),
            pleiebehov = mapOf(helePerioden to Pleiebehov.PROSENT_100),

            lovbestemtFerie = listOf(),
            inngangsvilkår = mapOf(
                "FP_VK_2" to listOf(Vilkårsperiode(periode = helePerioden, utfall = Utfall.OPPFYLT)),
                "FP_VK_3" to listOf(Vilkårsperiode(periode = helePerioden, utfall = Utfall.OPPFYLT))
            ),
            tilsynsperioder = mapOf()
        )
    }

    private fun arbeidsgiver1(): Arbeidsforhold {
        return Arbeidsforhold("AG", "123456789", null, null)
    }

}

private fun LukketPeriode.plusDager(dager: Long) = LukketPeriode(fom = this.fom.plusDays(dager), tom = this.tom.plusDays(dager))