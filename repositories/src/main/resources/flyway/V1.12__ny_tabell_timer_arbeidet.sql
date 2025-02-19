create table timer_arbeidet (
    id bigserial primary key,
    ident text not null,
    fagsak_system text not null,
    fagsak_nummer text not null,
    utfylling_referanse uuid not null,
    registreringstidspunkt timestamp not null,
    dato date not null,
    timer_arbeidet numeric(3, 1)
);