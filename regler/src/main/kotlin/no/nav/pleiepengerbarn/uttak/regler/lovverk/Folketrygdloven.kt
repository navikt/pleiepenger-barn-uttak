package no.nav.pleiepengerbarn.uttak.regler.lovverk

import no.nav.pleiepengerbarn.uttak.kontrakter.Anvendelse
import no.nav.pleiepengerbarn.uttak.kontrakter.Hjemmel
import java.net.URI

internal object Folketrygdloven {
    // https://lovdata.no/dokument/NL/lov/1997-02-28-19
    internal const val Navn = "Folketrygdloven"
    internal const val Versjon = "LOV-1997-02-28-19"
}

internal object ForskriftOmGraderingAvPleiepenger {
    // https://lovdata.no/forskrift/2017-09-14-1405
    internal const val Navn = "Forskrift om gradering av pleiepenger"
    internal const val Versjon = "FOR-2017-09-14-1405"
}

internal class FolketrygdlovenHenvisning( // TODO ParagrafHenvisning?
        navn: String,
        version: String,
        private val lovdata: URI,
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: Punktum? = null) : Lovhenvisning {
    private val henvisning = listOfNotNull(
            navn,
            version,
            "§ $paragraf",
            ledd?.leddTilTekst(),
            punktum?.punktumTilTekst()
    ).joinToString(" ")

    override fun anvend(anvendelse: Anvendelse) = Hjemmel(
            anvendelse = anvendelse,
            henvisning = henvisning
    )

    override fun toString() = "$henvisning ($lovdata)"
}

internal class FolketrygdlovenKapittelHenvisning(
        kapittel: Int,
        private val lovdata: URI
) : Lovhenvisning {
    private val henvisning = listOf(
            Folketrygdloven.Navn,
            Folketrygdloven.Versjon,
            "Kapittel $kapittel"
    ).joinToString(" ")

    override fun anvend(anvendelse: Anvendelse) = Hjemmel(
            anvendelse = anvendelse,
            henvisning = henvisning
    )

    override fun toString() = "$henvisning ($lovdata)"
}