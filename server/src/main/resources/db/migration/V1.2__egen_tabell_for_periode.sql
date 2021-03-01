create sequence if not exists seq_uttaksperiode;

create type utfall as enum ('OPPFYLT', 'IKKE_OPPFYLT');

create type annen_part as enum ('ALENE', 'MED_ANDRE', 'VENTER_ANDRE');

create table if not exists uttaksperiode (
    id bigint not null primary key,
    uttaksresultat_id bigint references uttaksresultat(id) not null,

    -- Periode
    fom date not null,
    tom date not null,

    -- Gradering mot tilsyn
    pleiebehov decimal(5,2) check (pleiebehov >= 0 and pleiebehov <=200),
    etablert_tilsyn decimal(5,2) check (etablert_tilsyn >= 0 and etablert_tilsyn <=100),
    andre_sokeres_tilsyn decimal(5,2) check (andre_sokeres_tilsyn >= 0 and andre_sokeres_tilsyn <=200),
    tilgjengelig_for_soker decimal(5,2) check (tilgjengelig_for_soker >= 0 and tilgjengelig_for_soker <=200),

    -- Resultat
    uttaksgrad decimal(5,2) check(uttaksgrad >= 0 and uttaksgrad <=100) not null,
    aarsaker jsonb not null,
    utfall utfall not null,

    -- Annet
    sokers_tapte_arbeidstid decimal(5,2) check (sokers_tapte_arbeidstid >= 0 and sokers_tapte_arbeidstid <=100) not null,
    inngangsvilkar jsonb not null,
    knekkpunkt_typer jsonb not null,
    kilde_behandling_uuid uuid not null,
    annen_part annen_part not null
);

create sequence if not exists seq_utbetalingsgrad;

create table if not exists utbetalingsgrad (
    id bigint not null primary key,
    uttaksperiode_id bigint references uttaksperiode(id) not null,

    -- Arbeidsforhold
    arbeidstype varchar(40) not null,
    organisasjonsnummer varchar(9),
    aktoer_id varchar(50),
    arbeidsforhold_id uuid,

    -- Arbeid
    normal_arbeidstid varchar(40) not null,
    faktisk_arbeidstid varchar(40) not null,

    -- Resultat
    utbetalingsgrad decimal(5,2) check (utbetalingsgrad >= 0 and utbetalingsgrad <=100) not null
);
