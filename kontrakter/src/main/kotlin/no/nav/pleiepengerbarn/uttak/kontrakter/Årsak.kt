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
        val årsak: InnvilgetÅrsaker,
        val hjemler: Set<Hjemmel>
): Årsak {
    override fun årsak() = årsak.name
    override fun hjemler() = hjemler
}

data class AvslåttÅrsak (
        val årsak: AvslåttÅrsaker,
        val hjemler: Set<Hjemmel>
): Årsak {
    override fun årsak() = årsak.name
    override fun hjemler() = hjemler
}
enum class InnvilgetÅrsaker {
    GRADERT_MOT_TILSYN,
    AVKORTET_MOT_INNTEKT,
    BARNETS_DØDSFALL,
    FULL_DEKNING
}

enum class AvslåttÅrsaker  {
    UTENOM_TILSYNSBEHOV,
    FOR_LAV_GRAD,
    FOR_HØY_TILSYNSGRAD,
    LOVBESTEMT_FERIE,
    IKKE_MEDLEM_I_FOLKETRYGDEN,
    SØKERS_DØDSFALL,
    BARNETS_DØDSFALL,
    SØKERS_ALDER
}
