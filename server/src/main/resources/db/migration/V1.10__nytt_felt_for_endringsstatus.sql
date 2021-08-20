
create type endringsstatus as enum ('NY', 'ENDRET', 'UENDRET');

alter table uttaksperiode add column endringsstatus endringsstatus default null;