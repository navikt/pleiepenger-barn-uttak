package no.nav.pleiepengerbarn.uttak.kontrakter

class RegionUtil {
    fun erIEØS(landkode: String?): Boolean {
        val eøsLand: List<String> = listOf(
            "AUT", // Østerrike
            "BEL", // Belgia
            "BGR", // Bulgaria
            "CYP", // Kypros
            "CZE", // Tsjekkia
            "DEU", // Tyskland
            "DNK", // Danmark
            "FRO", // Færøyene - Danmark
            "GRL", // Grønnland - Danmark
            "ESP", // Spania
            "EST", // Estland
            "FIN", // Finland
            "ATA", // Åland - Finland
            "FRA", // Frankrike
            "GRC", // Hellas
            "HRV", // Kroatia
            "HUN", // Ungarn
            "IRL", // Irland
            "ISL", // Island
            "ITA", // Italia
            "LIE", // Liechtenstein
            "LTU", // Litauen
            "LUX", // Luxembourg
            "LVA", // Latvia
            "MLT", // Malta
            "NLD", // Nederland
            "NOR", // Norge
            "POL", // Polen
            "PRT", // Portugal
            "ROU", // Romania
            "SVK", // Slovakia
            "SVN", // Slovenia
            "CHE", // Sveits (Sveits er ikke EØS-land, men omfattes av reglene for koordinering av trygd)
            "SWE" // Sverige
        )

        return eøsLand.contains(landkode)
    }
}
