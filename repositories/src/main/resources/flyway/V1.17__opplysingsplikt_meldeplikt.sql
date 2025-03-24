create table kelvin_fastsatt_periode
(
    id        bigserial primary key,
    sak_id    bigint references kelvin_sak (id),
    periode   daterange    not null,
    opprettet timestamp(3) not null default current_timestamp(3),
    oppdatert timestamp(3) not null default current_timestamp(3)
);
create unique index uidx_kelvin_fastsatt_periode on kelvin_fastsatt_periode (sak_id, periode);

create table kelvin_opplysningsbehov
(
    id        bigserial primary key,
    sak_id    bigint references kelvin_sak (id),
    periode   daterange    not null,
    opprettet timestamp(3) not null default current_timestamp(3),
    oppdatert timestamp(3) not null default current_timestamp(3)

);
create unique index uidx_kelvin_opplysningsbehov on kelvin_opplysningsbehov(sak_id, periode);
