package no.nav.pleiepengerbarn.uttak.kontrakter

enum class Årsak(val oppfylt: Boolean)  {
    //Oppfylt årsaker
    GRADERT_MOT_TILSYN(true),
    AVKORTET_MOT_INNTEKT(true),
    AVKORTET_MOT_SØKERS_ØNSKE(true),
    @Deprecated("Bruk OPPFYLT_PGA_BARNETS_DØDSFALL_6_UKER eller OPPFYLT_PGA_BARNETS_DØDSFALL_12_UKER i stedet for OPPFYLT_PGA_BARNETS_DØDSFALL")
    OPPFYLT_PGA_BARNETS_DØDSFALL(true),
    OPPFYLT_PGA_BARNETS_DØDSFALL_6_UKER(true),
    OPPFYLT_PGA_BARNETS_DØDSFALL_12_UKER(true),
    FULL_DEKNING(true),

    //Ikke oppfylt årsaker
    UTENOM_PLEIEBEHOV(false),
    FOR_LAV_REST_PGA_ETABLERT_TILSYN(false),
    FOR_LAV_REST_PGA_ANDRE_SØKERE(false),
    FOR_LAV_REST_PGA_ETABLERT_TILSYN_OG_ANDRE_SØKERE(false),
    FOR_LAV_TAPT_ARBEIDSTID(false),
    FOR_LAV_ØNSKET_UTTAKSGRAD(false),
    LOVBESTEMT_FERIE(false),
    BARNETS_DØDSFALL(false),
    SØKERS_DØDSFALL(false),
    INNGANGSVILKÅR_IKKE_OPPFYLT(false),
    FOR_LAV_INNTEKT(false),
    FOR_MANGE_DAGER_UTENLANDSOPPHOLD(false),
    MAKS_DAGER_OVERSTEGET(false)

}
