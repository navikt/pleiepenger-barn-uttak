package no.nav.pleiepengerbarn.uttak.kontrakter

typealias Henvisning = String
typealias Anvendelse = String
typealias Årsaknavn = String

data class Hjemmel(
        val henvisning: Henvisning,
        val anvendelse: Anvendelse
)

interface Årsak {
    fun årsak() : Årsaknavn
    fun hjemler() : Set<Hjemmel>
}

data class InnvilgetÅrsak (
        private val årsak: InnvilgetÅrsaker,
        private val hjemler: Set<Hjemmel>
): Årsak {
    override fun årsak() = årsak.name
    override fun hjemler() = hjemler
}

data class AvlslåttÅrsak (
        val årsak: AvslåttÅrsaker,
        val hjemler: Set<Hjemmel>
): Årsak {
    override fun årsak() = årsak.name
    override fun hjemler() = hjemler
}
enum class InnvilgetÅrsaker {
    GradertMotTilsyn,
    AvkortetMotInntekt,
    BarnetsDødsfall
}

enum class AvslåttÅrsaker  {
    UtenomTilsynsbehov,
    ForLavGradPleiepengegrad,
    Ferie,
    IkkeMedlemIFolketrygden,
    SøkersDødsfall
}
