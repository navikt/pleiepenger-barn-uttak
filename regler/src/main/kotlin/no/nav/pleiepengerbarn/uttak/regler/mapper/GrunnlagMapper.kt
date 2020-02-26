package no.nav.pleiepengerbarn.uttak.regler.mapper

import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.ikkeMedlem
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.sortertPåFom

object GrunnlagMapper {

    fun tilRegelGrunnlag(uttaksgrunnlag: Uttaksgrunnlag, andrePartersUttakplan:List<Uttaksplan>): RegelGrunnlag {

        val søknadsperioderSortert = uttaksgrunnlag.søknadsperioder.sortertPåFom()
        return RegelGrunnlag(
                tilsynsbehov = uttaksgrunnlag.tilsynsbehov.perioder.sortertPåFom(),
                søknadsperioder = søknadsperioderSortert,
                arbeidsforhold = uttaksgrunnlag.arbeidsforhold.sorterteArbeidsforhold(),
                //TODO tilsynsperioder = uttaksgrunnlag.tilsyn.perioder.sortertPåFom(),
                tilsynsperioder = mapOf(),
                ferier = uttaksgrunnlag.lovbestemtFerie.sortertPåFom(),
                andrePartersUttaksplan = andrePartersUttakplan.sorterteUttaksplaner(),
                ikkeMedlem = uttaksgrunnlag.medlemskap.perioder.ikkeMedlem(søknadsperioderSortert)
        )
    }

    private fun List<Arbeidsforhold>.sorterteArbeidsforhold() = map { it.copy(
            perioder = it.perioder.sortertPåFom()
    )}

    private fun List<Uttaksplan>.sorterteUttaksplaner() = map { it.copy(
            perioder = it.perioder.sortertPåFom()
    )}
}