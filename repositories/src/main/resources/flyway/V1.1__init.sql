create table arena_meldekort
(
    id                        bigserial primary key,
    ident                     text      not null,
    meldekort_id              bigint    not null,
    kan_korrigeres            boolean   not null,
    periode                   daterange not null,
    type                      text      not null, -- VANLIG ETTERREGISTRERING KORRIGERING UKJENT
    foo                       text      not null, --- HISTORISK KOMMENDE

    -- for historiske
    begrunnelse_endring       text      null,
    mottatt                   date      null,
    original_meldekort_id     bigint    null,
    beregning_status          text      null,      -- KORRIGERT INNSENDT FERDIG FEILET

    unique (ident, meldekort_id),
    foreign key (ident, original_meldekort_id) references arena_meldekort (ident, meldekort_id)
);

create table arena_skjema
(
    id                            bigserial primary key,
    ident                         text      not null,
    flyt                          text      not null,
    tilstand                      text      not null,
    meldekort_id                  bigint    not null,
    steg                          text      not null,
    meldeperiode                  daterange not null,
    payload_svarer_du_sant        boolean   null,
    payload_har_du_jobbet         boolean   null,
    payload_stemmer_opplysningene boolean   null,

    foreign key (ident, meldekort_id) references arena_meldekort (ident, meldekort_id)
);
create index arena_skjema_ident_meldekort_id_idx on arena_skjema (ident, meldekort_id);

create table arena_skjema_timer_arbeidet
(
    id             bigserial primary key,
    skjema_id      bigserial     not null references arena_skjema (id),
    dato           date          not null,
    timer_arbeidet numeric(3, 1) null
);