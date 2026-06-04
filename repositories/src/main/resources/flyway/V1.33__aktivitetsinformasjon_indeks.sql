-- Index for mer effektiv bruk av hentAktivitetsInformasjon
create index idx_aktivitetsinformasjon_fagsak_dato
    on aktivitetsinformasjon (fagsak_system, fagsak_nummer, dato, registreringstidspunkt desc);

