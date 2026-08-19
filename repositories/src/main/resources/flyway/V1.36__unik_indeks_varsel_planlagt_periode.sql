create unique index uidx_varsel_planlagt_periode
    on varsel (saksnummer, type_varsel_om, for_periode)
    where status = 'PLANLAGT';
