create index idx_utfylling_avsluttet_sist_endret
    on utfylling (sist_endret desc)
    where avsluttet = true;

create index idx_varsel_status_sist_endret
    on varsel (status, sist_endret);
