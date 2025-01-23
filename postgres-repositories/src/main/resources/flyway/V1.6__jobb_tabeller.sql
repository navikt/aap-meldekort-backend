create table jobb
(
    id            bigserial                              not null primary key,
    status        varchar(50)  default 'klar'            not null,
    type          varchar(50)                            not null,
    neste_kjoring timestamp(3)                           not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    parameters    text,
    payload       text,
    sak_id        bigint       default null,
    behandling_id bigint       default null
);

create index idx_jobb_status on jobb (status, sak_id, behandling_id, neste_kjoring);

create index idx_jobb_sak on jobb (sak_id);

create index idx_jobb_behandling on jobb (behandling_id);

create index idx_jobb_sak_behandling on jobb (sak_id, behandling_id);

create index idx_jobb_type on jobb (type);

create index idx_jobb_status_neste_kjoring on jobb (status, neste_kjoring);

create index idx_jobb_neste_kjoring on jobb (neste_kjoring);

create index idx_jobb_neste_kjoring_sak_behandling on jobb (sak_id, behandling_id, neste_kjoring);


create table jobb_historikk
(
    id            bigserial                              not null primary key,
    jobb_id       bigint                                 not null references jobb (id),
    status        varchar(50)                            not null,
    feilmelding   text                                   null,
    opprettet_tid timestamp(3) default current_timestamp not null
);

create index idx_jobb_historikk_jobb_id_status on jobb_historikk (jobb_id, status);

create index idx_jobb_historikk_status on jobb_historikk (status);

create index idx_jobb_historikk_tid on jobb_historikk (opprettet_tid);
