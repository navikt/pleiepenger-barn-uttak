package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttÅrsaker
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.domene.Årsaksbygger
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.MedlemskapIFolketrygden

internal class MedlemskapRegel : PeriodeRegel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        val årsaksbygger = Årsaksbygger()

        grunnlag.ikkeMedlem.overlappendePeriode(periode)?.apply {
            return Avslått(årsaker = årsaksbygger.hjemmel(AvslåttÅrsaker.IkkeMedlemIFolketrygden, MedlemskapIFolketrygden.anvend(
                    "Fastsatt at personen ikke er medlem av folketrygden i perioden."
            )).byggAvslåttÅrsaker())
        }
        return TilBeregningAvGrad(
                hjemler = setOf(
                        MedlemskapIFolketrygden.anvend(
                                "Fastatt at personen er medlem av folketrygden i perioden."
                        )
                )
        )
    }
}