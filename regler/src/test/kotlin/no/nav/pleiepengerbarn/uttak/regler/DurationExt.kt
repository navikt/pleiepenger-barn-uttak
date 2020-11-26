package no.nav.pleiepengerbarn.uttak.regler

import java.time.Duration

fun Duration.prosent(prosentsats: Long): Duration {
    return this.multipliedBy(prosentsats).dividedBy(100)
}
