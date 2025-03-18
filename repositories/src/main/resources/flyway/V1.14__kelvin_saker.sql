create table kelvin_person(
    id bigserial primary key,
    opprettet timestamp(3) not null default current_timestamp
);

create table kelvin_person_ident(
    id bigserial primary key,
    person_id bigint not null references kelvin_person(id),
    ident text not null,
    opprettet timestamp(3) not null default current_timestamp
);
create index uidx_kelvin_person_ident on kelvin_person_ident(ident);

create table kelvin_sak(
    id bigserial primary key,
    person_id bigint not null references kelvin_person(id),
    saksnummer text not null,
    periode daterange not null,
    opprettet timestamp(3) not null default current_timestamp,
    oppdatert timestamp(3) not null default current_timestamp
);

create table kelvin_meldeperiode(
    id bigserial primary key,
    person_id bigint not null references kelvin_person(id),
    sak_id bigint not null references kelvin_sak(id),
    periode daterange not null
);
create unique index uidx_kelvin_meldeperiode on kelvin_meldeperiode(person_id, sak_id, periode);