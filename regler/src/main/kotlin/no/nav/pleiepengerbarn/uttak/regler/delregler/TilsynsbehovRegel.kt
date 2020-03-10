package no.nav.pleiepengerbarn.uttak.regler.delregler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttÅrsaker
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.domene.Årsaksbygger
import no.nav.pleiepengerbarn.uttak.regler.kontrakter_ext.overlappendePeriode
import no.nav.pleiepengerbarn.uttak.regler.lovverk.Lovhenvisninger.BorteFraArbeidet

internal class TilsynsbehovRegel : PeriodeRegel {
    override fun kjør(periode: LukketPeriode, grunnlag: RegelGrunnlag): Utfall {
        val årsaksbygger = Årsaksbygger()
        grunnlag.tilsynsbehov.entries.map { it.key }.overlappendePeriode(periode)?.apply {
            return TilBeregningAvGrad(
                    hjemler = setOf() // TODO trengs noe her?
            )
        }
        return Avslått(årsaker = årsaksbygger.hjemmel(AvslåttÅrsaker.UtenomTilsynsbehov, BorteFraArbeidet.anvend(
                "Fastsatt at barnet ikke har behov for tilsyn i perioden."
        )).byggAvslåttÅrsaker())
    }
}