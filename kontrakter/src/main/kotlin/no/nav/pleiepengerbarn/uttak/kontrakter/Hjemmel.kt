package no.nav.pleiepengerbarn.uttak.kontrakter

typealias Henvisning = String
typealias Anvendelse = String
typealias Utfallnavn = String

data class Hjemmel(
        val henvisning: Henvisning,
        val anvendelse: Anvendelse
)

interface Utfall {
    fun navn() : Utfallnavn
    fun hjemler() : Set<Hjemmel>
}

data class InnvilgelsesUtfall (
        private val navn: InnvilgelsesUtfallnavn,
        private val hjemler: Set<Hjemmel>
): Utfall {
    override fun navn() = navn.name
    override fun hjemler() = hjemler
}

data class AvslagsUtfall (
        val navn: AvslagsUtfallnavn,
        val hjemler: Set<Hjemmel>
): Utfall {
    override fun navn() = navn.name
    override fun hjemler() = hjemler
}
enum class InnvilgelsesUtfallnavn {
    GradertMotTilsyn,
    AvkortetMotInntekt,
    BarnetsDødsfall
}

enum class AvslagsUtfallnavn  {
    UtenomTilsynsbehov,
    ForLavGradPleiepengegrad,
    Ferie,
    IkkeMedlemIFolketrygden,
    SøkersDødsfall
}
