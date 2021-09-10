create table if not exists trukket_uttaksperiode (
    id bigint not null primary key,
    uttaksresultat_id bigint references uttaksresultat(id) not null,

    fom date not null,
    tom date not null
);

create sequence if not exists seq_trukket_uttaksperiode;

create index IDX_TRUKKET_UTTAKSPERIODE_01 on TRUKKET_UTTAKSPERIODE (uttaksresultat_id);