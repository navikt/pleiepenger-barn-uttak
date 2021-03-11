
create type overse_etablert_tilsyn_arsak as enum ('FOR_LAVT', 'NATTEVÅK', 'BEREDSKAP', 'NATTEVÅK_OG_BEREDSKAP');

alter table UTTAKSPERIODE add column overse_etablert_tilsyn_arsak overse_etablert_tilsyn_arsak default null;