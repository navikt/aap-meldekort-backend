-- Indeks for hentVarsler og slettPlanlagteVarsler
create index idx_varsel_saksnummer on varsel (saksnummer, type_varsel_om, status);

-- Indeks for hentVarslerForUtsending
create index idx_varsel_utsending on varsel (sendingstidspunkt, status);

-- Index for mer effektiv bruk av hentSak på ident
create index idx_kelvin_person_ident_ident on kelvin_person_ident (ident);

-- Index for mer effektiv bruk av hentSak-filter på saken_gjelder_for
create index idx_kelvin_sak_gjelder_for on kelvin_sak using gist (saken_gjelder_for);

-- Index for mer effektiv bruk av hentAktivitetsInformasjon
create index idx_aktivitetsinformasjon_fagsak_dato
    on aktivitetsinformasjon (fagsak_system, fagsak_nummer, dato, registreringstidspunkt desc);

