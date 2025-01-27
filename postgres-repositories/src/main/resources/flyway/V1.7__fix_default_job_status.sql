alter table jobb
alter column status set default 'KLAR';

update jobb
set status = 'KLAR'
where status = 'klar';
