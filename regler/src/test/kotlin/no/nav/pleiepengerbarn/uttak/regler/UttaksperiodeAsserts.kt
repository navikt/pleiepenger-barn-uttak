package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.AvslåttPeriodeÅrsak
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode
import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksperiode
import org.junit.jupiter.api.Assertions

object UttaksperiodeAsserts {

    internal fun sjekkInnvilget(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, utbetalingsgrad:Prosent) {
        Assertions.assertEquals(forventetPeriode.fom, uttaksperiode.periode.fom)
        Assertions.assertEquals(forventetPeriode.tom, uttaksperiode.periode.tom)
        Assertions.assertNotNull(uttaksperiode.uttaksperiodeResultat)
        Assertions.assertEquals(utbetalingsgrad, uttaksperiode.uttaksperiodeResultat.grad)
        Assertions.assertTrue(uttaksperiode.uttaksperiodeResultat.avslåttPeriodeÅrsaker.isEmpty())

    }

    internal fun sjekkAvslått(uttaksperiode: Uttaksperiode, forventetPeriode: LukketPeriode, årsaker:Set<AvslåttPeriodeÅrsak>) {
        Assertions.assertEquals(forventetPeriode.fom, uttaksperiode.periode.fom)
        Assertions.assertEquals(forventetPeriode.tom, uttaksperiode.periode.tom)
        Assertions.assertNotNull(uttaksperiode.uttaksperiodeResultat)
        Assertions.assertEquals(Prosent.ZERO, uttaksperiode.uttaksperiodeResultat.grad)
        Assertions.assertEquals(årsaker, uttaksperiode.uttaksperiodeResultat.avslåttPeriodeÅrsaker)
    }

}