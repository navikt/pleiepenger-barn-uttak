package no.nav.pleiepengerbarn.uttak.regler.mapper

import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom
import java.util.*

object GrunnlagMapper {

    fun tilRegelGrunnlag(
        uttaksgrunnlag: Uttaksgrunnlag,
        andrePartersUttakplanPerBehandling: Map<UUID, Uttaksplan>,
        forrigeUttaksplan: Uttaksplan?,
        commitId: String = ""
    ): RegelGrunnlag {

        val søknadsperioderSortert = uttaksgrunnlag.søktUttak.sortertPåFom()

        val unikeArbeidsforhold = uttaksgrunnlag.arbeid.map { it.arbeidsforhold }.toSet().size
        if (unikeArbeidsforhold != uttaksgrunnlag.arbeid.size) {
            throw IllegalStateException("Arbeidsforholdene i grunnlaget må være unike.")
        }

        val kravprioritetForBehandlinger = mutableMapOf<LukketPeriode, List<UUID>>()
        uttaksgrunnlag.kravprioritetForBehandlinger.forEach { (periode, kravprio) ->
            kravprioritetForBehandlinger[periode] = kravprio.map { UUID.fromString(it) }
        }
        val sisteVedtatteUttaksplanForBehandling = mutableMapOf<UUID, UUID>()
        uttaksgrunnlag.sisteVedtatteUttaksplanForBehandling.filterValues { it != null }.forEach { (behandling, originalBehandling) ->
            sisteVedtatteUttaksplanForBehandling[UUID.fromString(behandling)] = UUID.fromString(originalBehandling)
        }

        return RegelGrunnlag(
                ytelseType = uttaksgrunnlag.ytelseType,
                saksnummer = uttaksgrunnlag.saksnummer,
                behandlingUUID = UUID.fromString(uttaksgrunnlag.behandlingUUID),
                barn = uttaksgrunnlag.barn,
                søker = uttaksgrunnlag.søker,
                pleiebehov = uttaksgrunnlag.pleiebehov.sortertPåFom(),
                søktUttak = søknadsperioderSortert,
                trukketUttak = uttaksgrunnlag.trukketUttak,
                arbeid = uttaksgrunnlag.arbeid,
                tilsynsperioder = uttaksgrunnlag.tilsynsperioder,
                lovbestemtFerie = uttaksgrunnlag.lovbestemtFerie.sortedBy { it.fom },
                inngangsvilkår = uttaksgrunnlag.inngangsvilkår,
                andrePartersUttaksplanPerBehandling = andrePartersUttakplanPerBehandling,
                sisteVedtatteUttaksplanForBehandling = sisteVedtatteUttaksplanForBehandling,
                forrigeUttaksplan = forrigeUttaksplan,
                beredskapsperioder = uttaksgrunnlag.beredskapsperioder,
                nattevåksperioder = uttaksgrunnlag.nattevåksperioder,
                kravprioritetForBehandlinger = kravprioritetForBehandlinger,
                utenlandsoppholdperioder = uttaksgrunnlag.utenlandsoppholdperioder,
                commitId = commitId
        )
    }

}
