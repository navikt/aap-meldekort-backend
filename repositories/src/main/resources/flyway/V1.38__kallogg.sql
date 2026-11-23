create table kallogg (
    id uuid primary key,
    person_id bigint not null references kelvin_person(id),
    entry_type text not null check (entry_type in ('BACKEND_CALL', 'FRONTEND_SNAPSHOT')),
    received_at timestamptz not null,
    completed_at timestamptz,
    method text,
    path text,
    query_parameters jsonb,
    http_status integer check (http_status between 100 and 599),
    response_body text,
    outcome text not null check (outcome in ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'TIMED_OUT', 'INTERRUPTED')),
    html_snapshot text
);

create index idx_kallogg_person_received on kallogg (person_id, received_at);
create index idx_kallogg_received on kallogg (received_at);
