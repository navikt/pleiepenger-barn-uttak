package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttÅrsaker
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.domene.Årsaksbygger
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.TapAvInntekt

internal class FerieRegel : PeriodeRegel {

    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        grunnlag.lovbestemtFerie.overlappendePeriode(periode)?.apply {
            return Avslått(årsaker = Årsaksbygger().hjemmel(AvslåttÅrsaker.LOVBESTEMT_FERIE, TapAvInntekt.anvend(
                    "Fastsatt at det ikke er noe inntektstap ved avvkling av lovbestemt ferie."
            )).byggAvslåttÅrsaker())

            // Ferie: Legge til ref. til 9-15 jfr. 8-17 andre lett
        }
        return TilBeregningAvGrad(
                hjemler = setOf() // TOD: Legge til noe her..?
        )
    }
}