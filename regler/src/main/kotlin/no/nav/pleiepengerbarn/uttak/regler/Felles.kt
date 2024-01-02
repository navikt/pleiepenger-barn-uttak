package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Prosent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

val NULL_PROSENT: BigDecimal = Prosent.ZERO
val TI_PROSENT = Prosent(10)
val TJUE_PROSENT = Prosent(20)
val ÅTTI_PROSENT = Prosent(80)
val HUNDRE_PROSENT = Prosent(100)

val FULL_DAG: Duration = Duration.ofHours(7).plusMinutes(30)
val TJUE_PROSENT_AV_FULL_DAG: Duration = Duration.ofHours(1).plusMinutes(30);

fun Duration.prosent(prosent: Prosent): Duration = Duration.ofMillis(
        (BigDecimal(this.toMillis()).setScale(8, RoundingMode.HALF_UP)
                * prosent.divide(HUNDRE_PROSENT,8, RoundingMode.HALF_UP)).toLong())
