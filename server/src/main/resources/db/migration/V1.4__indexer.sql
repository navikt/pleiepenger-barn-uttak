create index IDX_UTTAKSRESULTAT_01 on UTTAKSRESULTAT (saksnummer);
create index IDX_UTTAKSRESULTAT_02 on UTTAKSRESULTAT (behandling_id);

create index IDX_UTTAKSPERIODE_01 on UTTAKSPERIODE (uttaksresultat_id);

create index IDX_UTBETALINGSGRAD_01 on UTBETALINGSGRAD (uttaksperiode_id);
