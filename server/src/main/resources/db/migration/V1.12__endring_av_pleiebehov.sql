ALTER TABLE uttaksperiode DROP CONSTRAINT uttaksperiode_pleiebehov_check;
ALTER TABLE uttaksperiode ALTER COLUMN pleiebehov TYPE DECIMAL(6,2);

ALTER TABLE uttaksperiode ADD CONSTRAINT uttaksperiode_pleiebehov_check CHECK (pleiebehov >= 0 and pleiebehov <=6000);