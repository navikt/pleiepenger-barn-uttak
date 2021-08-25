# Pleiepenger barn uttak

Applikasjon som behandler uttaksplaner for ytelsen "pleiepenger for sykt barn". 

Uttaksplaner lagres per behandling, og inneholder alle perioder for denne og tidligere behandlinger på en sak.

## Datamodell

Toppnivået i en uttaksplan heter Uttaksresultat. Et uttaksresultat inneholder uttaksperioder, og hver uttaksperiode har en utbetalingsgrad per arbeidsforhold.

### Uttaksresultat
|Felt|Beskrivelse|
|----|-----------|
|saksnummer|Saksnummer i K9sak som uttaksresultatet tilhører.|
|behandling_id|UUID til behandlingen som uttaksresultatet tilhører.|
|regel_grunnlag|JSON dump av strukturer til klassen RegelGrunnlag. Denne inneholde alle data som er sendt inn til uttak. I tillegg inneholder den uttaksresultat til andre parter knyttet til samme barn.|
|slette|True dersom uttaksresultatet er slettet, ellers false.|
|opprettet_tid|Når uttaksplanen ble opprettet.|
|slettet_tid|Tidspunkt for når uttaksresultat ble markert som slettet.|

### Uttaksperiode

|Felt|Beskrivelse|
|----|-----------|
|uttaksresultat_id|Fremmednøkkel til uttaksresultat perioden er koblet til.|
|fom|Periodens fra og med dato.|
|tom|Periodens til og med dato.|
|pleiebehov|Barnets pleiebehov for denne perioden. Kan være mellom 0 og 200 prosent.|
|etablert_tilsyn|Hvor mange prosent etablert tilsyn har barnet. Kan være mellom 0 og 100 prosent.|
|andre_soekeres_tilsyn|Hvor mye andre søkere knyttet til barnet har brukt av pleiepehovet. Kan være mellom 0 og 200 prosent.|
|tilgjengelig_for_soker|Hvor mye som er tilgjengelig for søk for denne periode. Kan være mellom 0 og 100 prosent.|
|uttaksgrad|Hvor mye søker bruker av pleiebehovet for denne periode. Kan være mellom 0 og 100 prosent.|
|aarsaker|Årsaker til utfall for perioden. En årsak for oppfylte perioder, og en eller flere for ikke oppfylte perioder. Lagres som JSON array.|
|utfall|Periodens utfall.|
|sokers_tapte_arbeidstid|Hvor mange prosent av normal arbeidstid søker har tapt. Kan være mellom 0 og 100 prosent.|
|inngangsvilkar|JSON map av utfall for vurderte inngangsvilkår.|
|knekkpunkt_typer|JSON array av knekkpunkttyper. Disse typene beskriver årsaker til at perioden er knekt opp. Brukes foreløpig bare til debugging.|
|kilde_behandling_uuid|UUID til behandling som var kilden til denne perioden.|
|annen_part|ALENE dersom søker er eneste part på tidspunktet uttaksplanen ble opprettet. MED_ANDRE dersom det er flere parter. VENTER_ANDRE brukes ikke.|
|overse_etablert_tilsyn_arsak|Årsak til at etablert tilsyn skal sees bort ifra.|
|nattevåk|Utfall for om nattevåk vurdering. Null dersom ikke vurdert.|
|beredskap|Utfall for om beredskap vurdering. Null dersom ikke vurdert.|
|andre_sokeres_tilsyn_reberegnet|Er andre søkeres tilsyn rebregnet for pga av endrede fakta.|
|oppgitt_tilsyn|Hvor mye har søker oppgitt av den ønsker som max uttaksgrad. Brukes av søker for å la deler av pleiebehovet være igjen til andre parter.|
|endringsstatus|Angir om en perioder er ny, endret eller uendret i behandlingen som denne uttaksplanen tilhører. Lovlige verdier: NY, ENDRET, UENDRET eller null. Vil være null for perioder som er opprettet før dette feltet ble innført.|


### Utbetalingsgrad

|Felt|Beskrivelse|
|----|-----------|
|uttaksperiode_id|Fremmednøkkel til perioden utbetalingsgraden gjelder for.|
|arbeidstype, organisasjonsnummer, aktoer_id, arbeidsforhold_id| Disse feltene koblet utbetalingsgraden til riktig arbeidsforhold.|
|normal_arbeidstid|Hvor mange timer jobber søker per dag vanligvis i dette arbeidsforholdet.|
|faktisk_arbeidstid|Hvor mange timer jobber søker nå i dette arbeidsforholdet./
|utbetalingsgrad|Hvor mange prosent skal søker har utbetalt for dette arbeidsforholdet.|



## Oppsett for utvikling

Pleiepenger barn uttak bruker testcontaint.org sin modul for PostgreSql for tester og lokal test av server. Port nummer til databasen skrives under oppstart(vil forandre seg fra gang til gang).

Start no.nav.pleiepengerbarn.uttak.server.DevApp for å starte lokal server.

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #sykdom-i-familien.
