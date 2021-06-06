package no.nav.pleiepengerbarn.uttak.regler.mapper

import no.nav.pleiepengerbarn.uttak.kontrakter.Saksnummer
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom

object GrunnlagMapper {

    fun tilRegelGrunnlag(uttaksgrunnlag: Uttaksgrunnlag, andrePartersUttakplan:Map<Saksnummer, Uttaksplan>): RegelGrunnlag {

        val søknadsperioderSortert = uttaksgrunnlag.søktUttak.sortertPåFom()

        val unikeArbeidsforhold = uttaksgrunnlag.arbeid.map { it.arbeidsforhold }.toSet().size
        if (unikeArbeidsforhold != uttaksgrunnlag.arbeid.size) {
            throw IllegalStateException("Arbeidsforholdene i grunnlaget må være unike.")
        }

        return RegelGrunnlag(
                behandlingUUID = uttaksgrunnlag.behandlingUUID,
                barn = uttaksgrunnlag.barn,
                søker = uttaksgrunnlag.søker,
                pleiebehov = uttaksgrunnlag.pleiebehov.sortertPåFom(),
                søktUttak = søknadsperioderSortert,
                arbeid = uttaksgrunnlag.arbeid,
                tilsynsperioder = uttaksgrunnlag.tilsynsperioder,
                lovbestemtFerie = uttaksgrunnlag.lovbestemtFerie.sortedBy { it.fom },
                inngangsvilkår = uttaksgrunnlag.inngangsvilkår,
                andrePartersUttaksplan = andrePartersUttakplan,
                beredskapsperioder = uttaksgrunnlag.beredskapsperioder,
                nattevåksperioder = uttaksgrunnlag.nattevåksperioder,
                kravprioritet = uttaksgrunnlag.kravprioritet
        )
    }

}