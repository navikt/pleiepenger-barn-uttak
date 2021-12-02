create type grunnlagstype as enum ('UTTAKSGRUNNLAG', 'ENDRINGSGRUNNLAG');

alter table uttaksresultat add column grunnlagstype grunnlagstype default 'UTTAKSGRUNNLAG';
