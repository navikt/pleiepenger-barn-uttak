create sequence if not exists seq_kvote_info;

create table if not exists kvote_info (
    id bigint not null primary key,
    uttaksresultat_id bigint references uttaksresultat(id) not null,
    max_dato date,
    forbrukt_kvote_hittil decimal(8,2) default '0.00',
    forbrukt_kvote_denne_behandlingen decimal(8,2) default '0.00'
);

create index IDX_KVOTE_INFO_01 on KVOTE_INFO (uttaksresultat_id);