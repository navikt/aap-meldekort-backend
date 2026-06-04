-- Index for mer effektiv bruk av hentSak på ident
create index idx_kelvin_person_ident_ident on kelvin_person_ident (ident);

-- Index for mer effektiv bruk av hentSak-filter på saken_gjelder_for
create index idx_kelvin_sak_gjelder_for on kelvin_sak using gist (saken_gjelder_for);

