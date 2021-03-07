package no.nav.pleiepengerbarn.uttak.regler

enum class Valideringsfeil{
    ANDRE_PARTERS_SAKSNUMMER_DUPLIKAT,
    BEHANDLING_UUID_FORMATFEIL,
    ARBEIDSFORHOLD_ER_IKKE_UNIKE,
    OVERLAPP_MELLOM_ARBEIDSPERIODER,
    OVERLAPP_MELLOM_INNGANGSVILKÅRPERIODER,
    OVERLAPP_MELLOM_FERIEPERIODER,
    OVERLAPP_MELLOM_PLEIEBEHOVPERIODER,
    OVERLAPP_MELLOM_TILSYNSPERIODER,
    OVERLAPP_MELLOM_SØKT_UTTAK,
}
