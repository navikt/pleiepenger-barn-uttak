package no.nav.pleiepengerbarn.uttak.kontrakter

enum class Årsak(val innvilget: Boolean)  {
    //Oppfylt årsak
    GRADERT_MOT_TILSYN(true),
    AVKORTET_MOT_INNTEKT(true),
    OPPFYLT_PGA_BARNETS_DØDSFALL(true),
    FULL_DEKNING(true),

    //Ikke oppfylt årsak
    UTENOM_TILSYNSBEHOV(false),
    FOR_LAV_GRAD(false),
    FOR_HØY_TILSYNSGRAD(false),
    LOVBESTEMT_FERIE(false),
    IKKE_MEDLEM_I_FOLKETRYGDEN(false),
    SØKERS_DØDSFALL(false),
    BARNETS_DØDSFALL(false),
    SØKERS_ALDER(false),
    INNGANGSVILKÅR_AVSLÅTT(false)

}
