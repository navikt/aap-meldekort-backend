insert into kelvin_sak (id, saksnummer, saken_gjelder_for)
values (0, '111111', '[2020-01-01, 2026-01-01)'::daterange);

insert into kelvin_person (id, sak_id)
values (0, 0);

insert into kelvin_person_ident(person_id, ident)
values (0, '11111111111');

insert into kelvin_meldeperiode (sak_id, periode)
values
    (0, '[2025-02-10, 2025-02-24)'::daterange),
    (0, '[2025-02-24, 2025-03-10)'::daterange),
    (0, '[2025-03-10, 2025-03-24)'::daterange),
    (0, '[2025-03-24, 2025-04-07)'::daterange);

insert into kelvin_opplysningsbehov (sak_id, periode)
values (0, '[2025-02-13,2025-04-07)'::daterange);