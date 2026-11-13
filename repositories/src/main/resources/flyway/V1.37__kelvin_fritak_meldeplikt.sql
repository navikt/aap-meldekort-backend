create table kelvin_fritak_meldeplikt
(
    id        bigserial primary key,
    sak_id    bigint references kelvin_sak (id),
    periode   daterange    not null,
    opprettet timestamp(3) not null default current_timestamp(3),
    oppdatert timestamp(3) not null default current_timestamp(3)
);

create unique index uidx_kelvin_fritak_meldeplikt on kelvin_fritak_meldeplikt (sak_id, periode);
