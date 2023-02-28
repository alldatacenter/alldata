DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME character varying(120) NOT NULL,
    JOB_NAME character varying(200) NOT NULL,
    JOB_GROUP character varying(200) NOT NULL,
    DESCRIPTION character varying(250) NULL,
    JOB_CLASS_NAME character varying(250) NOT NULL,
    IS_DURABLE boolean NOT NULL,
    IS_NONCONCURRENT boolean NOT NULL,
    IS_UPDATE_DATA boolean NOT NULL,
    REQUESTS_RECOVERY boolean NOT NULL,
    JOB_DATA bytea NULL
);

alter table QRTZ_JOB_DETAILS add primary key(SCHED_NAME,JOB_NAME,JOB_GROUP);

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME character varying(120) NOT NULL,
    TRIGGER_NAME character varying(200) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL,
    JOB_NAME character varying(200) NOT NULL,
    JOB_GROUP character varying(200) NOT NULL,
    DESCRIPTION character varying(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE character varying(16) NOT NULL,
    TRIGGER_TYPE character varying(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME character varying(200) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA bytea NULL
) ;

alter table QRTZ_TRIGGERS add primary key(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME character varying(120) NOT NULL,
    TRIGGER_NAME character varying(200) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL
) ;

alter table QRTZ_SIMPLE_TRIGGERS add primary key(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME character varying(120) NOT NULL,
    TRIGGER_NAME character varying(200) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL,
    CRON_EXPRESSION character varying(120) NOT NULL,
    TIME_ZONE_ID character varying(80)
) ;

alter table QRTZ_CRON_TRIGGERS add primary key(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME character varying(120) NOT NULL,
    TRIGGER_NAME character varying(200) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL,
    STR_PROP_1 character varying(512) NULL,
    STR_PROP_2 character varying(512) NULL,
    STR_PROP_3 character varying(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 boolean NULL,
    BOOL_PROP_2 boolean NULL
) ;

alter table QRTZ_SIMPROP_TRIGGERS add primary key(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME character varying(120) NOT NULL,
    TRIGGER_NAME character varying(200) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL,
    BLOB_DATA bytea NULL
) ;

alter table QRTZ_BLOB_TRIGGERS add primary key(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME character varying(120) NOT NULL,
    CALENDAR_NAME character varying(200) NOT NULL,
    CALENDAR bytea NOT NULL
) ;

alter table QRTZ_CALENDARS add primary key(SCHED_NAME,CALENDAR_NAME);

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME character varying(120) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL
) ;

alter table QRTZ_PAUSED_TRIGGER_GRPS add primary key(SCHED_NAME,TRIGGER_GROUP);

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME character varying(120) NOT NULL,
    ENTRY_ID character varying(200) NOT NULL,
    TRIGGER_NAME character varying(200) NOT NULL,
    TRIGGER_GROUP character varying(200) NOT NULL,
    INSTANCE_NAME character varying(200) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE character varying(16) NOT NULL,
    JOB_NAME character varying(200) NULL,
    JOB_GROUP character varying(200) NULL,
    IS_NONCONCURRENT boolean NULL,
    REQUESTS_RECOVERY boolean NULL
) ;

alter table QRTZ_FIRED_TRIGGERS add primary key(SCHED_NAME,ENTRY_ID);

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME character varying(120) NOT NULL,
    INSTANCE_NAME character varying(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL
) ;

alter table QRTZ_SCHEDULER_STATE add primary key(SCHED_NAME,INSTANCE_NAME);

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME character varying(120) NOT NULL,
    LOCK_NAME character varying(40) NOT NULL
) ;

alter table QRTZ_LOCKS add primary key(SCHED_NAME,LOCK_NAME);

DROP TABLE IF EXISTS dv_actual_values;
CREATE TABLE dv_actual_values (
    id bigserial NOT NULL,
    job_execution_id int8 DEFAULT NULL,
    metric_name varchar(255) DEFAULT NULL,
    unique_code varchar(255) DEFAULT NULL,
    actual_value float8 DEFAULT NULL,
    data_time timestamp DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT actual_values_pk PRIMARY KEY (id)
) ;

DROP TABLE IF EXISTS dv_command;
CREATE TABLE dv_command (
    id bigserial NOT NULL ,
    type int2 DEFAULT 0 ,
    parameter text ,
    job_execution_id int8 NOT NULL ,
    priority int4 DEFAULT NULL ,
    create_time timestamp(0) NOT NULL DEFAULT current_timestamp ,
    update_time timestamp(0) NOT NULL DEFAULT current_timestamp ,
    CONSTRAINT command_pk PRIMARY KEY (id)
) ;

DROP TABLE IF EXISTS dv_job;
CREATE TABLE dv_job (
    id bigserial NOT NULL,
    name varchar(255) DEFAULT NULL ,
    type int4 NOT NULL DEFAULT '0',
    datasource_id int8 NOT NULL,
    datasource_id_2 int8 NOT NULL,
    schema_name varchar(128) DEFAULT NULL,
    table_name varchar(128) DEFAULT NULL,
    column_name varchar(128) DEFAULT NULL,
    metric_type varchar(128) DEFAULT NULL,
    execute_platform_type varchar(128) DEFAULT NULL,
    execute_platform_parameter text,
    engine_type varchar(128) DEFAULT NULL,
    engine_parameter text,
    error_data_storage_id int8 DEFAULT NULL,
    parameter text ,
    retry_times int4 DEFAULT NULL ,
    retry_interval int4 DEFAULT NULL ,
    timeout int4 DEFAULT NULL ,
    timeout_strategy int4 DEFAULT NULL ,
    tenant_code int8 DEFAULT NULL ,
    env int8 DEFAULT NULL ,
    create_by int8 DEFAULT NULL ,
    create_time timestamp(0) NOT NULL DEFAULT current_timestamp ,
    update_by int8 DEFAULT NULL ,
    update_time timestamp(0) NOT NULL DEFAULT current_timestamp ,
    CONSTRAINT job_pk PRIMARY KEY (id),
    CONSTRAINT job_un UNIQUE (name,schema_name,table_name,column_name)
) ;

DROP TABLE IF EXISTS dv_job_schedule;
CREATE TABLE dv_job_schedule (
    id bigserial NOT NULL ,
    type varchar(255) NOT NULL,
    param text DEFAULT NULL,
    job_id int8 NOT NULL,
    cron_expression varchar(255) DEFAULT NULL,
    status int2 NOT NULL DEFAULT 1,
    start_time timestamp(0) DEFAULT NULL,
    end_time timestamp(0) DEFAULT NULL,
    create_by int8 DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_by int8 DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT job_schedule_pk PRIMARY KEY (id),
    CONSTRAINT datasource_name_un UNIQUE (name)
);

DROP TABLE IF EXISTS dv_server;
CREATE TABLE dv_server (
    id serial NOT NULL,
    host varchar(255) NOT NULL,
    port int4 NOT NULL,
    create_time timestamp(0) DEFAULT current_timestamp,
    update_time timestamp(0) DEFAULT current_timestamp,
    CONSTRAINT server_pk PRIMARY KEY (id),
    CONSTRAINT server_un UNIQUE (host,port)
) ;

DROP TABLE IF EXISTS dv_job_execution;
CREATE TABLE dv_job_execution (
    id bigserial NOT NULL,
    name varchar(255) NOT NULL,
    job_id int8 NOT NULL DEFAULT '-1',
    job_type int4 NOT NULL DEFAULT '0',
    datasource_id int8 NOT NULL DEFAULT '-1',
    execute_platform_type varchar(128) DEFAULT NULL,
    execute_platform_parameter text,
    engine_type varchar(128) DEFAULT NULL,
    engine_parameter text,
    error_data_storage_type varchar(128) DEFAULT NULL,
    error_data_storage_parameter text,
    error_data_file_name varchar(255) DEFAULT NULL,
    parameter text NOT NULL,
    status int4 DEFAULT NULL,
    retry_times int4 DEFAULT NULL ,
    retry_interval int4 DEFAULT NULL ,
    timeout int4 DEFAULT NULL ,
    timeout_strategy int4 DEFAULT NULL ,
    tenant_code varchar(255) DEFAULT NULL ,
    execute_host varchar(255) DEFAULT NULL ,
    application_id varchar(255) DEFAULT NULL ,
    application_tag varchar(255) DEFAULT NULL ,
    process_id int4 DEFAULT NULL ,
    execute_file_path varchar(255) DEFAULT NULL ,
    log_path varchar(255) DEFAULT NULL ,
    env text,
    submit_time timestamp DEFAULT NULL,
    schedule_time timestamp DEFAULT NULL,
    start_time timestamp DEFAULT NULL,
    end_time timestamp DEFAULT NULL,
    create_time timestamp(0) NOT NULL DEFAULT current_timestamp ,
    update_time timestamp(0) NOT NULL DEFAULT current_timestamp ,
    CONSTRAINT task_pk PRIMARY KEY (id)
);

DROP TABLE IF EXISTS dv_job_execution_result;
CREATE TABLE dv_job_execution_result (
    id bigserial NOT NULL,
    job_execution_id int8 DEFAULT NULL,
    metric_type varchar(255) DEFAULT NULL,
    metric_dimension varchar(255) DEFAULT NULL,
    metric_name varchar(255) DEFAULT NULL,
    database_name varchar(255) DEFAULT NULL,
    table_name varchar(255) DEFAULT NULL,
    column_name varchar(255) DEFAULT NULL,
    actual_value float8 DEFAULT NULL,
    expected_value float8 DEFAULT NULL,
    expected_type varchar(255) DEFAULT NULL,
    result_formula varchar(255) DEFAULT NULL,
    operator varchar(255) DEFAULT NULL,
    threshold float8 DEFAULT NULL,
    state int2 NOT NULL DEFAULT 0,
    create_time timestamp(0) DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT task_result_pk PRIMARY KEY (id)
);

DROP TABLE IF EXISTS dv_datasource;
CREATE TABLE dv_datasource (
    id bigserial NOT NULL ,
    name varchar(255) NOT NULL,
    type varchar(255) NOT NULL,
    param text NOT NULL,
    workspace_id int8 NOT NULL,
    create_by int8 DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_by int8 DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT datasource_pk PRIMARY KEY (id),
    CONSTRAINT datasource_name_un UNIQUE (name,workspace_id)
);

DROP TABLE IF EXISTS dv_workspace;
CREATE TABLE dv_workspace (
    id bigserial NOT NULL,
    name varchar(255) NOT NULL,
    create_by int8 DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_by int8 DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT workspace_pk PRIMARY KEY (id),
    CONSTRAINT workspace_name_un UNIQUE (name)
);

DROP TABLE IF EXISTS dv_user;
CREATE TABLE dv_user (
    id bigserial NOT NULL,
    username varchar(255) NOT NULL,
    password varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    phone varchar(127) DEFAULT NULL,
    admin int2 NOT NULL DEFAULT 0,
    create_time timestamp(0) DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT user_pk PRIMARY KEY (id)
);

DROP TABLE IF EXISTS dv_sla;
CREATE TABLE dv_sla (
    id bigserial NOT NULL,
    workspace_id bigint NOT NULL,
    name varchar(255) NOT NULL,
    description varchar(255) NOT NULL,
    create_by bigint DEFAULT NULL,
    create_time timestamp default current_timestamp,
    update_by bigint DEFAULT NULL,
    update_time timestamp default current_timestamp
);

DROP TABLE IF EXISTS dv_sla_job;
CREATE TABLE dv_sla_job (
    id bigserial NOT NULL,
    sla_id bigint NOT NULL,
    workspace_id bigint NOT NULL,
    job_id bigint NOT NULL,
    create_by bigint DEFAULT NULL,
    create_time timestamp default current_timestamp,
    update_by bigint DEFAULT NULL,
    update_time timestamp default current_timestamp
);

DROP TABLE if EXISTS dv_sla_notification;
CREATE TABLE dv_sla_notification(
    id bigserial NOT NULL,
    type VARCHAR(40) NOT NULL,
    workspace_id bigint NOT NULL,
    sla_id bigint NOT NULL,
    sender_id bigint NOT null,
    config text DEFAULT NULL ,
    create_by bigint DEFAULT NULL,
    create_time timestamp default current_timestamp,
    update_by bigint DEFAULT NULL,
    update_time timestamp default current_timestamp
);

DROP TABLE if exists dv_sla_sender;
CREATE TABLE dv_sla_sender(
    id bigserial NOT NULL,
    type VARCHAR(40) NOT NULL,
    name VARCHAR(255) NOT NULL,
    workspace_id bigint NOT NULL,
    config text NOT NULL,
    create_by bigint DEFAULT NULL,
    create_time timestamp default current_timestamp,
    update_by bigint DEFAULT NULL,
    update_time timestamp default current_timestamp
);

DROP TABLE IF EXISTS dv_env;
CREATE TABLE dv_env (
    id bigserial NOT NULL,
    name varchar(255) NOT NULL,
    env text NOT NULL,
    workspace_id int8 NOT NULL,
    create_by int8 DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_by int8 DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT workspace_pk PRIMARY KEY (id),
    CONSTRAINT workspace_name_un UNIQUE (name)
);

DROP TABLE IF EXISTS dv_tenant;
CREATE TABLE dv_tenant (
    id bigserial NOT NULL,
    tenant varchar(255) NOT NULL,
    workspace_id int8 NOT NULL,
    create_by int8 DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_by int8 DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT workspace_pk PRIMARY KEY (id),
    CONSTRAINT workspace_name_un UNIQUE (name)
);

DROP TABLE IF EXISTS dv_error_data_storage;
CREATE TABLE dv_error_data_storage (
  `id` bigserial NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `param` text NOT NULL,
  `workspace_id` int8 NOT NULL,
  `create_by` int8 NOT NULL,
  `create_time` timestamp(0) DEFAULT NULL,
  `update_by` int8 NOT NULL,
  `update_time` timestamp(0) DEFAULT NULL,
  CONSTRAINT eds_pk PRIMARY KEY (id),
  CONSTRAINT eds_name_un UNIQUE (name,workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS dv_user_workspace;
CREATE TABLE `dv_user_workspace` (
    id bigserial NOT NULL,
    user_id int8 NOT NULL,
    workspace_id int8 NOT NULL,
    role_id int8 NOT NULL,
    create_by int8 DEFAULT NULL,
    create_time timestamp(0) DEFAULT NULL,
    update_by int8 DEFAULT NULL,
    update_time timestamp(0) DEFAULT NULL,
    CONSTRAINT user_workspace_pk PRIMARY KEY (id)
);

INSERT INTO dv_user (id, username, password, email, phone, admin, create_time, update_time) VALUES ('1', 'admin', '$2a$10$9ZcicUYFl/.knBi9SE53U.Nml8bfNeArxr35HQshxXzimbA6Ipgqq', 'admin@gmail.com', NULL, '0', NULL, '2022-05-04 22:08:24');
INSERT INTO dv_workspace (id, name, create_by, create_time, update_by, update_time) VALUES ('1', "admin\'s default", '1', '2022-05-20 23:01:18', '1', '2022-05-20 23:01:21');
INSERT INTO dv_user_workspace (id, user_id, workspace_id, role_id, create_by, create_time, update_by, update_time) VALUES ('1', '1', '1', '1', '1', '2022-07-16 20:34:02', '1', '2022-07-16 20:34:02');