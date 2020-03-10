create sequence if not exists seq_uttaksresultat;

CREATE TABLE IF NOT EXISTS uttaksresultat (
    id bigint NOT NULL PRIMARY KEY,
    saksnummer varchar(19) not null,
    behandling_id uuid not null,
    regel_grunnlag jsonb not null,
    uttaksplan jsonb not null,
    slettet boolean not null,
    opprettet_tid timestamp not null
);


