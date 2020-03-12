package no.nav.pleiepengerbarn.uttak.regler.mapper

import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.ikkeMedlem
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom

object GrunnlagMapper {

    fun tilRegelGrunnlag(uttaksgrunnlag: Uttaksgrunnlag, andrePartersUttakplan:List<Uttaksplan>): RegelGrunnlag {

        val søknadsperioderSortert = uttaksgrunnlag.søknadsperioder.sortertPåFom()

        val unikeArbeidsforhold = uttaksgrunnlag.arbeid.map { it.arbeidsforhold }.toSet().size
        if (unikeArbeidsforhold != uttaksgrunnlag.arbeid.size) {
            throw IllegalStateException("Arbeidsforholdene i grunnlaget må være unike.")
        }

        return RegelGrunnlag(
                barn = uttaksgrunnlag.barn,
                søker = uttaksgrunnlag.søker,
                tilsynsbehov = uttaksgrunnlag.tilsynsbehov.sortertPåFom(),
                søknadsperioder = søknadsperioderSortert,
                arbeid = uttaksgrunnlag.arbeid,
                tilsynsperioder = uttaksgrunnlag.tilsynsperioder,
                ferier = uttaksgrunnlag.lovbestemtFerie.sortertPåFom(),
                andrePartersUttaksplan = andrePartersUttakplan.sorterteUttaksplaner(),
                ikkeMedlem = uttaksgrunnlag.medlemskap.ikkeMedlem(søknadsperioderSortert)
        )
    }

    private fun List<Uttaksplan>.sorterteUttaksplaner() = map { it.copy(
            perioder = it.perioder.sortertPåFom()
    )}
}