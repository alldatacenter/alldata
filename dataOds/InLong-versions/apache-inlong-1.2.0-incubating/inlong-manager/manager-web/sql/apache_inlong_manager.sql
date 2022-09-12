/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- database for Manager Web
-- ----------------------------
CREATE DATABASE IF NOT EXISTS apache_inlong_manager;
USE apache_inlong_manager;

-- ----------------------------
-- Table structure for inlong_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_group`
(
    `id`                     int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`        varchar(256) NOT NULL COMMENT 'Inlong group id, filled in by the user, undeleted ones cannot be repeated',
    `name`                   varchar(128)      DEFAULT '' COMMENT 'Inlong group name, English, Chinese, numbers, etc',
    `description`            varchar(256)      DEFAULT '' COMMENT 'Inlong group Introduction',
    `mq_type`                varchar(20)       DEFAULT 'TUBE' COMMENT 'The message queue type, high throughput: TUBE, high consistency: PULSAR',
    `mq_resource`            varchar(128) NOT NULL COMMENT 'MQ resource, for Tube, its Topic, for Pulsar, its Namespace',
    `daily_records`          int(11)           DEFAULT '10' COMMENT 'Number of access records per day, unit: 10,000 records per day',
    `daily_storage`          int(11)           DEFAULT '10' COMMENT 'Access size by day, unit: GB per day',
    `peak_records`           int(11)           DEFAULT '1000' COMMENT 'Access peak per second, unit: records per second',
    `max_length`             int(11)           DEFAULT '10240' COMMENT 'The maximum length of a single piece of data, unit: Byte',
    `enable_zookeeper`       tinyint(2)        DEFAULT '0' COMMENT 'Whether to enable the zookeeper, 0-disable, 1-enable',
    `enable_create_resource` tinyint(2)        DEFAULT '1' COMMENT 'Whether to enable create resource? 0-disable, 1-enable',
    `lightweight`            tinyint(2)        DEFAULT '0' COMMENT 'Whether to use lightweight mode, 0-false, 1-true',
    `inlong_cluster_tag`     varchar(128)      DEFAULT NULL COMMENT 'The cluster tag, which links to inlong_cluster table',
    `ext_params`             text              DEFAULT NULL COMMENT 'Extended params, will be saved as JSON string, such as queue_module, partition_num',
    `in_charges`             varchar(512) NOT NULL COMMENT 'Name of responsible person, separated by commas',
    `followers`              varchar(512)      DEFAULT NULL COMMENT 'Name of followers, separated by commas',
    `status`                 int(4)            DEFAULT '100' COMMENT 'Inlong group status',
    `previous_status`        int(4)            DEFAULT '100' COMMENT 'Previous group status',
    `is_deleted`             int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`                varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`               varchar(64)       DEFAULT NULL COMMENT 'Modifier name',
    `create_time`            timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`            timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_inlong_group` (`inlong_group_id`, `is_deleted`),
    INDEX group_status_deleted_idx (`status`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong group table';

-- ----------------------------
-- Table structure for inlong_group_ext
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_group_ext`
(
    `id`              int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id` varchar(256) NOT NULL COMMENT 'Inlong group id',
    `key_name`        varchar(256) NOT NULL COMMENT 'Configuration item name',
    `key_value`       text              DEFAULT NULL COMMENT 'The value of the configuration item',
    `is_deleted`      int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `modify_time`     timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    KEY `index_group_id` (`inlong_group_id`),
    UNIQUE KEY `unique_group_key` (`inlong_group_id`, `key_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong group extension table';

-- ----------------------------
-- Table structure for inlong_cluster
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_cluster`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `name`        varchar(128) NOT NULL COMMENT 'Cluster name',
    `type`        varchar(20)       DEFAULT '' COMMENT 'Cluster type, such as: TUBE, PULSAR, DATA_PROXY, etc',
    `url`         varchar(512)      DEFAULT NULL COMMENT 'Cluster URL',
    `cluster_tag` varchar(128)      DEFAULT NULL COMMENT 'Cluster tag, the same tab indicates that cluster belongs to the same set',
    `ext_tag`     varchar(128)      DEFAULT NULL COMMENT 'Extension tag, for extended use',
    `token`       varchar(512)      DEFAULT NULL COMMENT 'Cluster token',
    `ext_params`  text              DEFAULT NULL COMMENT 'Extended params, will saved as JSON string',
    `heartbeat`   text              DEFAULT NULL COMMENT 'Cluster heartbeat info',
    `in_charges`  varchar(512) NOT NULL COMMENT 'Name of responsible person, separated by commas',
    `status`      int(4)            DEFAULT '0' COMMENT 'Cluster status',
    `is_deleted`  int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`     varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`    varchar(64)       DEFAULT NULL COMMENT 'Modifier name',
    `create_time` timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time` timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_cluster_index` (`name`, `type`, `cluster_tag`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong cluster table';

-- ----------------------------
-- Table structure for inlong_cluster_node
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_cluster_node`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `parent_id`   int(11)      NOT NULL COMMENT 'Id of the parent cluster',
    `type`        varchar(20)       DEFAULT '' COMMENT 'Cluster type, such as: DATA_PROXY, AGENT, etc',
    `ip`          varchar(512) NULL COMMENT 'Cluster IP, separated by commas, such as: 127.0.0.1:8080,host2:8081',
    `port`        int(6)       NULL COMMENT 'Cluster port',
    `ext_params`  text              DEFAULT NULL COMMENT 'Another fields will be saved as JSON string',
    `status`      int(4)            DEFAULT '0' COMMENT 'Cluster status',
    `is_deleted`  int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`     varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`    varchar(64)       DEFAULT NULL COMMENT 'Modifier name',
    `create_time` timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time` timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_cluster_node` (`parent_id`, `type`, `ip`, `port`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong cluster node table';

-- ----------------------------
-- Table structure for data_node
-- ----------------------------
CREATE TABLE IF NOT EXISTS `data_node`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `name`        varchar(128) NOT NULL COMMENT 'Node name',
    `type`        varchar(20)       DEFAULT '' COMMENT 'Node type, such as: MYSQL, HIVE, KAFKA, ES, etc',
    `url`         varchar(512)      DEFAULT NULL COMMENT 'Node URL',
    `username`    varchar(128)      DEFAULT NULL COMMENT 'Username for node',
    `token`       varchar(512)      DEFAULT NULL COMMENT 'Node token',
    `ext_params`  text              DEFAULT NULL COMMENT 'Extended params, will be saved as JSON string',
    `in_charges`  varchar(512) NOT NULL COMMENT 'Name of responsible person, separated by commas',
    `status`      int(4)            DEFAULT '0' COMMENT 'Node status',
    `is_deleted`  int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`     varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`    varchar(64)       DEFAULT NULL COMMENT 'Modifier name',
    `create_time` timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time` timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_data_node_index` (`name`, `type`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Data node table';

-- ----------------------------
-- Table structure for consumption
-- ----------------------------
CREATE TABLE IF NOT EXISTS `consumption`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `consumer_group`   varchar(256) NOT NULL COMMENT 'Consumer group',
    `in_charges`       varchar(512) NOT NULL COMMENT 'Person in charge of consumption',
    `inlong_group_id`  varchar(256) NOT NULL COMMENT 'Inlong group id',
    `mq_type`          varchar(10)       DEFAULT 'TUBE' COMMENT 'Message queue, high throughput: TUBE, high consistency: PULSAR',
    `topic`            varchar(256) NOT NULL COMMENT 'Consumption topic',
    `filter_enabled`   int(2)            DEFAULT '0' COMMENT 'Whether to filter, default 0, not filter consume',
    `inlong_stream_id` varchar(256)      DEFAULT NULL COMMENT 'Inlong stream ID for consumption, if filter_enable is 1, it cannot empty',
    `status`           int(4)       NOT NULL COMMENT 'Status: draft, pending approval, approval rejected, approval passed',
    `is_deleted`       int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`          varchar(64)  NOT NULL COMMENT 'creator',
    `modifier`         varchar(64)       DEFAULT NULL COMMENT 'modifier',
    `create_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Data consumption configuration table';

-- ----------------------------
-- Table structure for consumption_pulsar
-- ----------------------------
CREATE TABLE IF NOT EXISTS `consumption_pulsar`
(
    `id`                 int(11)      NOT NULL AUTO_INCREMENT,
    `consumption_id`     int(11)      DEFAULT NULL COMMENT 'ID of the consumption information to which it belongs, guaranteed to be uniquely associated with consumption information',
    `consumer_group`     varchar(256) NOT NULL COMMENT 'Consumer group',
    `inlong_group_id`    varchar(256) NOT NULL COMMENT 'Inlong group ID',
    `is_rlq`             tinyint(1)   DEFAULT '0' COMMENT 'Whether to configure the retry letter topic, 0: no configuration, 1: configuration',
    `retry_letter_topic` varchar(256) DEFAULT NULL COMMENT 'The name of the retry queue topic',
    `is_dlq`             tinyint(1)   DEFAULT '0' COMMENT 'Whether to configure dead letter topic, 0: no configuration, 1: means configuration',
    `dead_letter_topic`  varchar(256) DEFAULT NULL COMMENT 'dead letter topic name',
    `is_deleted`         int(11)      DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Pulsar consumption table';

-- ----------------------------
-- Table structure for data_schema
-- ----------------------------
CREATE TABLE IF NOT EXISTS `data_schema`
(
    `id`                 int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `name`               varchar(128) NOT NULL COMMENT 'Data format name, globally unique',
    `agent_type`         varchar(20)  NOT NULL COMMENT 'Agent type: file, db_incr, db_full',
    `data_generate_rule` varchar(32)  NOT NULL COMMENT 'Data file generation rules, including day and hour',
    `sort_type`          int(11)      NOT NULL COMMENT 'sort logic rules, 0, 5, 9, 10, 13, 15',
    `time_offset`        varchar(10)  NOT NULL COMMENT 'time offset',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_schema_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Data format table';

-- create default data schema
INSERT INTO `data_schema` (name, agent_type, data_generate_rule, sort_type, time_offset)
values ('m0_day', 'file_agent', 'day', 0, '-0d');

-- ----------------------------
-- Table structure for stream_source_cmd_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_source_cmd_config`
(
    `id`                  int(11)     NOT NULL AUTO_INCREMENT COMMENT 'cmd id',
    `cmd_type`            int(11)     NOT NULL,
    `task_id`             int(11)     NOT NULL,
    `specified_data_time` varchar(64) NOT NULL,
    `bSend`               tinyint(1)  NOT NULL,
    `create_time`         timestamp   NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`         timestamp   NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `result_info`         varchar(64)      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_1` (`task_id`, `bSend`, `specified_data_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

-- ----------------------------
-- Table structure for inlong_stream
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_stream`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`  varchar(256) NOT NULL COMMENT 'Owning inlong group id',
    `inlong_stream_id` varchar(256) NOT NULL COMMENT 'Inlong stream id, non-deleted globally unique',
    `name`             varchar(64)       DEFAULT NULL COMMENT 'The name of the inlong stream page display, can be Chinese',
    `description`      varchar(256)      DEFAULT '' COMMENT 'Introduction to inlong stream',
    `mq_resource`      varchar(128)      DEFAULT NULL COMMENT 'MQ resource, in one stream, corresponding to the filter ID of Tube, corresponding to the topic of Pulsar',
    `data_type`        varchar(20)       DEFAULT NULL COMMENT 'Data type, including: CSV, KEY-VALUE, JSON, AVRO, etc.',
    `data_encoding`    varchar(8)        DEFAULT 'UTF-8' COMMENT 'Data encoding format, including: UTF-8, GBK, etc.',
    `data_separator`   varchar(8)        DEFAULT NULL COMMENT 'The source data field separator, stored as ASCII code',
    `data_escape_char` varchar(8)        DEFAULT NULL COMMENT 'Source data field escape character, the default is NULL (NULL), stored as 1 character',
    `sync_send`        tinyint(1)        DEFAULT '0' COMMENT 'order_preserving 0: none, 1: yes',
    `daily_records`    int(11)           DEFAULT '10' COMMENT 'Number of access records per day, unit: 10,000 records per day',
    `daily_storage`    int(11)           DEFAULT '10' COMMENT 'Access size by day, unit: GB per day',
    `peak_records`     int(11)           DEFAULT '1000' COMMENT 'Access peak per second, unit: records per second',
    `max_length`       int(11)           DEFAULT '10240' COMMENT 'The maximum length of a single piece of data, unit: Byte',
    `storage_period`   int(11)           DEFAULT '1' COMMENT 'The storage period of data in MQ, unit: day',
    `ext_params`       text              DEFAULT NULL COMMENT 'Extended params, will be saved as JSON string',
    `status`           int(4)            DEFAULT '100' COMMENT 'Inlong stream status',
    `previous_status`  int(4)            DEFAULT '100' COMMENT 'Previous status',
    `is_deleted`       int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`          varchar(64)       DEFAULT NULL COMMENT 'Creator name',
    `modifier`         varchar(64)       DEFAULT NULL COMMENT 'Modifier name',
    `create_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_inlong_stream` (`inlong_stream_id`, `inlong_group_id`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong stream table';

-- ----------------------------
-- Table structure for inlong_stream_ext
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_stream_ext`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`  varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id` varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `key_name`         varchar(256) NOT NULL COMMENT 'Configuration item name',
    `key_value`        text COMMENT 'The value of the configuration item',
    `is_deleted`       int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `modify_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `stream_key_idx` (`inlong_group_id`, `inlong_stream_id`, `key_name`),
    KEY `index_stream_id` (`inlong_group_id`, `inlong_stream_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Inlong stream extension table';

-- ----------------------------
-- Table structure for inlong_stream_field
-- ----------------------------
CREATE TABLE IF NOT EXISTS `inlong_stream_field`
(
    `id`                  int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`     varchar(256) NOT NULL COMMENT 'Owning inlong group id',
    `inlong_stream_id`    varchar(256) NOT NULL COMMENT 'Owning inlong stream id',
    `is_predefined_field` tinyint(1)   DEFAULT '0' COMMENT 'Whether it is a predefined field, 0: no, 1: yes',
    `field_name`          varchar(20)  NOT NULL COMMENT 'field name',
    `field_value`         varchar(128) DEFAULT NULL COMMENT 'Field value, required if it is a predefined field',
    `pre_expression`      varchar(256) DEFAULT NULL COMMENT 'Pre-defined field value expression',
    `field_type`          varchar(20)  NOT NULL COMMENT 'field type',
    `field_comment`       varchar(50)  DEFAULT NULL COMMENT 'Field description',
    `is_meta_field`       smallint(3)  DEFAULT '0' COMMENT 'Is this field a meta field? 0: no, 1: yes',
    `meta_field_name`     varchar(20)  DEFAULT NULL COMMENT 'Meta field name',
    `field_format`        varchar(50)  DEFAULT NULL COMMENT 'Field format, including: MICROSECONDS, MILLISECONDS, SECONDS, SQL, ISO_8601 and custom such as yyyy-MM-dd HH:mm:ss',
    `rank_num`            smallint(6)  DEFAULT '0' COMMENT 'Field order (front-end display field order)',
    `is_deleted`          int(11)      DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    PRIMARY KEY (`id`),
    KEY `index_field_stream_id` (`inlong_stream_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='File/DB data source field table';

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `operation_log`
(
    `id`                  int(11)   NOT NULL AUTO_INCREMENT,
    `authentication_type` varchar(64)        DEFAULT NULL COMMENT 'Authentication type',
    `operation_type`      varchar(256)       DEFAULT NULL COMMENT 'operation type',
    `http_method`         varchar(64)        DEFAULT NULL COMMENT 'Request method',
    `invoke_method`       varchar(256)       DEFAULT NULL COMMENT 'invoke method',
    `operator`            varchar(256)       DEFAULT NULL COMMENT 'operator',
    `proxy`               varchar(256)       DEFAULT NULL COMMENT 'proxy',
    `request_url`         varchar(256)       DEFAULT NULL COMMENT 'Request URL',
    `remote_address`      varchar(256)       DEFAULT NULL COMMENT 'Request IP',
    `cost_time`           bigint(20)         DEFAULT NULL COMMENT 'time-consuming',
    `body`                text COMMENT 'Request body',
    `param`               text COMMENT 'parameter',
    `status`              int(4)             DEFAULT NULL COMMENT 'status',
    `request_time`        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'request time',
    `err_msg`             text COMMENT 'Error message',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ----------------------------
-- Table structure for role
-- ----------------------------
CREATE TABLE IF NOT EXISTS `role`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `role_code`   varchar(100) NOT NULL COMMENT 'Role code',
    `role_name`   varchar(256) NOT NULL COMMENT 'Role Chinese name',
    `create_time` datetime     NOT NULL,
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`   varchar(256) NOT NULL,
    `update_by`   varchar(256) NOT NULL,
    `disabled`    tinyint(1)   NOT NULL DEFAULT '0' COMMENT 'Is it disabled?',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_role_code_idx` (`role_code`),
    UNIQUE KEY `unique_role_name_idx` (`role_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Role Table';

-- ----------------------------
-- Table structure for source_file_basic
-- ----------------------------
CREATE TABLE IF NOT EXISTS `source_file_basic`
(
    `id`                int(11)      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `inlong_group_id`   varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id`  varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `is_hybrid_source`  tinyint(1)        DEFAULT '0' COMMENT 'Whether to mix data sources',
    `is_table_mapping`  tinyint(1)        DEFAULT '0' COMMENT 'Is there a table name mapping',
    `date_offset`       int(4)            DEFAULT '0' COMMENT 'Time offset\n',
    `date_offset_unit`  varchar(2)        DEFAULT 'H' COMMENT 'Time offset unit',
    `file_rolling_type` varchar(2)        DEFAULT 'H' COMMENT 'File rolling type',
    `upload_max_size`   int(4)            DEFAULT '120' COMMENT 'Upload maximum size',
    `need_compress`     tinyint(1)        DEFAULT '0' COMMENT 'Whether need compress',
    `is_deleted`        int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`           varchar(64)  NOT NULL COMMENT 'Creator',
    `modifier`          varchar(64)       DEFAULT NULL COMMENT 'Modifier',
    `create_time`       timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`       timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `temp_view`         text              DEFAULT NULL COMMENT 'temp view',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='basic configuration of file data source';

-- ----------------------------
-- Table structure for source_file_detail
-- ----------------------------
CREATE TABLE IF NOT EXISTS `source_file_detail`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`  varchar(256) NOT NULL COMMENT 'Owning inlong group id',
    `inlong_stream_id` varchar(256) NOT NULL COMMENT 'Owning inlong stream id',
    `access_type`      varchar(20)       DEFAULT 'Agent' COMMENT 'Collection type, there are Agent, DataProxy client, LoadProxy, the file can only be Agent temporarily',
    `server_name`      varchar(64)       DEFAULT NULL COMMENT 'The name of the data source service. If it is empty, add configuration through the following fields',
    `ip`               varchar(128) NOT NULL COMMENT 'Data source IP address',
    `port`             int(11)      NOT NULL COMMENT 'Data source port number',
    `is_inner_ip`      tinyint(1)        DEFAULT '0' COMMENT 'Whether it is intranet, 0: no, 1: yes',
    `issue_type`       varchar(10)       DEFAULT 'SSH' COMMENT 'Issuing method, there are SSH, TCS',
    `username`         varchar(32)       DEFAULT NULL COMMENT 'User name of the data source IP host',
    `password`         varchar(64)       DEFAULT NULL COMMENT 'The password corresponding to the above user name',
    `file_path`        varchar(256) NOT NULL COMMENT 'File path, supports regular matching',
    `status`           int(4)            DEFAULT '0' COMMENT 'Data source status',
    `previous_status`  int(4)            DEFAULT '0' COMMENT 'Previous status',
    `is_deleted`       int(11)           DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`          varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`         varchar(64)       DEFAULT NULL COMMENT 'Modifier name',
    `create_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `temp_view`        text              DEFAULT NULL COMMENT 'Temporary view, used to save un-submitted and unapproved intermediate data after modification',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Detailed table of file data source';

-- ----------------------------
-- Table structure for stream_source
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_source`
(
    `id`                 int(11)      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `inlong_group_id`    varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id`   varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `source_name`        varchar(128) NOT NULL DEFAULT '' COMMENT 'source_name',
    `source_type`        varchar(20)           DEFAULT '0' COMMENT 'Source type, including: FILE, DB, etc',
    `agent_ip`           varchar(40)           DEFAULT NULL COMMENT 'Ip of the agent running the task',
    `uuid`               varchar(30)           DEFAULT NULL COMMENT 'Mac uuid of the agent running the task',
    `data_node_name`     varchar(128)          DEFAULT NULL COMMENT 'Node name, which links to data_node table',
    `cluster_id`         int(11)               DEFAULT NULL COMMENT 'Id of the cluster that collected this source',
    `serialization_type` varchar(20)           DEFAULT NULL COMMENT 'Serialization type, support: csv, json, canal, avro, etc',
    `snapshot`           text                  DEFAULT NULL COMMENT 'Snapshot of this source task',
    `report_time`        timestamp    NULL COMMENT 'Snapshot time',
    `ext_params`         text                  DEFAULT NULL COMMENT 'Another fields will be saved as JSON string, such as filePath, dbName, tableName, etc',
    `version`            int(11)               DEFAULT '1' COMMENT 'Stream source version',
    `status`             int(4)                DEFAULT '110' COMMENT 'Data source status',
    `previous_status`    int(4)                DEFAULT '110' COMMENT 'Previous status',
    `is_deleted`         int(11)               DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`            varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`           varchar(64)           DEFAULT NULL COMMENT 'Modifier name',
    `create_time`        timestamp    NULL     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`        timestamp    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_source_name` (`inlong_group_id`, `inlong_stream_id`, `source_name`, `is_deleted`),
    KEY `source_status_idx` (`status`, `is_deleted`),
    KEY `source_agent_ip_idx` (`agent_ip`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream source table';

-- ----------------------------
-- Table structure for stream_transform
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_transform`
(
    `id`                   int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`      varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id`     varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `transform_name`       varchar(128) NOT NULL COMMENT 'Transform name, unique in one stream',
    `transform_type`       varchar(20)  NOT NULL COMMENT 'Transform type, including: splitter, filter, joiner, etc.',
    `pre_node_names`       text         NOT NULL COMMENT 'Pre node names of transform in this stream',
    `post_node_names`      text COMMENT 'Post node names of transform in this stream',
    `transform_definition` text         NOT NULL COMMENT 'Transform definition in json type',
    `version`              int(11)      NOT NULL DEFAULT '1' COMMENT 'Stream transform version',
    `is_deleted`           int(11)      NOT NULL DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`              varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`             varchar(64)           DEFAULT '' COMMENT 'Modifier name',
    `create_time`          timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`          timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_transform_name` (`inlong_group_id`, `inlong_stream_id`, `transform_name`, `is_deleted`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream transform table';

-- ----------------------------
-- Table structure for stream_sink
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_sink`
(
    `id`                     int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`        varchar(256) NOT NULL COMMENT 'Owning inlong group id',
    `inlong_stream_id`       varchar(256) NOT NULL COMMENT 'Owning inlong stream id',
    `sink_type`              varchar(15)           DEFAULT 'HIVE' COMMENT 'Sink type, including: HIVE, ES, etc',
    `sink_name`              varchar(128) NOT NULL DEFAULT '' COMMENT 'Sink name',
    `description`            varchar(500) NULL COMMENT 'Sink description',
    `enable_create_resource` tinyint(2)            DEFAULT '1' COMMENT 'Whether to enable create sink resource? 0-disable, 1-enable',
    `inlong_cluster_name`    varchar(128)          DEFAULT NULL COMMENT 'Cluster name, which links to inlong_cluster table',
    `data_node_name`         varchar(128)          DEFAULT NULL COMMENT 'Node name, which links to data_node table',
    `sort_task_name`         varchar(512)          DEFAULT NULL COMMENT 'Sort task name or task ID',
    `sort_consumer_group`    varchar(512)          DEFAULT NULL COMMENT 'Consumer group name for Sort task',
    `ext_params`             text         NULL COMMENT 'Another fields, will be saved as JSON type',
    `operate_log`            text                  DEFAULT NULL COMMENT 'Background operate log',
    `status`                 int(11)               DEFAULT '100' COMMENT 'Status',
    `previous_status`        int(11)               DEFAULT '100' COMMENT 'Previous status',
    `is_deleted`             int(11)               DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `creator`                varchar(64)  NOT NULL COMMENT 'Creator name',
    `modifier`               varchar(64)           DEFAULT NULL COMMENT 'Modifier name',
    `create_time`            timestamp    NULL     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`            timestamp    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_sink_name` (`inlong_group_id`, `inlong_stream_id`, `sink_name`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream sink table';

-- ----------------------------
-- Table structure for stream_sink_ext
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_sink_ext`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `sink_type`   varchar(20)  NOT NULL COMMENT 'Sink type, including: HDFS, HIVE, etc.',
    `sink_id`     int(11)      NOT NULL COMMENT 'Sink id',
    `key_name`    varchar(256) NOT NULL COMMENT 'Configuration item name',
    `key_value`   text                  DEFAULT NULL COMMENT 'The value of the configuration item',
    `is_deleted`  int(11)               DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `modify_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    KEY `index_sink_id` (`sink_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream sink extension table';

-- ----------------------------
-- Table structure for stream_source_field
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_source_field`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`  varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id` varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `source_id`        int(11)      NOT NULL COMMENT 'Sink id',
    `source_type`      varchar(15)  NOT NULL COMMENT 'Sink type',
    `field_name`       varchar(20)  NOT NULL COMMENT 'field name',
    `field_value`      varchar(128) DEFAULT NULL COMMENT 'Field value, required if it is a predefined field',
    `pre_expression`   varchar(256) DEFAULT NULL COMMENT 'Pre-defined field value expression',
    `field_type`       varchar(20)  NOT NULL COMMENT 'field type',
    `field_comment`    varchar(50)  DEFAULT NULL COMMENT 'Field description',
    `is_meta_field`    smallint(3)  DEFAULT '0' COMMENT 'Is this field a meta field? 0: no, 1: yes',
    `meta_field_name`  varchar(20)  DEFAULT NULL COMMENT 'Meta field name',
    `field_format`     varchar(50)  DEFAULT NULL COMMENT 'Field format, including: MICROSECONDS, MILLISECONDS, SECONDS, SQL, ISO_8601 and custom such as yyyy-MM-dd HH:mm:ss',
    `rank_num`         smallint(6)  DEFAULT '0' COMMENT 'Field order (front-end display field order)',
    `is_deleted`       int(11)      DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    PRIMARY KEY (`id`),
    KEY `index_source_id` (`source_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream source field table';

-- ----------------------------
-- Table structure for stream_transform_field
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_transform_field`
(
    `id`                int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`   varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id`  varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `transform_id`      int(11)      NOT NULL COMMENT 'Transform id',
    `transform_type`    varchar(15)  NOT NULL COMMENT 'Transform type',
    `field_name`        varchar(50)  NOT NULL COMMENT 'Field name',
    `field_value`       varchar(128)  DEFAULT NULL COMMENT 'Field value, required if it is a predefined field',
    `pre_expression`    varchar(256)  DEFAULT NULL COMMENT 'Pre-defined field value expression',
    `field_type`        varchar(50)  NOT NULL COMMENT 'Field type',
    `field_comment`     varchar(2000) DEFAULT NULL COMMENT 'Field description',
    `is_meta_field`     smallint(3)   DEFAULT '0' COMMENT 'Is this field a meta field? 0: no, 1: yes',
    `meta_field_name`   varchar(20)   DEFAULT NULL COMMENT 'Meta field name',
    `field_format`      varchar(50)   DEFAULT NULL COMMENT 'Field format, including: MICROSECONDS, MILLISECONDS, SECONDS, SQL, ISO_8601 and custom such as yyyy-MM-dd HH:mm:ss',
    `rank_num`          smallint(6)   DEFAULT '0' COMMENT 'Field order (front-end display field order)',
    `is_deleted`        int(11)       DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    `origin_node_name`  varchar(256)  DEFAULT '' COMMENT 'Origin node name which stream field belongs',
    -- The source node name of the transport field
    `origin_field_name` varchar(50)   DEFAULT '' COMMENT 'Origin field name before transform operation',
    PRIMARY KEY (`id`),
    KEY `index_transform_id` (`transform_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream transform field table';

-- ----------------------------
-- Table structure for stream_sink_field
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_sink_field`
(
    `id`                int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `inlong_group_id`   varchar(256) NOT NULL COMMENT 'Inlong group id',
    `inlong_stream_id`  varchar(256) NOT NULL COMMENT 'Inlong stream id',
    `sink_id`           int(11)      NOT NULL COMMENT 'Sink id',
    `sink_type`         varchar(15)  NOT NULL COMMENT 'Sink type',
    `source_field_name` varchar(50)   DEFAULT NULL COMMENT 'Source field name',
    `source_field_type` varchar(50)   DEFAULT NULL COMMENT 'Source field type',
    `field_name`        varchar(50)  NOT NULL COMMENT 'Field name',
    `field_type`        varchar(50)  NOT NULL COMMENT 'Field type',
    `field_comment`     varchar(2000) DEFAULT NULL COMMENT 'Field description',
    `ext_params`        text COMMENT 'Field ext params',
    `is_meta_field`     smallint(3)   DEFAULT '0' COMMENT 'Is this field a meta field? 0: no, 1: yes',
    `meta_field_name`   varchar(20)   DEFAULT NULL COMMENT 'Meta field name',
    `field_format`      varchar(50)   DEFAULT NULL COMMENT 'Field format, including: MICROSECONDS, MILLISECONDS, SECONDS, SQL, ISO_8601 and custom such as yyyy-MM-dd HH:mm:ss',
    `rank_num`          smallint(6)   DEFAULT '0' COMMENT 'Field order (front-end display field order)',
    `is_deleted`        int(11)       DEFAULT '0' COMMENT 'Whether to delete, 0: not deleted, > 0: deleted',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Stream sink field table';

-- ----------------------------
-- Table structure for user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT,
    `name`         varchar(256) NOT NULL COMMENT 'account name',
    `password`     varchar(64)  NOT NULL COMMENT 'password md5',
    `account_type` int(11)      NOT NULL DEFAULT '1' COMMENT 'account type, 0-manager 1-normal',
    `due_date`     datetime              DEFAULT NULL COMMENT 'due date for account',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time`  datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `create_by`    varchar(256) NOT NULL COMMENT 'create by sb.',
    `update_by`    varchar(256)          DEFAULT NULL COMMENT 'update by sb.',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_name_idx` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='User table';

-- create default admin user, username is 'admin', password is 'inlong'
INSERT INTO `user` (name, password, account_type, due_date, create_time, update_time, create_by, update_by)
VALUES ('admin', '628ed559bff5ae36bd2184d4216973cf', 0, '2099-12-31 23:59:59',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'inlong_init', 'inlong_init');

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_role`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `user_name`   varchar(256) NOT NULL COMMENT 'username rtx',
    `role_code`   varchar(256) NOT NULL COMMENT 'role',
    `create_time` datetime     NOT NULL,
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_by`   varchar(256) NOT NULL,
    `update_by`   varchar(256) NOT NULL,
    `disabled`    tinyint(1)   NOT NULL DEFAULT '0' COMMENT 'Is it disabled?',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='User Role Table';

-- ----------------------------
-- Table structure for workflow_approver
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_approver`
(
    `id`                int(11)       NOT NULL AUTO_INCREMENT,
    `process_name`      varchar(256)  NOT NULL COMMENT 'Process name',
    `task_name`         varchar(256)  NOT NULL COMMENT 'Approval task name',
    `filter_key`        varchar(64)   NOT NULL COMMENT 'Filter condition KEY',
    `filter_value`      varchar(256)       DEFAULT NULL COMMENT 'Filter matching value',
    `filter_value_desc` varchar(256)       DEFAULT NULL COMMENT 'Filter value description',
    `approvers`         varchar(1024) NOT NULL COMMENT 'Approvers, separated by commas',
    `creator`           varchar(64)   NOT NULL COMMENT 'Creator',
    `modifier`          varchar(64)   NULL COMMENT 'Modifier',
    `create_time`       timestamp     NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`       timestamp     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    `is_deleted`        int(11)            DEFAULT '0' COMMENT 'Whether to delete, 0 is not deleted, if greater than 0, delete',
    PRIMARY KEY (`id`),
    KEY `process_name_task_name_index` (`process_name`, `task_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow approver table';

-- create default approver for new consumption and new inlong group
INSERT INTO `workflow_approver`(`process_name`, `task_name`, `filter_key`, `filter_value`, `approvers`,
                                `creator`, `modifier`, `create_time`, `modify_time`, `is_deleted`)
VALUES ('NEW_CONSUMPTION_PROCESS', 'ut_admin', 'DEFAULT', NULL, 'admin',
        'inlong_init', 'inlong_init', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
       ('NEW_GROUP_PROCESS', 'ut_admin', 'DEFAULT', NULL, 'admin',
        'inlong_init', 'inlong_init', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ----------------------------
-- Table structure for workflow_event_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_event_log`
(
    `id`                   int(11)      NOT NULL AUTO_INCREMENT,
    `process_id`           int(11)      NOT NULL,
    `process_name`         varchar(256)  DEFAULT NULL COMMENT 'Process name',
    `process_display_name` varchar(256) NOT NULL COMMENT 'Process name',
    `inlong_group_id`      varchar(256)  DEFAULT NULL COMMENT 'Inlong group id',
    `task_id`              int(11)       DEFAULT NULL COMMENT 'Task ID',
    `element_name`         varchar(256) NOT NULL COMMENT 'Name of the component that triggered the event',
    `element_display_name` varchar(256) NOT NULL COMMENT 'Display name of the component that triggered the event',
    `event_type`           varchar(64)  NOT NULL COMMENT 'Event type: process / task ',
    `event`                varchar(64)  NOT NULL COMMENT 'Event name',
    `listener`             varchar(1024) DEFAULT NULL COMMENT 'Event listener name',
    `status`               int(11)      NOT NULL COMMENT 'Status',
    `async`                tinyint(1)   NOT NULL COMMENT 'Asynchronous or not',
    `ip`                   varchar(64)   DEFAULT NULL COMMENT 'IP address executed by listener',
    `start_time`           datetime     NOT NULL COMMENT 'Monitor start execution time',
    `end_time`             datetime      DEFAULT NULL COMMENT 'Listener end time',
    `remark`               text COMMENT 'Execution result remark information',
    `exception`            text COMMENT 'Exception information',
    PRIMARY KEY (`id`),
    INDEX group_status_idx (`inlong_group_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow event log table';

-- ----------------------------
-- Table structure for workflow_process
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_process`
(
    `id`              int(11)      NOT NULL AUTO_INCREMENT,
    `name`            varchar(256) NOT NULL COMMENT 'process name',
    `display_name`    varchar(256) NOT NULL COMMENT 'WorkflowProcess display name',
    `type`            varchar(256)          DEFAULT NULL COMMENT 'WorkflowProcess classification',
    `title`           varchar(256)          DEFAULT NULL COMMENT 'WorkflowProcess title',
    `inlong_group_id` varchar(256)          DEFAULT NULL COMMENT 'Inlong group id: to facilitate related inlong group',
    `applicant`       varchar(256) NOT NULL COMMENT 'applicant',
    `status`          varchar(64)  NOT NULL COMMENT 'status',
    `form_data`       text COMMENT 'form information',
    `start_time`      datetime     NOT NULL COMMENT 'start time',
    `end_time`        datetime              DEFAULT NULL COMMENT 'End event',
    `ext_params`      text COMMENT 'Extended information-json',
    `hidden`          tinyint(1)   NOT NULL DEFAULT '0' COMMENT 'Whether to hidden, 0: not hidden, 1: hidden',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow process table';

-- ----------------------------
-- Table structure for workflow_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `workflow_task`
(
    `id`                   int(11)       NOT NULL AUTO_INCREMENT,
    `type`                 varchar(64)   NOT NULL COMMENT 'Task type: UserTask / ServiceTask',
    `process_id`           int(11)       NOT NULL COMMENT 'Process ID',
    `process_name`         varchar(256)  NOT NULL COMMENT 'Process name',
    `process_display_name` varchar(256)  NOT NULL COMMENT 'Process name',
    `name`                 varchar(256)  NOT NULL COMMENT 'Task name',
    `display_name`         varchar(256)  NOT NULL COMMENT 'Task display name',
    `applicant`            varchar(64)   DEFAULT NULL COMMENT 'Applicant',
    `approvers`            varchar(1024) NOT NULL COMMENT 'Approvers',
    `status`               varchar(64)   NOT NULL COMMENT 'Status',
    `operator`             varchar(256)  DEFAULT NULL COMMENT 'Actual operator',
    `remark`               varchar(1024) DEFAULT NULL COMMENT 'Remark information',
    `form_data`            text COMMENT 'Form information submitted by the current task',
    `start_time`           datetime      NOT NULL COMMENT 'Start time',
    `end_time`             datetime      DEFAULT NULL COMMENT 'End time',
    `ext_params`           text COMMENT 'Extended information-json',
    PRIMARY KEY (`id`),
    INDEX process_status_idx (`process_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Workflow task table';

-- ----------------------------
-- Table structure for db_collector_detail_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `db_collector_detail_task`
(
    `id`            int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `main_id`       varchar(128) NOT NULL COMMENT 'main task id',
    `type`          int(11)      NOT NULL COMMENT 'task type',
    `time_var`      varchar(64)  NOT NULL COMMENT 'time variable',
    `db_type`       int(11)      NOT NULL COMMENT 'db type',
    `ip`            varchar(64)  NOT NULL COMMENT 'db ip',
    `port`          int(11)      NOT NULL COMMENT 'db port',
    `db_name`       varchar(64)  NULL COMMENT 'db name',
    `user`          varchar(64)  NULL COMMENT 'user name',
    `password`      varchar(64)  NULL COMMENT 'password',
    `sql_statement` varchar(256) NULL COMMENT 'sql statement',
    `offset`        int(11)      NOT NULL COMMENT 'offset for the data source',
    `total_limit`   int(11)      NOT NULL COMMENT 'total limit in a task',
    `once_limit`    int(11)      NOT NULL COMMENT 'limit for one query',
    `time_limit`    int(11)      NOT NULL COMMENT 'time limit for task',
    `retry_times`   int(11)      NOT NULL COMMENT 'max retry times if task failes',
    `group_id`      varchar(64)  NULL COMMENT 'group id',
    `stream_id`     varchar(64)  NULL COMMENT 'stream id',
    `state`         int(11)      NOT NULL COMMENT 'task state',
    `create_time`   timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `modify_time`   timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modify time',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='db collector detail task table';

-- ----------------------------
-- Table structure for sort_cluster_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sort_cluster_config`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `cluster_name` varchar(128) NOT NULL COMMENT 'Cluster name',
    `task_name`    varchar(128) NOT NULL COMMENT 'Task name',
    `sink_type`    varchar(128) NOT NULL COMMENT 'Type of sink',
    PRIMARY KEY (`id`),
    KEY `index_sort_cluster_config` (`cluster_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Sort cluster config table';

-- ----------------------------
-- Table structure for sort_task_id_param
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sort_task_id_param`
(
    `id`          int(11)       NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `task_name`   varchar(128)  NOT NULL COMMENT 'Task name',
    `group_id`    varchar(128)  NOT NULL COMMENT 'Inlong group id',
    `stream_id`   varchar(128)  NULL COMMENT 'Inlong stream id',
    `param_key`   varchar(128)  NOT NULL COMMENT 'Key of param',
    `param_value` varchar(1024) NOT NULL COMMENT 'Value of param',
    PRIMARY KEY (`id`),
    KEY `index_sort_task_id_param` (`task_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Sort task id params table';

-- ----------------------------
-- Table structure for sort_task_sink_param
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sort_task_sink_param`
(
    `id`          int(11)       NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `task_name`   varchar(128)  NOT NULL COMMENT 'Task name',
    `sink_type`   varchar(128)  NOT NULL COMMENT 'Type of sink',
    `param_key`   varchar(128)  NOT NULL COMMENT 'Key of param',
    `param_value` varchar(1024) NOT NULL COMMENT 'Value of param',
    PRIMARY KEY (`id`),
    KEY `index_sort_task_sink_params` (`task_name`, `sink_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Sort task sink params table';

-- ----------------------------
-- Table structure for sort_source_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sort_source_config`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `cluster_name` varchar(128) NOT NULL COMMENT 'Cluster name',
    `task_name`    varchar(128) NOT NULL COMMENT 'Task name',
    `zone_name`    varchar(128) NOT NULL COMMENT 'Cache zone name',
    `topic`        varchar(128) DEFAULT '' COMMENT 'Topic',
    `ext_params`   text         DEFAULT NULL COMMENT 'Another fields, will be saved as JSON type',
    PRIMARY KEY (`id`),
    KEY `index_sort_source_config` (`cluster_name`, `task_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Sort source config table';

-- ----------------------------
-- Table structure for config log report
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_config_log`
(
    `ip`               varchar(24)  NOT NULL COMMENT 'client host ip',
    `version`          varchar(64)           DEFAULT NULL COMMENT 'client version',
    `inlong_stream_id` varchar(256) NOT NULL DEFAULT '' COMMENT 'Inlong stream ID for consumption',
    `inlong_group_id`  varchar(256) NOT NULL DEFAULT '' COMMENT 'Inlong group id',
    `component_name`   varchar(64)  NOT NULL DEFAULT '' COMMENT 'current report info component name',
    `config_name`      varchar(64)  NOT NULL DEFAULT '' COMMENT 'massage in heartbeat request',
    `log_type`         int(1)                DEFAULT 0 COMMENT '0 normal, 1 error',
    `log_info`         text                  DEFAULT NULL COMMENT 'massage in heartbeat request',
    `report_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'report time',
    `modify_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`ip`, `config_name`, `component_name`, `log_type`, `inlong_stream_id`, `inlong_group_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='stream config log report information table';

-- ----------------------------
-- Table structure for inlong component heartbeat
-- ----------------------------
CREATE TABLE IF NOT EXISTS `component_heartbeat`
(
    `id`               int(11)     NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `component`        varchar(64) NOT NULL DEFAULT '' COMMENT 'Component name, such as: Agent, Sort...',
    `instance`         varchar(64) NOT NULL DEFAULT '' COMMENT 'Component instance, can be ip, name...',
    `status_heartbeat` text        NOT NULL COMMENT 'Status heartbeat info',
    `metric_heartbeat` text        NOT NULL COMMENT 'Metric heartbeat info',
    `report_time`      bigint(20)  NOT NULL COMMENT 'Report time',
    `create_time`      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_component_heartbeat` (`component`, `instance`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='Inlong component heartbeat';

-- ----------------------------
-- Table structure for inlong group heartbeat
-- ----------------------------
CREATE TABLE IF NOT EXISTS `group_heartbeat`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `component`        varchar(64)  NOT NULL DEFAULT '' COMMENT 'Component name, such as: Agent, Sort...',
    `instance`         varchar(64)  NOT NULL DEFAULT '' COMMENT 'Component instance, can be ip, name...',
    `inlong_group_id`  varchar(256) NOT NULL DEFAULT '' COMMENT 'Owning inlong group id',
    `status_heartbeat` text         NOT NULL COMMENT 'Status heartbeat info',
    `metric_heartbeat` text         NOT NULL COMMENT 'Metric heartbeat info',
    `report_time`      bigint(20)   NOT NULL COMMENT 'Report time',
    `create_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_group_heartbeat` (`component`, `instance`, `inlong_group_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='Inlong group heartbeat';

-- ----------------------------
-- Table structure for inlong stream heartbeat
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stream_heartbeat`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT COMMENT 'Incremental primary key',
    `component`        varchar(64)  NOT NULL DEFAULT '' COMMENT 'Component name, such as: Agent, Sort...',
    `instance`         varchar(64)  NOT NULL DEFAULT '' COMMENT 'Component instance, can be ip, name...',
    `inlong_group_id`  varchar(256) NOT NULL DEFAULT '' COMMENT 'Owning inlong group id',
    `inlong_stream_id` varchar(256) NOT NULL DEFAULT '' COMMENT 'Owning inlong stream id',
    `status_heartbeat` text         NOT NULL COMMENT 'Status heartbeat info',
    `metric_heartbeat` text         NOT NULL COMMENT 'Metric heartbeat info',
    `report_time`      bigint(20)   NOT NULL COMMENT 'Report time',
    `create_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `modify_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Modify time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_stream_heartbeat` (`component`, `instance`, `inlong_group_id`, `inlong_stream_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='Inlong stream heartbeat';

-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
