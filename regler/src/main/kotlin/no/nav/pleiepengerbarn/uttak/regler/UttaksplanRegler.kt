package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.KnekkpunktType
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.OverseEtablertTilsynÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader
import no.nav.pleiepengerbarn.uttak.kontrakter.Utenlandsopphold
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak
import no.nav.pleiepengerbarn.uttak.regler.delregler.BarnsDødPeriodeRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.FerieRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.IkkeOppfylt
import no.nav.pleiepengerbarn.uttak.regler.delregler.InngangsvilkårIkkeOppfyltRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.MaxAntallDagerRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.SøkersDødRegel
import no.nav.pleiepengerbarn.uttak.regler.delregler.TilBeregningAvGrad
import no.nav.pleiepengerbarn.uttak.regler.delregler.UtenlandsoppholdRegel
import no.nav.pleiepengerbarn.uttak.regler.domene.GraderBeregnet
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.annenPart
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlapperDelvis
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

internal object UttaksplanRegler {

    private val PeriodeRegler = linkedSetOf(
        FerieRegel(),
        SøkersDødRegel(),
        BarnsDødPeriodeRegel()
    )

    private val UttaksplanRegler = linkedSetOf(
        InngangsvilkårIkkeOppfyltRegel(),
        UtenlandsoppholdRegel(),
        MaxAntallDagerRegel()
    )

    internal fun fastsettUttaksplan(
        grunnlag: RegelGrunnlag,
        knektePerioder: Map<SøktUttak, Set<KnekkpunktType>>
    ): Uttaksplan {
        // Fastsett periode
        val perioder = mutableMapOf<LukketPeriode, UttaksperiodeInfo>()
        knektePerioder.forEach { (søktUttaksperiode, knekkpunktTyper) ->
            val ikkeOppfyltÅrsaker = fastsettPeriodeRegler(søktUttaksperiode.periode, grunnlag)
            fastsettGrader(perioder, søktUttaksperiode.periode, grunnlag, knekkpunktTyper, ikkeOppfyltÅrsaker)
        }
        //Fastsett uttaksplan
        return fastsettUttaksplanRegler(perioder, grunnlag)
    }

    private fun fastsettPeriodeRegler(søktUttaksperiode: LukketPeriode, grunnlag: RegelGrunnlag): Set<Årsak> {
        val årsaker = mutableSetOf<Årsak>()
        var overstyrtÅrsak: Årsak? = null
        PeriodeRegler.forEach { regel ->
            val utfall = regel.kjør(periode = søktUttaksperiode, grunnlag = grunnlag)
            if (utfall is IkkeOppfylt) {
                årsaker.addAll(utfall.årsaker)
            } else if (utfall is TilBeregningAvGrad) {
                if (utfall.overstyrtÅrsak != null) {
                    overstyrtÅrsak = utfall.overstyrtÅrsak
                }
            }
        }
        if (overstyrtÅrsak != null) {
            return setOf(overstyrtÅrsak!!)
        }
        return årsaker
    }

    private fun fastsettGrader(
        perioder: MutableMap<LukketPeriode, UttaksperiodeInfo>,
        søktUttaksperiode: LukketPeriode,
        grunnlag: RegelGrunnlag,
        knekkpunktTyper: Set<KnekkpunktType>,
        årsaker: Set<Årsak>
    ) {
        val grader = finnGrader(søktUttaksperiode, grunnlag)
        val nattevåk = grunnlag.finnNattevåk(søktUttaksperiode)
        val beredskap = grunnlag.finnBeredskap(søktUttaksperiode)
        val ikkeOppfyltÅrsaker = årsaker.filter { !it.oppfylt }.toSet()
        var søktPeriodeOverlapperMedUtenlandsperiode = false
        var utenlandsopphold: Map.Entry<LukketPeriode, UtenlandsoppholdInfo>? = null
        val landkode: String?
        val utenlandsoppholdÅrsak: UtenlandsoppholdÅrsak
        for (utenlandsoppholdElement in grunnlag.utenlandsoppholdperioder) {
            if (utenlandsoppholdElement.key.overlapperDelvis(søktUttaksperiode)) {
                søktPeriodeOverlapperMedUtenlandsperiode = true
                utenlandsopphold = utenlandsoppholdElement
                break
            }
        }
        if (søktPeriodeOverlapperMedUtenlandsperiode) {
            landkode = utenlandsopphold?.value?.landkode
            utenlandsoppholdÅrsak = utenlandsopphold?.value?.utenlandsoppholdÅrsak ?: UtenlandsoppholdÅrsak.INGEN
        } else {
            landkode = grunnlag.utenlandsoppholdperioder[søktUttaksperiode]?.landkode
            utenlandsoppholdÅrsak = grunnlag.utenlandsoppholdperioder[søktUttaksperiode]?.utenlandsoppholdÅrsak
                ?: UtenlandsoppholdÅrsak.INGEN
        }
        if (ikkeOppfyltÅrsaker.isNotEmpty()) {
            perioder[søktUttaksperiode] = UttaksperiodeInfo.ikkeOppfylt(
                utbetalingsgrader = grader.tilUtbetalingsgrader(false),
                søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                oppgittTilsyn = grader.oppgittTilsyn,
                årsaker = ikkeOppfyltÅrsaker,
                pleiebehov = grader.pleiebehov.prosent,
                graderingMotTilsyn = grader.graderingMotTilsyn,
                knekkpunktTyper = knekkpunktTyper,
                kildeBehandlingUUID = grunnlag.behandlingUUID.toString(),
                annenPart = grunnlag.annenPart(søktUttaksperiode),
                nattevåk = nattevåk,
                beredskap = beredskap,
                utenlandsopphold = Utenlandsopphold(landkode, utenlandsoppholdÅrsak)
            )
        } else {
            if (grader.årsak.oppfylt) {
                val årsak = if (årsaker.size == 1) {
                    årsaker.first()
                } else {
                    grader.årsak
                }
                perioder[søktUttaksperiode] = UttaksperiodeInfo.oppfylt(
                    uttaksgrad = grader.uttaksgrad,
                    utbetalingsgrader = grader.tilUtbetalingsgrader(true),
                    søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                    oppgittTilsyn = grader.oppgittTilsyn,
                    årsak = årsak,
                    pleiebehov = grader.pleiebehov.prosent,
                    graderingMotTilsyn = grader.graderingMotTilsyn,
                    knekkpunktTyper = knekkpunktTyper,
                    kildeBehandlingUUID = grunnlag.behandlingUUID.toString(),
                    annenPart = grunnlag.annenPart(søktUttaksperiode),
                    nattevåk = nattevåk,
                    beredskap = beredskap,
                    utenlandsopphold = Utenlandsopphold(landkode, utenlandsoppholdÅrsak)
                )
            } else {
                perioder[søktUttaksperiode] = UttaksperiodeInfo.ikkeOppfylt(
                    utbetalingsgrader = grader.tilUtbetalingsgrader(false),
                    søkersTapteArbeidstid = grader.søkersTapteArbeidstid,
                    oppgittTilsyn = grader.oppgittTilsyn,
                    årsaker = setOf(grader.årsak),
                    pleiebehov = grader.pleiebehov.prosent,
                    graderingMotTilsyn = grader.graderingMotTilsyn,
                    knekkpunktTyper = knekkpunktTyper,
                    kildeBehandlingUUID = grunnlag.behandlingUUID.toString(),
                    annenPart = grunnlag.annenPart(søktUttaksperiode),
                    nattevåk = nattevåk,
                    beredskap = beredskap,
                    utenlandsopphold = Utenlandsopphold(landkode, utenlandsoppholdÅrsak)
                )
            }
        }
    }

    private fun fastsettUttaksplanRegler(
        perioder: Map<LukketPeriode, UttaksperiodeInfo>,
        grunnlag: RegelGrunnlag
    ): Uttaksplan {
        var uttaksplan = Uttaksplan(perioder, grunnlag.trukketUttak, null, grunnlag.commitId)
        UttaksplanRegler.forEach { uttaksplanRegler ->
            uttaksplan = uttaksplanRegler.kjør(
                uttaksplan = uttaksplan,
                grunnlag = grunnlag
            )
        }
        return uttaksplan
    }


    private fun GraderBeregnet.tilUtbetalingsgrader(oppfylt: Boolean): List<Utbetalingsgrader> {
        return this.utbetalingsgrader.map {
            val utbetalingsgrad = if (oppfylt) it.value.utbetalingsgrad else BigDecimal.ZERO
            Utbetalingsgrader(
                arbeidsforhold = it.key,
                utbetalingsgrad = utbetalingsgrad,
                normalArbeidstid = it.value.normalArbeidstid,
                faktiskArbeidstid = it.value.faktiskArbeidstid,
                tilkommet = it.value.tilkommet
            )
        }
    }

    private fun finnGrader(periode: LukketPeriode, grunnlag: RegelGrunnlag): GraderBeregnet {
        val pleiebehov = grunnlag.finnPleiebehov(periode)
        val etablertTilsyn = grunnlag.finnEtablertTilsyn(periode)
        val oppgittTilsyn = grunnlag.finnOppgittTilsyn(periode)
        val (andreSøkeresTilsynReberegnet, andrePartersTilsyn) = grunnlag.finnAndreSøkeresTilsyn(periode)
        val arbeidPerArbeidsforhold = grunnlag.finnArbeidPerArbeidsforhold(periode)
        val overseEtablertTilsynÅrsak = grunnlag.avklarOverseEtablertTilsynÅrsak(periode, etablertTilsyn)

        return BeregnGrader.beregn(
            pleiebehov = pleiebehov,
            etablertTilsyn = etablertTilsyn,
            oppgittTilsyn = oppgittTilsyn,
            andreSøkeresTilsyn = andrePartersTilsyn,
            andreSøkeresTilsynReberegnet = andreSøkeresTilsynReberegnet,
            arbeid = arbeidPerArbeidsforhold,
            overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak,
            ytelseType = grunnlag.ytelseType
        )
    }

    private fun RegelGrunnlag.avklarOverseEtablertTilsynÅrsak(
        periode: LukketPeriode,
        etablertTilsyn: Duration
    ): OverseEtablertTilsynÅrsak? {
        val etablertTilsynsprosent = BigDecimal(etablertTilsyn.toMillis()).setScale(2, RoundingMode.HALF_UP)
                .divide(BigDecimal(FULL_DAG.toMillis()), 2, RoundingMode.HALF_UP) * HUNDRE_PROSENT
        if (etablertTilsynsprosent > Prosent.ZERO && etablertTilsynsprosent < TI_PROSENT) {
            return OverseEtablertTilsynÅrsak.FOR_LAVT
        }
        val nattevåk = this.finnNattevåk(periode)
        val beredskap = this.finnBeredskap(periode)
        return finnOverseEtablertTilsynÅrsak(nattevåk, beredskap)
    }
}
