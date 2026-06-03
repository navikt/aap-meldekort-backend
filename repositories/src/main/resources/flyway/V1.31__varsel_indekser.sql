-- Indeks for hentVarsler og slettPlanlagteVarsler
create index idx_varsel_saksnummer on varsel (saksnummer, type_varsel_om, status);

-- Indeks for hentVarslerForUtsending
create index idx_varsel_utsending on varsel (sendingstidspunkt, status);