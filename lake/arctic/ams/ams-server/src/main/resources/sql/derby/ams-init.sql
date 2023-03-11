
CREATE TABLE optimize_group (
    group_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    name varchar(50) unique NOT NULL,
    scheduling_policy varchar(20),
    properties clob(64m),
    container varchar(64) DEFAULT NULL,
    PRIMARY KEY (group_id)
    );

CREATE TABLE catalog_metadata(
    catalog_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    catalog_name varchar(64) unique NOT NULL,
    catalog_type varchar(64) NOT NULL,
    storage_configs clob(64m),
    auth_configs clob(64m),
    catalog_properties clob(64m),
    PRIMARY KEY (catalog_id)
    );

CREATE TABLE optimizer (
    optimizer_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    optimizer_name varchar(1024) DEFAULT NULL,
    queue_id bigint DEFAULT NULL,
    queue_name varchar(1024) DEFAULT NULL,
    optimizer_start_time varchar(1024) DEFAULT NULL,
    optimizer_fail_time varchar(1024) DEFAULT NULL,
    optimizer_status varchar(16) DEFAULT NULL,
    core_number bigint DEFAULT NULL,
    memory bigint DEFAULT NULL,
    parallelism bigint DEFAULT NULL,
    jobmanager_url varchar(1024) DEFAULT NULL,
    optimizer_instance blob,
    optimizer_state_info clob(64m),
    container varchar(50) DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (optimizer_id)
    );

CREATE TABLE container_metadata (
    name varchar(64) NOT NULL,
    type varchar(64) NOT NULL,
    properties clob(64m),
    PRIMARY KEY (name, type)
);

CREATE TABLE snapshot_info_cache (
    table_identifier varchar(384) NOT NULL,
    snapshot_id bigint NOT NULL,
    snapshot_sequence bigint NOT NULL DEFAULT -1,
    parent_snapshot_id bigint NOT NULL,
    action varchar(64) DEFAULT NULL,
    inner_table varchar(64) NOT NULL,
    producer varchar(64) NOT NULL DEFAULT 'INGESTION',
    file_size bigint NOT NULL DEFAULT 0,
    file_count int NOT NULL DEFAULT 0,
    commit_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (table_identifier,inner_table,snapshot_id)
    );

CREATE TABLE optimize_task (
    trace_id varchar(40) NOT NULL,
    optimize_type varchar(10) NOT NULL,
    catalog_name varchar(64) NOT NULL,
    db_name varchar(128) NOT NULL,
    table_name varchar(128) NOT NULL,
    partition varchar(128) DEFAULT NULL,
    task_commit_group varchar(40) DEFAULT NULL,
    to_sequence bigint NOT NULL WITH DEFAULT -1,
    from_sequence bigint NOT NULL WITH DEFAULT -1,
    create_time timestamp DEFAULT NULL,
    properties clob(64m),
    queue_id bigint NOT NULL,
    insert_files bigint DEFAULT NULL,
    delete_files bigint DEFAULT NULL,
    base_files bigint DEFAULT NULL,
    pos_delete_files bigint DEFAULT NULL,
    insert_file_size bigint DEFAULT NULL,
    delete_file_size bigint DEFAULT NULL,
    base_file_size bigint DEFAULT NULL,
    pos_delete_file_size bigint DEFAULT NULL,
    source_nodes varchar(2048) DEFAULT NULL,
    is_delete_pos_delete int DEFAULT NULL,
    task_plan_group varchar(40) DEFAULT NULL,
    status varchar(16) DEFAULT NULL,
    pending_time timestamp DEFAULT NULL,
    execute_time timestamp DEFAULT NULL,
    prepared_time timestamp DEFAULT NULL,
    report_time timestamp DEFAULT NULL,
    commit_time timestamp DEFAULT NULL,
    job_type varchar(16) DEFAULT NULL,
    job_id varchar(32) DEFAULT NULL,
    attempt_id varchar(40) DEFAULT NULL,
    retry bigint DEFAULT NULL,
    fail_reason varchar(4096) DEFAULT NULL,
    fail_time timestamp DEFAULT NULL,
    new_file_size bigint DEFAULT NULL,
    new_file_cnt bigint DEFAULT NULL,
    cost_time bigint DEFAULT NULL,
    PRIMARY KEY (trace_id)
);

CREATE TABLE optimize_table_runtime (
    catalog_name varchar(64) NOT NULL,
    db_name varchar(128) NOT NULL,
    table_name varchar(128) NOT NULL,
    current_snapshot_id bigint NOT NULL DEFAULT -1,
    latest_major_optimize_time clob(64m),
    latest_full_optimize_time clob(64m),
    latest_minor_optimize_time clob(64m),
    latest_task_plan_group varchar(40) DEFAULT NULL,
    optimize_status varchar(20) DEFAULT 'Idle',
    optimize_status_start_time timestamp DEFAULT NULL,
    current_change_snapshotId bigint DEFAULT NULL,
    PRIMARY KEY (catalog_name,db_name,table_name)
);

CREATE TABLE table_metadata (
    catalog_name varchar(64) NOT NULL,
    db_name varchar(128) NOT NULL,
    table_name varchar(128) NOT NULL,
    primary_key varchar(256) DEFAULT NULL,
    sort_key varchar(256) DEFAULT NULL,
    table_location varchar(256) DEFAULT NULL,
    base_location varchar(256) DEFAULT NULL,
    delta_location varchar(256) DEFAULT NULL,
    properties clob(64m),
    meta_store_site clob(64m),
    hdfs_site clob(64m),
    core_site clob(64m),
    hbase_site clob(64m),
    auth_method varchar(32) DEFAULT NULL,
    hadoop_username varchar(64) DEFAULT NULL,
    krb_keytab clob(64m),
    krb_conf clob(64m),
    krb_principal clob(64m),
    current_tx_id bigint DEFAULT 0,
    cur_schema_id   int DEFAULT 0,
    PRIMARY KEY (catalog_name, db_name, table_name)
);

CREATE TABLE file_info_cache (
    primary_key_md5 varchar(64) NOT NULL,
    table_identifier varchar(384) NOT NULL,
    add_snapshot_id bigint NOT NULL,
    parent_snapshot_id bigint NOT NULL,
    delete_snapshot_id bigint DEFAULT NULL,
    add_snapshot_sequence bigint NOT NULL DEFAULT -1,
    inner_table varchar(64) DEFAULT NULL,
    file_path varchar(400) NOT NULL,
    file_type varchar(64) DEFAULT NULL,
    producer varchar(64) NOT NULL DEFAULT 'INGESTION',
    file_size bigint DEFAULT NULL,
    file_mask bigint DEFAULT NULL,
    file_index bigint DEFAULT NULL,
    spec_id bigint DEFAULT NULL,
    record_count bigint DEFAULT NULL,
    partition_name varchar(256) DEFAULT NULL,
    action varchar(64) DEFAULT NULL,
    commit_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (primary_key_md5)
);

CREATE TABLE optimize_file (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    optimize_type varchar(10) NOT NULL,
    trace_id varchar(40) NOT NULL,
    content_type varchar(32) NOT NULL,
    is_target int DEFAULT 0,
    file_content blob(60000) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE optimize_history (
    history_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    catalog_name varchar(64) NOT NULL,
    db_name varchar(128) NOT NULL,
    table_name varchar(128) NOT NULL,
    optimize_range varchar(10) NOT NULL,
    visible_time timestamp DEFAULT NULL,
    commit_time timestamp DEFAULT NULL,
    plan_time timestamp DEFAULT NULL,
    duration bigint DEFAULT NULL,
    total_file_cnt_before int NOT NULL,
    total_file_size_before bigint NOT NULL,
    insert_file_cnt_before int NOT NULL,
    insert_file_size_before bigint NOT NULL,
    delete_file_cnt_before int NOT NULL,
    delete_file_size_before bigint NOT NULL,
    base_file_cnt_before int NOT NULL,
    base_file_size_before bigint NOT NULL,
    pos_delete_file_cnt_before int NOT NULL,
    pos_delete_file_size_before bigint NOT NULL,
    total_file_cnt_after int NOT NULL,
    total_file_size_after bigint NOT NULL,
    snapshot_id bigint DEFAULT NULL,
    total_size bigint DEFAULT NULL,
    added_files int DEFAULT NULL,
    removed_files int DEFAULT NULL,
    added_records bigint DEFAULT NULL,
    removed_records bigint DEFAULT NULL,
    added_files_size bigint DEFAULT NULL,
    removed_files_size bigint DEFAULT NULL,
    total_files bigint DEFAULT NULL,
    total_records bigint DEFAULT NULL,
    partition_cnt int NOT NULL,
    partitions clob(64m),
    partition_optimized_sequence clob(64m),
    optimize_type varchar(10) NOT NULL,
    PRIMARY KEY (history_id)
);


CREATE TABLE table_transaction_meta (
    table_identifier varchar(384) NOT NULL,
    transaction_id bigint NOT NULL,
    signature varchar(256) NOT NULL,
    commit_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (table_identifier, transaction_id),
    UNIQUE (table_identifier, signature)
);

CREATE TABLE optimize_task_history (
    task_trace_id     varchar(50) NOT NULL,
    retry             int NOT NULL,
    task_plan_group   varchar(40) NOT NULL,
    catalog_name      varchar(64) NOT NULL,
    db_name           varchar(128) NOT NULL,
    table_name        varchar(128) NOT NULL,
    start_time        timestamp DEFAULT NULL,
    end_time          timestamp DEFAULT NULL,
    cost_time         bigint DEFAULT NULL,
    queue_id          int DEFAULT NULL,
    PRIMARY KEY (task_trace_id, retry)
);

CREATE TABLE database_metadata (
    db_id int NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    catalog_name varchar(64) NOT NULL,
    db_name varchar(128) NOT NULL,
    PRIMARY KEY (db_id),
    UNIQUE (catalog_name,db_name)
);

CREATE TABLE api_tokens (
    id int NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 33, INCREMENT BY 1),
    apikey varchar(256) NOT NULL,
    secret varchar(256) NOT NULL,
    apply_time timestamp DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE (apikey)
);

CREATE TABLE ddl_record
(
    table_identifier varchar(384) NOT NULL,
    ddl        clob(64m),
    ddl_type       varchar(256) NOT NULL,
    commit_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE platform_file_info (
  id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  file_name varchar(100) NOT NULL,
  file_content_b64 varchar(32672) NOT NULL,
  file_path varchar(100) DEFAULT NULL,
  add_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE table_blocker (
  blocker_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  catalog_name varchar(64) NOT NULL,
  db_name varchar(128) NOT NULL,
  table_name varchar(128) NOT NULL,
  operations varchar(128) NOT NULL,
  create_time timestamp DEFAULT NULL,
  expiration_time timestamp DEFAULT NULL,
  properties clob(64m),
  PRIMARY KEY (blocker_id)
);

INSERT INTO catalog_metadata(catalog_name,catalog_type,storage_configs,auth_configs, catalog_properties) VALUES ('local_catalog','ams','{"storage.type":"hdfs","hive.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==","hadoop.core.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==","hadoop.hdfs.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg=="}','{"auth.type":"simple","auth.simple.hadoop_username":"root"}','{"warehouse":"/tmp/arctic/warehouse","table-formats":"MIXED_ICEBERG"}');