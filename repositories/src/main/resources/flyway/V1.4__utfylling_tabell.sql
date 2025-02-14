create table arena_utfylling(
    id bigserial primary key,
    ident text not null,
    meldekort_id bigint not null,
    steg text not null,
    flyt text not null,
    skjema_id bigint not null references arena_skjema(id),
    tid_opprettet timestamp(3) not null default current_timestamp
);

alter table arena_skjema
    drop column steg,
    drop column flyt;