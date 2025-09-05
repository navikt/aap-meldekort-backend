DO
$$
    BEGIN
        PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');

    END
$$ LANGUAGE 'plpgsql';