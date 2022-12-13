package no.nav.pleiepengerbarn.uttak.regler

import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan
import java.util.*

data class UttaksplanMedBehandlingUuid constructor(val uttaksplan: Uttaksplan, val behandlingUUID: UUID)
