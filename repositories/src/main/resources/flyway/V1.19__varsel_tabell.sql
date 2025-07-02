create table varsel
(
    id                bigserial primary key,
    varsel_id         uuid         not null,
    type_varsel       text         not null,
    type_varsel_om    text         not null,
    saksnummer        text         not null references kelvin_sak (saksnummer),
    sendingstidspunkt timestamp(3) not null,
    status            text         not null,
    for_periode       daterange    not null,
    opprettet         timestamp(3) not null,
    sist_endret       timestamp(3) not null
);

create unique index uidx_varsel_referanse on varsel (varsel_id);
