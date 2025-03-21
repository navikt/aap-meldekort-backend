drop index uidx_kelvin_meldeperiode;
drop index uidx_kelvin_person_ident;

alter table kelvin_sak
drop column person_id,
drop column periode;

alter table kelvin_person
add column sak_id bigserial not null references kelvin_sak(id),
add column oppdatert timestamp(3) not null default current_timestamp(3);

alter table kelvin_person_ident
add column oppdatert timestamp(3) not null default current_timestamp(3);

alter table kelvin_meldeperiode
drop column person_id,
add column opprettet timestamp(3) not null default current_timestamp(3),
add column oppdatert timestamp(3) not null default current_timestamp(3);

create unique index uidx_kelvin_meldeperiode on kelvin_meldeperiode (sak_id, periode);
create unique index uidx_kelvin_sak_saknummer on kelvin_sak(saksnummer);
create unique index uidx_kelvin_person on kelvin_person(sak_id);
create unique index uidx_kelvin_person_ident on kelvin_person_ident(person_id, ident);
