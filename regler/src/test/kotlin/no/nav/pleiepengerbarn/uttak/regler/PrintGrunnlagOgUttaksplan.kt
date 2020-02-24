package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.tidslinje.Tidslinje
import no.nav.pleiepengerbarn.uttak.regler.tidslinje.TidslinjeAsciiArt

class PrintGrunnlagOgUttaksplan(private val grunnlag: RegelGrunnlag, private val uttaksplan: Uttaksplan) {

    fun print() {
        val tidslinjer = LinkedHashSet<Tidslinje>()

        tidslinjer.add(tilsynsbehovTidslinje())
        tidslinjer.add(ikkeMedlem())
        tidslinjer.addAll(andrePartesUttak())
        tidslinjer.add(søknadsperioder())
        tidslinjer.add(tilsynTidsplinje())
        tidslinjer.add(ferieperioder())
        tidslinjer.addAll(arbeidsperioder())
        tidslinjer.add(uttaksplan())

        TidslinjeAsciiArt.printTidslinje(tidslinjer)
    }

    private fun ikkeMedlem(): Tidslinje {
        val ikkeMedlemPerioder = mutableMapOf<LukketPeriode, Prosent?>()
        grunnlag.ikkeMedlem.forEach { ikkeMedlemPerioder[it] = null }
        return Tidslinje("Ikke Medlem", ikkeMedlemPerioder.toMap())

    }

    private fun tilsynsbehovTidslinje():Tidslinje {
        val tilsynbehovPerioder = mutableMapOf<LukketPeriode, Prosent>()
        grunnlag.tilsynsbehov.forEach {tilsynsbehov ->
            val grad = if (tilsynsbehov.tilsynsbehovStørrelse == TilsynsbehovStørrelse.PROSENT_200) {
                Prosent(200)
            } else {
                Prosent(100)
            }
            tilsynbehovPerioder[tilsynsbehov.periode] = grad
        }
        return Tidslinje("Tilsynsbehov", tilsynbehovPerioder)
    }

    private fun andrePartesUttak():List<Tidslinje> {
        val tidslinjer = mutableListOf<Tidslinje>()
        grunnlag.andrePartersUttaksplan.forEach {uttaksplan ->
            val uttaksperioder = mutableMapOf<LukketPeriode, Prosent>()
            uttaksplan.perioder.forEach {periode -> uttaksperioder[periode.periode] = periode.uttaksperiodeResultat.grad }
            tidslinjer.add(Tidslinje("Annen part", uttaksperioder))
        }
        return tidslinjer
    }

    private fun søknadsperioder():Tidslinje {
        val søknadsperioder = mutableMapOf<LukketPeriode, Prosent>()
        grunnlag.søknadsperioder.forEach { søktPeriode ->  søknadsperioder[søktPeriode] = Prosent(100) }
        return Tidslinje("Søknadsperioder", søknadsperioder)
    }

    private fun tilsynTidsplinje():Tidslinje {
        val tilsynPerioder = mutableMapOf<LukketPeriode, Prosent>()
        grunnlag.tilsynsperioder.forEach { tilsyn ->  tilsynPerioder[tilsyn.periode] = tilsyn.grad }
        return Tidslinje("Tilsyn", tilsynPerioder)
    }

    private fun ferieperioder():Tidslinje {
        val ferier = mutableMapOf<LukketPeriode, Prosent>()
        grunnlag.ferier.forEach { ferie ->  ferier[ferie] = Prosent(100) }
        return Tidslinje("Ferier", ferier)
    }

    private fun arbeidsperioder():List<Tidslinje> {
        val tidslinjer = mutableListOf<Tidslinje>()
        grunnlag.arbeidsforhold.forEach { (_, arbeidList) ->

            val arbeidsperioder = mutableMapOf<LukketPeriode, Prosent>()
            arbeidList.forEach { arbeidsperiode ->
                arbeidsperioder[arbeidsperiode.periode] = arbeidsperiode.arbeidsprosent
            }
            tidslinjer.add(Tidslinje("Arbeid", arbeidsperioder))
        }
        return tidslinjer
    }

    private fun uttaksplan():Tidslinje {
        val uttaksperioder = mutableMapOf<LukketPeriode, Prosent>()
        uttaksplan.perioder.forEach {periode ->  uttaksperioder[periode.periode] = periode.uttaksperiodeResultat.grad}
        return Tidslinje("Uttaksplan", uttaksperioder)
    }

}