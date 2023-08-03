CREATE TABLE catalog_metadata (
    catalog_id             INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    catalog_name           VARCHAR(64) NOT NULL,
    catalog_metastore      VARCHAR(64) NOT NULL,
    storage_configs        CLOB(64m),
    auth_configs           CLOB(64m),
    catalog_properties     CLOB(64m),
    database_count         INT NOT NULL DEFAULT 0,
    table_count            INT NOT NULL DEFAULT 0,
    PRIMARY KEY (catalog_id),
    CONSTRAINT catalog_name_index UNIQUE (catalog_name)
);

CREATE TABLE database_metadata (
    catalog_name           VARCHAR(64) NOT NULL,
    db_name                VARCHAR(128) NOT NULL,
    table_count            INT NOT NULL DEFAULT 0,
    PRIMARY KEY (catalog_name, db_name)
);

CREATE TABLE optimizer (
    token                      VARCHAR(300) NOT NULL,
    resource_id                VARCHAR(100) DEFAULT NULL,
    group_name                 VARCHAR(50),
    container_name             VARCHAR(100),
    start_time                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    touch_time                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    thread_count               INT,
    total_memory               INT,
    properties                 CLOB(64m),
    PRIMARY KEY (token)
);

CREATE TABLE resource (
    resource_id               VARCHAR(100),
    resource_type             SMALLINT DEFAULT 0,
    container_name            VARCHAR(100),
    group_name                VARCHAR(50),
    thread_count              INT,
    total_memory              INT,
    start_time                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    properties                CLOB(64m),
    CONSTRAINT resource_pk PRIMARY KEY (resource_id)
);

CREATE TABLE resource_group (
    group_name       VARCHAR(50) NOT NULL,
    container_name   VARCHAR(100),
    properties       CLOB,
    PRIMARY KEY (group_name)
);

CREATE TABLE table_identifier (
    table_id        BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    catalog_name    VARCHAR(64) NOT NULL,
    db_name         VARCHAR(128) NOT NULL,
    table_name      VARCHAR(128) NOT NULL,
    CONSTRAINT table_identifier_pk PRIMARY KEY (table_id),
    CONSTRAINT table_name_idx UNIQUE (catalog_name, db_name, table_name)
);

CREATE TABLE table_metadata (
    table_id         BIGINT NOT NULL,
    catalog_name     VARCHAR(256) NOT NULL,
    db_name          VARCHAR(256) NOT NULL,
    table_name       VARCHAR(256) NOT NULL,
    format           VARCHAR(32) NOT NULL,
    primary_key      VARCHAR(256),
    sort_key         VARCHAR(256),
    table_location   VARCHAR(256),
    base_location    VARCHAR(256),
    change_location  VARCHAR(256),
    properties       CLOB(64m),
    meta_store_site  CLOB(64m),
    hdfs_site        CLOB(64m),
    core_site        CLOB(64m),
    auth_method      VARCHAR(32),
    hadoop_username  VARCHAR(64),
    krb_keytab       CLOB(64m),
    krb_conf         CLOB(64m),
    krb_principal    CLOB(64m),
    current_schema_id INT NOT NULL DEFAULT 0,
    meta_version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT table_metadata_pk PRIMARY KEY (table_id)
);

CREATE TABLE table_runtime (
    table_id                    BIGINT NOT NULL,
    catalog_name                VARCHAR(64) NOT NULL,
    db_name                     VARCHAR(128) NOT NULL,
    table_name                  VARCHAR(128) NOT NULL,
    current_snapshot_id         BIGINT NOT NULL DEFAULT -1,
    current_change_snapshotId   BIGINT,
    last_optimized_snapshotId   BIGINT NOT NULL DEFAULT -1,
    last_optimized_change_snapshotId   BIGINT NOT NULL DEFAULT -1,
    last_major_optimizing_time  TIMESTAMP,
    last_minor_optimizing_time  TIMESTAMP,
    last_full_optimizing_time   TIMESTAMP,
    optimizing_status           VARCHAR(20) DEFAULT 'Idle',
    optimizing_status_start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    optimizing_process_id       BIGINT NOT NULL,
    optimizer_group             VARCHAR(64) NOT NULL,
    table_config                CLOB(64m),
    optimizing_config           CLOB(64m),
    CONSTRAINT table_runtime_pk PRIMARY KEY (table_id),
    CONSTRAINT table_runtime_table_name_idx UNIQUE (catalog_name, db_name, table_name)
);

CREATE TABLE table_optimizing_process (
    process_id          BIGINT NOT NULL,
    table_id            BIGINT NOT NULL,
    catalog_name        VARCHAR(64) NOT NULL,
    db_name             VARCHAR(128) NOT NULL,
    table_name          VARCHAR(128) NOT NULL,
    target_snapshot_id  BIGINT NOT NULL,
    target_change_snapshot_id  BIGINT NOT NULL,
    status              VARCHAR(10) NOT NULL,
    optimizing_type     VARCHAR(10) NOT NULL,
    plan_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time            TIMESTAMP DEFAULT NULL,
    fail_reason         VARCHAR(4096),
    rewrite_input       BLOB(64m),
    summary             CLOB(64m),
    from_sequence       CLOB(64m),
    to_sequence         CLOB(64m),
    CONSTRAINT table_optimizing_process_pk PRIMARY KEY (process_id)
);

CREATE TABLE task_runtime (
    process_id      BIGINT NOT NULL,
    task_id         INT NOT NULL,
    retry_num       INT,
    table_id        BIGINT NOT NULL,
    partition_data  VARCHAR(128),
    create_time     TIMESTAMP DEFAULT NULL,
    start_time      TIMESTAMP DEFAULT NULL,
    end_time        TIMESTAMP DEFAULT NULL,
    cost_time       BIGINT,
    status          VARCHAR(16),
    fail_reason     VARCHAR(4096),
    optimizer_token VARCHAR(50),
    thread_id       INT,
    rewrite_output  BLOB,
    metrics_summary CLOB,
    properties      CLOB,
    CONSTRAINT task_runtime_pk PRIMARY KEY (process_id, task_id)
);

CREATE TABLE optimizing_task_quota (
    process_id      BIGINT NOT NULL,
    task_id         INT NOT NULL,
    retry_num       INT DEFAULT 0,
    table_id        BIGINT NOT NULL,
    start_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fail_reason     VARCHAR(4096),
    CONSTRAINT optimizing_task_quota_pk PRIMARY KEY (process_id, task_id, retry_num)
);

CREATE TABLE api_tokens (
    id          INT GENERATED ALWAYS AS IDENTITY,
    apikey      VARCHAR(256) NOT NULL,
    secret      VARCHAR(256) NOT NULL,
    apply_time  TIMESTAMP,
    CONSTRAINT api_tokens_pk PRIMARY KEY (id),
    CONSTRAINT api_tokens_un UNIQUE (apikey)
);

CREATE TABLE platform_file (
    id                 INT GENERATED ALWAYS AS IDENTITY,
    file_name          VARCHAR(100) NOT NULL,
    file_content_b64   LONG VARCHAR NOT NULL,
    file_path          VARCHAR(100),
    add_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT platform_file_pk PRIMARY KEY (id)
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

INSERT INTO catalog_metadata(catalog_name,catalog_metastore,storage_configs,auth_configs, catalog_properties) VALUES ('local_catalog','ams','{"storage.type":"hdfs","hive.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==","hadoop.core.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==","hadoop.hdfs.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg=="}','{"auth.type":"simple","auth.simple.hadoop_username":"root"}','{"warehouse":"/tmp/arctic/warehouse","table-formats":"MIXED_ICEBERG"}');
