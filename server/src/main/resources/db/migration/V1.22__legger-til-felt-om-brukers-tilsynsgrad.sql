ALTER TABLE uttaksperiode add column if not exists brukers_tilsynsgrad decimal(5,2) check(brukers_tilsynsgrad >= 0 and brukers_tilsynsgrad <=100);

update uttaksperiode set brukers_tilsynsgrad = uttaksgrad;

ALTER TABLE uttaksperiode alter column brukers_tilsynsgrad set not null;
