package no.nav.pleiepengerbarn.uttak.kontrakter

enum class Årsak(val oppfylt: Boolean)  {
    //Oppfylt årsaker
    GRADERT_MOT_TILSYN(true),
    AVKORTET_MOT_INNTEKT(true),
    OPPFYLT_PGA_BARNETS_DØDSFALL(true),
    FULL_DEKNING(true),

    //Ikke oppfylt årsaker
    UTENOM_PLEIEBEHOV(false),
    FOR_LAV_GRAD(false),
    FOR_HØY_TILSYNSGRAD(false),
    LOVBESTEMT_FERIE(false),
    SØKERS_DØDSFALL(false),
    BARNETS_DØDSFALL(false),
    SØKERS_ALDER(false),
    INNGANGSVILKÅR_IKKE_OPPFYLT(false)

}
