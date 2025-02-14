alter table arena_meldekort rename column foo to tilstand;

alter table arena_skjema add column tid_opprettet timestamp(3) not null default current_timestamp