package no.nav.pleiepengerbarn.uttak.kontrakter

class RegionUtil {
    fun erIEØS(landkode: String?): Boolean {
        val eøsLand: List<String> = listOf(
            "ALA", "AUT", "BEL", "BGR", "CYP", "CZE",
            "DEU", "DNK", "ESP", "EST", "FIN", "FRA", "FRO", "GBR", "GRC", "GRL",
            "HRV", "HUN", "IRL", "ISL", "ITA", "LIE", "LTU", "LUX", "LVA", "MLT",
            "NLD", "NOR", "POL", "PRT", "ROU", "SVK", "SVN", "SWE"
        )

        return eøsLand.contains(landkode)
    }
}
