CREATE TABLE `catalog_metadata`
(
    `catalog_id`             int(11) NOT NULL AUTO_INCREMENT,
    `catalog_name`           varchar(64) NOT NULL COMMENT 'catalog name',
    `catalog_metastore`      varchar(64) NOT NULL COMMENT 'catalog type like hms/ams/hadoop/custom',
    `storage_configs`        mediumtext COMMENT 'base64 code of storage configs',
    `auth_configs`           mediumtext COMMENT 'base64 code of auth configs',
    `catalog_properties`     mediumtext COMMENT 'catalog properties',
    `database_count`         int(11) NOT NULL default 0,
    `table_count`            int(11) NOT NULL default 0,
    PRIMARY KEY (`catalog_id`),
    UNIQUE KEY `catalog_name_index` (`catalog_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'catalog metadata';

CREATE TABLE `database_metadata`
(
    `catalog_name`           varchar(64) NOT NULL COMMENT 'catalog name',
    `db_name`                varchar(128) NOT NULL COMMENT 'database name',
    `table_count`            int(11) NOT NULL default 0,
    PRIMARY KEY (`catalog_name`, `db_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'database metadata';


CREATE TABLE `optimizer`
(
    `token`                      varchar(50) NOT NULL,
    `resource_id`                varchar(100) DEFAULT NULL  COMMENT 'optimizer instance id',
    `group_name`                 varchar(50) DEFAULT NULL COMMENT 'group/queue name',
    `container_name`             varchar(100) DEFAULT NULL  COMMENT 'container name',
    `start_time`                 timestamp not null default CURRENT_TIMESTAMP COMMENT 'optimizer start time',
    `touch_time`                 timestamp not null default CURRENT_TIMESTAMP COMMENT 'update time',
    `thread_count`               int(11) DEFAULT NULL COMMENT 'total number of all CPU resources',
    `total_memory`               bigint(30) DEFAULT NULL COMMENT 'optimizer use memory size',
    `properties`                 mediumtext COMMENT 'optimizer state info, contains like yarn application id and flink job id',
    PRIMARY KEY (`token`),
    KEY  `resource_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'resource table';

CREATE TABLE `resource`
(
    `resource_id`               varchar(100) NOT NULL  COMMENT 'optimizer instance id',
    `resource_type`             tinyint(4) DEFAULT 0 COMMENT 'resource type like optimizer/ingestor',
    `container_name`            varchar(100) DEFAULT NULL  COMMENT 'container name',
    `group_name`                varchar(50) DEFAULT NULL COMMENT 'queue name',
    `thread_count`              int(11) DEFAULT NULL COMMENT 'total number of all CPU resources',
    `total_memory`              bigint(30) DEFAULT NULL COMMENT 'optimizer use memory size',
    `start_time`                timestamp not null default CURRENT_TIMESTAMP COMMENT 'optimizer start time',
    `properties`                mediumtext COMMENT 'optimizer instance properties',
    PRIMARY KEY (`resource_id`),
    KEY  `resource_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Optimizer instance info';

CREATE TABLE `resource_group`
(
    `group_name`       varchar(50) NOT NULL  COMMENT 'Optimize group name',
    `container_name`   varchar(100) DEFAULT NULL  COMMENT 'Container name',
    `properties`       mediumtext  COMMENT 'Properties',
    PRIMARY KEY (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Group to divide optimize resources';

CREATE TABLE `table_identifier`
(
    `table_id`        bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Auto increment id',
    `catalog_name`    varchar(64) NOT NULL COMMENT 'Catalog name',
    `db_name`         varchar(128) NOT NULL COMMENT 'Database name',
    `table_name`      varchar(128) NOT NULL COMMENT 'Table name',
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `table_name_index` (`catalog_name`,`db_name`,`table_name`)
);

CREATE TABLE `table_metadata`
(
    `table_id`        bigint(20) NOT NULL COMMENT 'table id',
    `catalog_name`    varchar(64) NOT NULL COMMENT 'Catalog name',
    `db_name`         varchar(128) NOT NULL COMMENT 'Database name',
    `table_name`      varchar(128) NOT NULL COMMENT 'Table name',
    `format`          varchar(32)  NOT NULL COMMENT "format",
    `primary_key`     varchar(256) DEFAULT NULL COMMENT 'Primary key',
    `sort_key`        varchar(256) DEFAULT NULL COMMENT 'Sort key',
    `table_location`  varchar(256) DEFAULT NULL COMMENT 'Table location',
    `base_location`   varchar(256) DEFAULT NULL COMMENT 'Base table location',
    `change_location` varchar(256) DEFAULT NULL COMMENT 'change table location',
    `properties`      mediumtext COMMENT 'Table properties',
    `meta_store_site` mediumtext COMMENT 'base64 code of meta store site',
    `hdfs_site`       mediumtext COMMENT 'base64 code of hdfs site',
    `core_site`       mediumtext COMMENT 'base64 code of core site',
    `auth_method`     varchar(32)  DEFAULT NULL COMMENT 'auth method like KERBEROS/SIMPLE',
    `hadoop_username` varchar(64)  DEFAULT NULL COMMENT 'hadoop username when auth method is SIMPLE',
    `krb_keytab`      text COMMENT 'kerberos keytab when auth method is KERBEROS',
    `krb_conf`        text COMMENT 'kerberos conf when auth method is KERBEROS',
    `krb_principal`   text COMMENT 'kerberos principal when auth method is KERBEROS',
    `current_schema_id`   int(11) NOT NULL DEFAULT 0 COMMENT 'current schema id',
    `meta_version`    bigint(20) NOT NULL DEFAULT 0,
    PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Table metadata';

CREATE TABLE `table_runtime`
(
    `table_id`                      bigint(20) NOT NULL,
    `catalog_name`                  varchar(64) NOT NULL COMMENT 'Catalog name',
    `db_name`                       varchar(128) NOT NULL COMMENT 'Database name',
    `table_name`                    varchar(128) NOT NULL COMMENT 'Table name',
    `current_snapshot_id`           bigint(20) NOT NULL DEFAULT '-1' COMMENT 'Base table current snapshot id',
    `current_change_snapshotId`     bigint(20) DEFAULT NULL COMMENT 'Change table current snapshot id',
    `last_optimized_snapshotId`     bigint(20) NOT NULL DEFAULT '-1' COMMENT 'last optimized snapshot id',
    `last_optimized_change_snapshotId`     bigint(20) NOT NULL DEFAULT '-1' COMMENT 'last optimized change snapshot id',
    `last_major_optimizing_time`    timestamp NULL DEFAULT NULL COMMENT 'Latest Major Optimize time for all partitions',
    `last_minor_optimizing_time`    timestamp NULL DEFAULT NULL COMMENT 'Latest Minor Optimize time for all partitions',
    `last_full_optimizing_time`     timestamp NULL DEFAULT NULL COMMENT 'Latest Full Optimize time for all partitions',
    `optimizing_status`             varchar(20) DEFAULT 'Idle' COMMENT 'Table optimize status: MajorOptimizing, MinorOptimizing, Pending, Idle',
    `optimizing_status_start_time`  timestamp default CURRENT_TIMESTAMP COMMENT 'Table optimize status start time',
    `optimizing_process_id`         bigint(20) NOT NULL COMMENT 'optimizing_procedure UUID',
    `optimizer_group`               varchar(64) NOT NULL,
    `table_config`                  mediumtext,
    `optimizing_config`             mediumtext,
    PRIMARY KEY (`table_id`),
    UNIQUE KEY `table_index` (`catalog_name`,`db_name`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Optimize running information of each table';

CREATE TABLE `table_optimizing_process`
(
    `process_id`                    bigint(20) NOT NULL COMMENT 'optimizing_procedure UUID',
    `table_id`                      bigint(20) NOT NULL,
    `catalog_name`                  varchar(64) NOT NULL COMMENT 'Catalog name',
    `db_name`                       varchar(128) NOT NULL COMMENT 'Database name',
    `table_name`                    varchar(128) NOT NULL COMMENT 'Table name',
    `target_snapshot_id`            bigint(20) NOT NULL,
    `target_change_snapshot_id`     bigint(20) NOT NULL,
    `status`                        varchar(10) NOT NULL COMMENT 'Direct to TableOptimizingStatus',
    `optimizing_type`               varchar(10) NOT NULL COMMENT 'Optimize type: Major, Minor',
    `plan_time`                     timestamp DEFAULT CURRENT_TIMESTAMP COMMENT 'First plan time',
    `end_time`                      timestamp NULL DEFAULT NULL COMMENT 'finish time or failed time',
    `fail_reason`                   varchar(4096) DEFAULT NULL COMMENT 'Error message after task failed',
    `rewrite_input`                 longblob DEFAULT NULL COMMENT 'rewrite files input',
    `summary`                       mediumtext COMMENT 'Max change transaction id of these tasks',
    `from_sequence`                 mediumtext COMMENT 'from or min sequence of each partition',
    `to_sequence`                   mediumtext COMMENT 'to or max sequence of each partition',
    PRIMARY KEY (`process_id`),
    KEY  `table_index` (`table_id`, `plan_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'History of optimizing after each commit';

CREATE TABLE `task_runtime`
(
    `process_id`                bigint(20) NOT NULL,
    `task_id`                   int(11) NOT NULL,
    `retry_num`                 int(11) DEFAULT NULL COMMENT 'Retry times',
    `table_id`                  bigint(20) NOT NULL,
    `partition_data`            varchar(128)  DEFAULT NULL COMMENT 'Partition data',
    `create_time`               timestamp NULL DEFAULT NULL COMMENT 'Task create time',
    `start_time`                timestamp NULL DEFAULT NULL COMMENT 'Time when task start waiting to execute',
    `end_time`                  timestamp NULL DEFAULT NULL COMMENT 'Time when task finished',
    `cost_time`                 bigint(20) DEFAULT NULL,
    `status`                    varchar(16)   DEFAULT NULL  COMMENT 'Optimize Status: Init, Pending, Executing, Failed, Prepared, Committed',
    `fail_reason`               varchar(4096) DEFAULT NULL COMMENT 'Error message after task failed',
    `optimizer_token`           varchar(50) DEFAULT NULL COMMENT 'Job type',
    `thread_id`                 int(11) DEFAULT NULL COMMENT 'Job id',
    `rewrite_output`            longblob DEFAULT NULL COMMENT 'rewrite files output',
    `metrics_summary`           text COMMENT 'metrics summary',
    `properties`                mediumtext COMMENT 'task properties',
    PRIMARY KEY (`process_id`, `task_id`),
    KEY  `table_index` (`table_id`, `process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Optimize task basic information';

CREATE TABLE `optimizing_task_quota`
(
    `process_id`                bigint(20) NOT NULL COMMENT 'Optimize type: Major, Minor, FullMajor',
    `task_id`                   int(11) NOT NULL COMMENT 'Optimize task unique id',
    `retry_num`                 int(11) DEFAULT 0 COMMENT 'Retry times',
    `table_id`                  bigint(20) NOT NULL,
    `start_time`                timestamp default CURRENT_TIMESTAMP COMMENT 'Time when task start waiting to execute',
    `end_time`                  timestamp default CURRENT_TIMESTAMP COMMENT 'Time when task finished',
    `fail_reason`               varchar(4096) DEFAULT NULL COMMENT 'Error message after task failed',
    PRIMARY KEY (`process_id`, `task_id`, `retry_num`),
    KEY  `table_index` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Optimize task basic information';


CREATE TABLE `api_tokens`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `apikey`     varchar(256) NOT NULL COMMENT 'openapi client public key',
    `secret`     varchar(256) NOT NULL COMMENT 'The key used by the client to generate the request signature',
    `apply_time` timestamp NULL DEFAULT NULL COMMENT 'apply time',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `account_unique` (`apikey`) USING BTREE COMMENT 'account unique'
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Openapi  secret';

CREATE TABLE `platform_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'file id',
  `file_name` varchar(100) NOT NULL COMMENT 'file name',
  `file_content_b64` mediumtext NOT NULL COMMENT 'file content encoded with base64',
  `file_path` varchar(100) DEFAULT NULL COMMENT 'may be hdfs path , not be used now',
  `add_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'add timestamp',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='store files info saved in the platform';

CREATE TABLE `table_blocker` (
  `blocker_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Blocker unique id',
  `catalog_name` varchar(64) NOT NULL COMMENT 'Catalog name',
  `db_name` varchar(128) NOT NULL COMMENT 'Database name',
  `table_name` varchar(128) NOT NULL COMMENT 'Table name',
  `operations` varchar(128) NOT NULL COMMENT 'Blocked operations',
  `create_time` timestamp NULL DEFAULT NULL COMMENT 'Blocker create time',
  `expiration_time` timestamp NULL DEFAULT NULL COMMENT 'Blocker expiration time',
  `properties` mediumtext COMMENT 'Blocker properties',
  PRIMARY KEY (`blocker_id`),
  KEY `table_index` (`catalog_name`,`db_name`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Table blockers';

INSERT INTO catalog_metadata(catalog_name,catalog_metastore,storage_configs,auth_configs, catalog_properties) VALUES ('local_catalog','ams','{"storage.type":"hdfs","hive.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==","hadoop.core.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==","hadoop.hdfs.site":"PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg=="}','{"auth.type":"simple","auth.simple.hadoop_username":"root"}','{"warehouse":"/tmp/arctic/warehouse","table-formats":"MIXED_ICEBERG"}');
