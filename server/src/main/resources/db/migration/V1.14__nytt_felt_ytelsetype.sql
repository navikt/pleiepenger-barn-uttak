CREATE TYPE ytelsetype AS enum ('PSB', 'PLS');

ALTER TABLE uttaksresultat ADD COLUMN ytelsetype ytelsetype NOT NULL DEFAULT 'PSB';