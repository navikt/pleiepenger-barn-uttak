package no.nav.pleiepengerbarn.uttak.regler

enum class Arbeidstype(val kode: String) {
    ARBEIDSTAKER("AT"),
    FRILANSER("FL"),
    DAGPENGER("DP"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SN"),
    IKKE_YRKESAKTIV("IKKE_YRKESAKTIV"),
    SELVSTENDIG_NÆRINGSDRIVENDE_IKKE_AKTIV("SN_IKKE_AKTIV"),
    FRILANSER_IKKE_AKTIV("FL_IKKE_AKTIV"),
    IKKE_YRKESAKTIV_UTEN_ERSTATNING("IKKE_YRKESAKTIV_UTEN_ERSTATNING"),
    KUN_YTELSE("BA"),
    INAKTIV("MIDL_INAKTIV"),
    SYKEPENGER_AV_DAGPENGER("SP_AV_DP"),
    PSB_AV_DP("PSB_AV_DP")
}

val GRUPPE_SOM_SKAL_SPESIALHÅNDTERES_NYE_REGLER = setOf(
    Arbeidstype.KUN_YTELSE
)

val GRUPPE_SOM_SKAL_SPESIALHÅNDTERES_GAMLE_REGLER = setOf(
    Arbeidstype.IKKE_YRKESAKTIV,
    Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE_IKKE_AKTIV,
    Arbeidstype.FRILANSER_IKKE_AKTIV,
    Arbeidstype.IKKE_YRKESAKTIV_UTEN_ERSTATNING,
    Arbeidstype.KUN_YTELSE
)

val STANDARD_HÅNDTERING_NYE_REGLER = setOf(
    Arbeidstype.ARBEIDSTAKER,
    Arbeidstype.IKKE_YRKESAKTIV,
    Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE_IKKE_AKTIV,
    Arbeidstype.FRILANSER_IKKE_AKTIV,
    Arbeidstype.IKKE_YRKESAKTIV_UTEN_ERSTATNING,
    Arbeidstype.FRILANSER,
    Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE
)

val STANDARD_HÅNDTERING_GAMLE_REGLER = setOf(
    Arbeidstype.ARBEIDSTAKER,
    Arbeidstype.FRILANSER,
    Arbeidstype.SELVSTENDIG_NÆRINGSDRIVENDE
)

val YTELSER = setOf(
    Arbeidstype.SYKEPENGER_AV_DAGPENGER,
    Arbeidstype.PSB_AV_DP,
    Arbeidstype.DAGPENGER,
    Arbeidstype.INAKTIV
)

fun getAktivitetsgruppe(skalBrukeNyeRegler: Boolean): List<Set<Arbeidstype>> {
    if (skalBrukeNyeRegler) {
        return listOf(
            STANDARD_HÅNDTERING_NYE_REGLER,
            YTELSER,
            GRUPPE_SOM_SKAL_SPESIALHÅNDTERES_NYE_REGLER
        )
    }
    return listOf(
        STANDARD_HÅNDTERING_GAMLE_REGLER,
        YTELSER,
        GRUPPE_SOM_SKAL_SPESIALHÅNDTERES_GAMLE_REGLER
    )
}

fun getGruppeSomSkalSpesialhåndteres(skalBrukeNyeRegler: Boolean): Set<Arbeidstype> {
    if (skalBrukeNyeRegler) {
        return GRUPPE_SOM_SKAL_SPESIALHÅNDTERES_NYE_REGLER
    }
    return GRUPPE_SOM_SKAL_SPESIALHÅNDTERES_GAMLE_REGLER
}
