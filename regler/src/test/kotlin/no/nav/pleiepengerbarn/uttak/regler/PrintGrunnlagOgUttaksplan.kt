package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.*
import no.nav.pleiepengerbarn.uttak.regler.domene.RegelGrunnlag
import no.nav.pleiepengerbarn.uttak.regler.tidslinje.Tidslinje
import no.nav.pleiepengerbarn.uttak.regler.tidslinje.TidslinjeAsciiArt
import java.math.BigDecimal


internal fun UttakTjeneste.uttaksplanOgPrint(grunnlag: RegelGrunnlag) : Uttaksplan {
    val uttaksplan = uttaksplan(grunnlag)
    PrintGrunnlagOgUttaksplan(grunnlag, uttaksplan).print()
    return uttaksplan
}

internal fun Uttaksplan.print(grunnlag: RegelGrunnlag) {
    PrintGrunnlagOgUttaksplan(grunnlag, this).print()
}


private class PrintGrunnlagOgUttaksplan(
        private val grunnlag: RegelGrunnlag,
        private val uttaksplan: Uttaksplan) {

    internal fun print() {
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
        grunnlag.tilsynsbehov.forEach { (periode, tilsynsbehov) ->
            val grad = if (tilsynsbehov.prosent == TilsynsbehovStørrelse.PROSENT_200) {
                Prosent(200)
            } else {
                Prosent(100)
            }
            tilsynbehovPerioder[periode] = grad
        }
        return Tidslinje("Tilsynsbehov", tilsynbehovPerioder)
    }

    private fun andrePartesUttak():List<Tidslinje> {
        val tidslinjer = mutableListOf<Tidslinje>()
        grunnlag.andrePartersUttaksplan.forEach { (_, uttaksplan) ->
            val uttaksperioder = mutableMapOf<LukketPeriode, Prosent>()
            uttaksplan.perioder.forEach { (periode, uttaksPeriodeInfo) -> uttaksperioder[periode] = uttaksPeriodeInfo.gradTilTidslinje() }
            tidslinjer.add(Tidslinje("Annen part", uttaksperioder))
        }
        return tidslinjer
    }

    private fun søknadsperioder():Tidslinje {
        val søknadsperioder = mutableMapOf<LukketPeriode, Prosent?>()
        grunnlag.søknadsperioder.forEach { søktPeriode ->  søknadsperioder[søktPeriode] = null }
        return Tidslinje("Søknadsperioder", søknadsperioder)
    }

    private fun tilsynTidsplinje():Tidslinje {
        val tilsynPerioder = mutableMapOf<LukketPeriode, Prosent>()
        grunnlag.tilsynsperioder.forEach {  tilsynPerioder[it.key] = it.somProsent() }
        return Tidslinje("Tilsyn", tilsynPerioder)
    }

    private fun ferieperioder():Tidslinje {
        val ferier = mutableMapOf<LukketPeriode, Prosent?>()
        grunnlag.lovbestemtFerie.forEach { ferie -> ferier[ferie] = null }
        return Tidslinje("Lovbestemt ferie", ferier)
    }

    private fun arbeidsperioder():List<Tidslinje> {
        val tidslinjer = mutableListOf<Tidslinje>()
        grunnlag.arbeid.forEach { _ ->
            val arbeidsperioder = mutableMapOf<LukketPeriode, Prosent>()
            grunnlag.arbeid.forEach { (_,perioder) ->
                perioder.forEach { (periode, arbeidsforholdPeriodeInfo) ->
                    arbeidsperioder[periode] = BigDecimal(arbeidsforholdPeriodeInfo.taptArbeidstid.toMillis()).setScale(2) / BigDecimal(arbeidsforholdPeriodeInfo.jobberNormalt.toMillis())
                }
            }

            tidslinjer.add(Tidslinje("Arbeid", arbeidsperioder))
        }
        return tidslinjer
    }

    private fun uttaksplan(): Tidslinje {
        val uttaksperioder = mutableMapOf<LukketPeriode, Prosent>()
        uttaksplan.perioder.forEach { (periode, uttaksPeriodeInfo) ->  uttaksperioder[periode] = uttaksPeriodeInfo.gradTilTidslinje()}
        return Tidslinje("Uttaksplan", uttaksperioder)
    }

    private fun UttaksPeriodeInfo.gradTilTidslinje() = if (this is InnvilgetPeriode) grad else Prosent(0)
}