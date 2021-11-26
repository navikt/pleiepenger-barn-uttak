ALTER TABLE uttaksperiode RENAME COLUMN pleiebehov to pleiebehov_gammel;

ALTER TABLE uttaksperiode ADD COLUMN pleiebehov decimal(6,2) check (pleiebehov >= 0 and pleiebehov <=6000);