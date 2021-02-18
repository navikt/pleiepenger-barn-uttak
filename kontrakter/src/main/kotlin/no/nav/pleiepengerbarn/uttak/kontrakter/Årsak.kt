package no.nav.pleiepengerbarn.uttak.kontrakter

enum class Årsak(val oppfylt: Boolean)  {
    //Oppfylt årsaker
    GRADERT_MOT_TILSYN(true),
    AVKORTET_MOT_INNTEKT(true),
    AVKORTET_MOT_SØKERS_ØNSKE(true),
    OPPFYLT_PGA_BARNETS_DØDSFALL(true),
    FULL_DEKNING(true),

    //Ikke oppfylt årsaker
    UTENOM_PLEIEBEHOV(false),
    FOR_LAV_GRAD(false),
    FOR_HØY_TILSYNSGRAD(false),
    LOVBESTEMT_FERIE(false),
    BARNETS_DØDSFALL(false),
    INNGANGSVILKÅR_IKKE_OPPFYLT(false)

}
