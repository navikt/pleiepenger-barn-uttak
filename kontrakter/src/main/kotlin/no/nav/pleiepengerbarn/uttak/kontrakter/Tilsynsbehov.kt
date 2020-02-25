package no.nav.pleiepengerbarn.uttak.kontrakter

enum class TilsynsbehovStørrelse {
    PROSENT_100,
    PROSENT_200
}

data class Tilsynsbehov(
        val prosent: TilsynsbehovStørrelse
)
