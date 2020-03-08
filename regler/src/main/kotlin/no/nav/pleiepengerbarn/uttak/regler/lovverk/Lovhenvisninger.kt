package no.nav.pleiepengerbarn.uttak.regler.lovverk

import java.net.URI

internal object Lovhenvisninger {

    internal val BorteFraArbeidet = FolketrygdlovenHenvisning(
            navn = Folketrygdloven.Navn,
            version = Folketrygdloven.Versjon,
            lovdata = URI("https://lovdata.no/lov/1997-02-28-19/§9-10"),
            paragraf = "9-10",
            ledd = 1
    )

    internal val InntilToOmsorgspersoner = FolketrygdlovenHenvisning(
            navn = Folketrygdloven.Navn,
            version = Folketrygdloven.Versjon,
            lovdata = URI("https://lovdata.no/lov/1997-02-28-19/§9-10"),
            paragraf = "9-10",
            ledd = 2
    )

    internal val GraderesNedForHverTimeBarnetHarTilsynAvAndre = FolketrygdlovenHenvisning(
            navn = Folketrygdloven.Navn,
            version = Folketrygdloven.Versjon,
            lovdata = URI("https://lovdata.no/lov/1997-02-28-19/§9-11"),
            paragraf = "9-11",
            ledd = 2
    )
    internal val YtelsenKanGraderesNedTil20Prosent = FolketrygdlovenHenvisning(
            navn = Folketrygdloven.Navn,
            version = Folketrygdloven.Versjon,
            lovdata = URI("https://lovdata.no/lov/1997-02-28-19/§9-11"),
            paragraf = "9-11",
            ledd = 2,
            punktum = 1
    )

    internal val MaksÅttiProsentTilsynAvAndre = FolketrygdlovenHenvisning(
            navn = Folketrygdloven.Navn,
            version = Folketrygdloven.Versjon,
            lovdata = URI("https://lovdata.no/lov/1997-02-28-19/§9-11"),
            paragraf = "9-11",
            ledd = 2,
            punktum = 2
    )

    internal val NormalArbeidsdag = FolketrygdlovenHenvisning(
            navn = ForskriftOmGraderingAvPleiepenger.Navn,
            version = ForskriftOmGraderingAvPleiepenger.Versjon,
            lovdata = URI("https://lovdata.no/forskrift/2017-09-14-1405/§2"),
            paragraf = "2",
            ledd = 2
    )

    internal val TilsynPåMindreEnn10ProsentSkalIkkeMedregnes = FolketrygdlovenHenvisning(
            navn = ForskriftOmGraderingAvPleiepenger.Navn,
            version = ForskriftOmGraderingAvPleiepenger.Versjon,
            lovdata = URI("https://lovdata.no/forskrift/2017-09-14-1405/§2"),
            paragraf = "2",
            ledd = 3,
            punktum = 2
    )

    internal val FastsettingAvTilsynsgradOgPleiepengegrad = FolketrygdlovenHenvisning(
            navn = ForskriftOmGraderingAvPleiepenger.Navn,
            version = ForskriftOmGraderingAvPleiepenger.Versjon,
            lovdata = URI("https://lovdata.no/forskrift/2017-09-14-1405/§3"),
            paragraf = "3",
            ledd = 3
    )
}