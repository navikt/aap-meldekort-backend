-- Mellomlagring av utfylling av bruker. Informasjonen i denne tabellen brukes ikke til noe annet
-- enn mellomlagring.

create table utfylling (
    id bigserial primary key,
    ident text not null,
    referanse uuid not null,
    periode daterange not null,
    opprettet timestamp(3) not null,
    sist_endret timestamp(3) not null,
    avsluttet boolean not null,
    flyt text not null,
    aktivt_steg text not null,
    svar jsonb not null
);

create index utfylling_ident_referanse_sist_endret on utfylling(ident, referanse, sist_endret);