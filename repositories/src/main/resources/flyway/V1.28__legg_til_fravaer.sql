alter table timer_arbeidet
    add column fravaer text null;

-- first migration to rename table
alter table timer_arbeidet
    rename to aktivitetsinformasjon;

create view timer_arbeidet as
select * from aktivitetsinformasjon;
-- add new migration in next deploy to remove view
-- drop view timer_arbeidet;