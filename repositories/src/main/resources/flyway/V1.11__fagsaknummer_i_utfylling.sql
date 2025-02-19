alter table utfylling
add column fagsaknummer text not null default '',
add column fagsystem text not null default '';

alter table utfylling
alter column fagsaknummer drop default,
alter column fagsystem drop default;